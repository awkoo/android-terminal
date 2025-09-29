#include <dirent.h>     // 引入 dirent.h 头文件，用于目录流操作，例如读取目录内容
#include <fcntl.h>      // 引入 fcntl.h 头文件，用于文件控制，例如文件打开模式
#include <jni.h>        // 引入 jni.h 头文件，用于 Java 本地接口 (JNI) 编程，使 C/C++ 代码可以与 Java 代码交互
#include <signal.h>     // 引入 signal.h 头文件，用于信号处理
#include <stdio.h>      // 引入 stdio.h 头文件，用于标准输入输出函数，例如 printf, perror
#include <stdlib.h>     // 引入 stdlib.h 头文件，用于通用实用程序函数，例如 malloc, exit, clearenv
#include <string.h>     // 引入 string.h 头文件，用于字符串操作函数，例如 strdup
#include <sys/ioctl.h>  // 引入 sys/ioctl.h 头文件，用于设备输入/输出控制，例如设置终端窗口大小
#include <sys/wait.h>   // 引入 sys/wait.h 头文件，用于等待子进程状态改变
#include <termios.h>    // 引入 termios.h 头文件，用于终端 I/O 接口，例如设置终端属性
#include <unistd.h>     // 引入 unistd.h 头文件，用于 POSIX 操作系统 API，例如 fork, execvp, chdir, close, dup2
#include <errno.h>      // 引入 errno.h 头文件，用于错误码定义
#include <limits.h>     // 引入 limits.h 头文件，用于定义整型类型的限制，如 LONG_MAX, LONG_MIN

#define UNUSED(x) x __attribute__((__unused__)) // 定义一个宏 UNUSED，用于标记函数参数为未使用，避免编译器警告

// 静态函数：抛出运行时异常到 Java 层
static int throw_runtime_exception(JNIEnv *env, char const *message) {
    // 查找 Java 中的 RuntimeException 类
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    // 使用找到的类和给定的消息抛出一个新的 RuntimeException
    (*env)->ThrowNew(env, exClass, message);
    // 返回 -1 表示操作失败或异常已抛出
    return -1;
}

// 静态函数：创建子进程
static int create_subprocess(
        JNIEnv *env,            // JNI 环境指针，用于与 Java 交互
        char const *cmd,        // 要执行的命令字符串
        char const *cwd,
        char *const argv[],     // 命令行参数数组
        char **envp,            // 环境变量数组
        int *pProcessId,        // 用于返回子进程 ID 的指针
        jint rows,              // 终端的行数
        jint columns,           // 终端的列数
        jint cell_width,        // 终端字符单元格的宽度（像素）
        jint cell_height        // 终端字符单元格的高度（像素）
) {
    // 打开伪终端主设备 (PTM)，O_RDWR 表示读写，O_CLOEXEC 表示在 execve 时关闭文件描述符
    int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    // 如果打开失败，抛出运行时异常
    if (ptm < 0) return throw_runtime_exception(env, "Cannot open /dev/ptmx");

    char devname[64]; // 用于存储伪终端从设备 (PTS) 的名称
    // grantpt() 更改伪终端从设备的权限
    // unlockpt() 解锁伪终端从设备
    // ptsname_r() 获取伪终端从设备的名称
    if (
            grantpt(ptm) ||
            unlockpt(ptm) ||
            ptsname_r(ptm, devname, sizeof(devname))
            ) {
        // 如果上述任何操作失败，抛出运行时异常
        return throw_runtime_exception(env, "Cannot grantpt()/unlockpt()/ptsname_r() on /dev/ptmx");
    }

    // 启用 UTF-8 模式并禁用流控制，以防止 Ctrl+S 锁定显示。
    struct termios tios; // 定义一个 termios 结构体，用于存储终端属性
    tcgetattr(ptm, &tios); // 获取伪终端主设备的当前属性
    tios.c_iflag |= IUTF8; // 启用输入流中的 UTF-8 编码
    tios.c_iflag &= ~(IXON | IXOFF); // 禁用 XON/XOFF 软件流控制（Ctrl+S / Ctrl+Q）
    tios.c_lflag &= ~ECHO; // 禁用回显
    tcsetattr(ptm, TCSANOW, &tios); // 设置伪终端主设备的新属性，立即生效

    /** 设置初始窗口大小。*/
    struct winsize sz = { // 定义一个 winsize 结构体，用于存储终端窗口大小
            .ws_row = (unsigned short) rows,          // 设置行数
            .ws_col = (unsigned short) columns,       // 设置列数
            .ws_xpixel = (unsigned short) (columns * cell_width),  // 设置水平像素数
            .ws_ypixel = (unsigned short) (rows * cell_height)     // 设置垂直像素数
    };
    ioctl(ptm, TIOCSWINSZ, &sz); // 使用 ioctl 设置伪终端主设备的窗口大小

    pid_t pid = fork(); // 创建一个新进程（子进程）
    if (pid < 0) {
        // 如果 fork 失败，抛出运行时异常
        return throw_runtime_exception(env, "Fork failed");
    } else if (pid > 0) {
        // 父进程分支
        *pProcessId = (int) pid; // 将子进程 ID 存储到传入的指针中
        return ptm; // 返回伪终端主设备的文件描述符
    } else {
        // 子进程分支
        // 清除 Android Java 进程可能已阻塞的信号：
        sigset_t signals_to_unblock; // 定义一个信号集
        sigfillset(&signals_to_unblock); // 将所有信号添加到信号集中
        sigprocmask(SIG_UNBLOCK, &signals_to_unblock, nullptr); // 解除所有信号的阻塞

        close(ptm); // 关闭伪终端主设备的文件描述符（子进程不需要）
        setsid(); // 创建一个新会话，子进程成为会话组长，并脱离控制终端

        int pts = open(devname, O_RDWR); // 打开伪终端从设备
        if (pts < 0) exit(-1); // 如果打开失败，子进程退出

        dup2(pts, 0); // 将伪终端从设备复制到标准输入 (stdin)
        dup2(pts, 1); // 将伪终端从设备复制到标准输出 (stdout)
        dup2(pts, 2); // 将伪终端从设备复制到标准错误 (stderr)

        DIR *self_dir = opendir("/proc/self/fd"); // 打开当前进程的文件描述符目录
        if (self_dir != NULL) { // 如果目录打开成功
            int self_dir_fd = dirfd(self_dir); // 获取目录的文件描述符
            struct dirent *entry; // 定义一个 dirent 结构体，用于存储目录条目
            while ((entry = readdir(self_dir)) != NULL) { // 遍历目录中的所有条目
                char *endptr;
                errno = 0; // 在调用 strtol 之前清除 errno
                long fd_long = strtol(entry->d_name, &endptr, 10); // 使用 strtol 将文件名（文件描述符数字）转换为长整型

                // 检查转换错误
                if ((errno == ERANGE && (fd_long == LONG_MAX || fd_long == LONG_MIN)) || // 检查是否超出 long 的范围
                    (endptr == entry->d_name) || // 检查是否未转换任何数字
                    (*endptr != '\0')) { // 检查是否有未转换的非数字字符
                    // 如果发生错误，跳过此文件描述符
                    continue;
                }
                int fd = (int) fd_long; // 将长整型转换为整型
                if (fd > 2 && fd != self_dir_fd) close(fd); // 关闭除 stdin, stdout, stderr 和 self_dir_fd 之外的所有文件描述符
            }
            closedir(self_dir); // 关闭目录
        }

        clearenv(); // 清除所有环境变量
        if (envp) for (; *envp; ++envp) putenv(*envp); // 如果有新的环境变量，则设置它们

        if (chdir(cwd) != 0) { // 改变当前工作目录
            char *error_message;
            // 无需释放 asprintf() 分配的内存，因为下面会执行 execvp() 或 exit()。
            if (asprintf(&error_message, "chdir(\"%s\")", cwd) == -1) error_message = "chdir()"; // 格式化错误消息
            perror(error_message); // 打印错误消息
            fflush(stderr); // 刷新标准错误输出流
        }
        execvp(cmd, argv); // 执行指定的命令，替换当前子进程的镜像
        // 显示关于 exec() 调用失败的终端输出：
        char *error_message;
        if (asprintf(&error_message, "exec(\"%s\")", cmd) == -1) error_message = "exec()"; // 格式化错误消息
        perror(error_message); // 打印错误消息
        _exit(1); // 如果 execvp 失败，子进程退出并返回状态码 1
    }
}

// JNI 导出函数：创建子进程
JNIEXPORT jint JNICALL Java_awkoo_terminal_core_JNI_createSubprocess(
        JNIEnv *env,            // JNI 环境指针
        jclass UNUSED(clazz),   // Java 类对象（在此函数中未使用）
        jstring cmd,
        jstring cwd,
        jobjectArray args,      // 命令行参数的 Java 字符串数组
        jobjectArray envVars,   // 环境变量的 Java 字符串数组
        jintArray processIdArray, // 用于返回子进程 ID 的 Java int 数组
        jint rows,              // 终端的行数
        jint columns,           // 终端的列数
        jint cell_width,        // 终端字符单元格的宽度（像素）
        jint cell_height        // 终端字符单元格的高度（像素）
) {
    // 获取命令行参数数组的长度，如果 args 为空则长度为 0
    jsize size = args ? (*env)->GetArrayLength(env, args) : 0;
    char **argv = nullptr; // 声明一个字符指针数组，用于存储 C 风格的命令行参数
    if (size > 0) {
        // 为 argv 数组分配内存，大小为 (size + 1) * sizeof(char*)，额外一个用于存储 NULL 终结符
        argv = (char **) malloc((size + 1) * sizeof(char *));
        // 如果内存分配失败，抛出运行时异常
        if (!argv) return throw_runtime_exception(env, "Couldn\'t allocate argv array");
        for (int i = 0; i < size; ++i) {
            // 从 Java 数组中获取单个命令行参数的 Java 字符串
            jstring arg_java_string = (jstring) (*env)->GetObjectArrayElement(env, args, i);
            // 将 Java 字符串转换为 C 风格的 UTF-8 字符串
            char const *arg_utf8 = (*env)->GetStringUTFChars(env, arg_java_string, nullptr);
            // 如果转换失败，抛出运行时异常
            if (!arg_utf8)
                return throw_runtime_exception(env, "GetStringUTFChars() failed for argv");
            argv[i] = strdup(arg_utf8); // 复制 C 字符串并存储到 argv 数组中
            (*env)->ReleaseStringUTFChars(env, arg_java_string, arg_utf8); // 释放 GetStringUTFChars 锁定的内存
        }
        argv[size] = nullptr; // 将 argv 数组的最后一个元素设置为 NULL，作为结束标记
    }

    // 获取环境变量数组的长度，如果 envVars 为空则长度为 0
    size = envVars ? (*env)->GetArrayLength(env, envVars) : 0;
    char **envp = nullptr; // 声明一个字符指针数组，用于存储 C 风格的环境变量
    if (size > 0) {
        // 为 envp 数组分配内存
        envp = (char **) malloc((size + 1) * sizeof(char *));
        // 如果内存分配失败，抛出运行时异常
        if (!envp) return throw_runtime_exception(env, "malloc() for envp array failed");
        for (int i = 0; i < size; ++i) {
            // 从 Java 数组中获取单个环境变量的 Java 字符串
            jstring env_java_string = (jstring) (*env)->GetObjectArrayElement(env, envVars, i);
            // 将 Java 字符串转换为 C 风格的 UTF-8 字符串
            char const *env_utf8 = (*env)->GetStringUTFChars(env, env_java_string, nullptr);
            // 如果转换失败，抛出运行时异常
            if (!env_utf8)
                return throw_runtime_exception(env, "GetStringUTFChars() failed for env");
            envp[i] = strdup(env_utf8); // 复制 C 字符串并存储到 envp 数组中
            (*env)->ReleaseStringUTFChars(env, env_java_string, env_utf8); // 释放 GetStringUTFChars 锁定的内存
        }
        envp[size] = nullptr; // 将 envp 数组的最后一个元素设置为 NULL
    }

    int procId = 0; // 初始化进程 ID
    // 将 Java 字符串 cwd 转换为 C 风格的 UTF-8 字符串
    char const *cmd_cwd = (*env)->GetStringUTFChars(env, cwd, nullptr);
    // 将 Java 字符串 cmd 转换为 C 风格的 UTF-8 字符串
    char const *cmd_utf8 = (*env)->GetStringUTFChars(env, cmd, nullptr);
    // 调用 create_subprocess 函数创建子进程
    int ptm = create_subprocess(
            env,
            cmd_utf8,
            cmd_cwd,
            argv,
            envp,
            &procId,
            rows,
            columns,
            cell_width,
            cell_height
    );
    // 释放 GetStringUTFChars 锁定的内存
    (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);
    (*env)->ReleaseStringUTFChars(env, cwd, cmd_cwd); // 注意：这里 cmd_cwd 应该对应 cwd，而不是 cmd

    if (argv) { // 如果 argv 数组不为空
        for (char **tmp = argv; *tmp; ++tmp) free(*tmp); // 释放 argv 数组中每个复制的字符串的内存
        free(argv); // 释放 argv 数组本身的内存
    }
    if (envp) { // 如果 envp 数组不为空
        for (char **tmp = envp; *tmp; ++tmp) free(*tmp); // 释放 envp 数组中每个复制的字符串的内存
        free(envp); // 释放 envp 数组本身的内存
    }

    // 获取 Java int 数组 processIdArray 的临界区指针
    int *pProcId = (int *) (*env)->GetPrimitiveArrayCritical(env, processIdArray, nullptr);
    // 如果获取失败，抛出运行时异常
    if (!pProcId)
        return throw_runtime_exception(
                env,
                "JNI call GetPrimitiveArrayCritical(processIdArray, &isCopy) failed"
        );

    *pProcId = procId; // 将子进程 ID 存储到 Java 数组中
    // 释放 Java int 数组 processIdArray 的临界区指针
    (*env)->ReleasePrimitiveArrayCritical(env, processIdArray, pProcId, 0);

    return ptm; // 返回伪终端主设备的文件描述符
}

// JNI 导出函数：设置伪终端窗口大小
JNIEXPORT void JNICALL Java_awkoo_terminal_core_JNI_setPtyWindowSize(
        JNIEnv *UNUSED(env),        // JNI 环境指针（在此函数中未使用）
        jclass UNUSED(clazz),       // Java 类对象（在此函数中未使用）
        jint fd,                    // 伪终端的文件描述符
        jint rows,                  // 终端的行数
        jint cols,                  // 终端的列数
        jint cell_width,            // 终端字符单元格的宽度（像素）
        jint cell_height            // 终端字符单元格的高度（像素）
) {
    struct winsize sz = { // 定义一个 winsize 结构体，用于存储终端窗口大小
            .ws_row = (unsigned short) rows,          // 设置行数
            .ws_col = (unsigned short) cols,          // 设置列数
            .ws_xpixel = (unsigned short) (cols * cell_width),  // 设置水平像素数
            .ws_ypixel = (unsigned short) (rows * cell_height)     // 设置垂直像素数
    };
    ioctl(fd, TIOCSWINSZ, &sz); // 使用 ioctl 设置指定文件描述符的窗口大小
}

// JNI 导出函数：等待子进程结束
JNIEXPORT jint JNICALL Java_awkoo_terminal_core_JNI_waitFor(
        JNIEnv *UNUSED(env),        // JNI 环境指针（在此函数中未使用）
        jclass UNUSED(clazz),       // Java 类对象（在此函数中未使用）
        jint pid                    // 要等待的子进程 ID
) {
    int status; // 用于存储子进程的状态信息
    waitpid(pid, &status, 0); // 等待指定的子进程结束，并获取其状态
    if (WIFEXITED(status)) { // 如果子进程正常退出
        return WEXITSTATUS(status); // 返回子进程的退出状态码
    } else if (WIFSIGNALED(status)) { // 如果子进程被信号终止
        return -WTERMSIG(status); // 返回导致子进程终止的信号的负值
    } else {
        // 这应该永远不会发生 - waitpid(2) 说 "前三个宏之一将评估为非零（真）值"。
        return 0; // 返回 0（理论上不会到达这里）
    }
}

// JNI 导出函数：关闭文件描述符
JNIEXPORT void JNICALL Java_awkoo_terminal_core_JNI_close(
        JNIEnv *UNUSED(env),        // JNI 环境指针（在此函数中未使用）
        jclass UNUSED(clazz),       // Java 类类对象（在此函数中未使用）
        jint fileDescriptor        // 要关闭的文件描述符
) {
    close(fileDescriptor); // 关闭指定的文件描述符
}