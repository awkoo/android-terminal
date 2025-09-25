package awkoo.terminal.terminal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.UUID;

import awkoo.terminal.R;

/**
 * 终端会话，由一个与终端接口耦合的进程组成。
 * <p>
 * 子进程将由构造函数执行，当通过调用
 * {@link #updateSize(int, int, int, int)} 告知大小后，终端仿真将开始，并创建线程处理子进程I/O。
 * 所有终端仿真和回调方法都将在主线程上执行。
 * <p>
 * 子进程可以通过使用 {@link #finishIfRunning()} 方法强制退出。
 * <p>
 * 注意：终端会话可能会比 EmulatorView 寿命更长，所以请小心使用回调！
 */
public final class TerminalSession extends TerminalOutput {

    private static final int MSG_NEW_INPUT = 1;
    private static final int MSG_PROCESS_EXITED = 4;

    public final String mHandle = UUID.randomUUID().toString();

    TerminalEmulator mEmulator;

    /**
     * 一个队列，当进程输出时从一个单独的线程写入，并由主线程读取以由终端模拟器处理。
     */
    final ByteQueue mProcessToTerminalIOQueue = new ByteQueue(4096);
    /**
     * 一个队列，由主线程由于用户交互而写入，并由另一个线程通过写入
     * {@link #mTerminalFileDescriptor} 进行转发。
     */
    final ByteQueue mTerminalToProcessIOQueue = new ByteQueue(4096);
    /**
     * 缓冲区，用于在写入 mTerminalToProcessIOQueue 之前将代码点转换为 utf8。
     */
    private final byte[] mUtf8InputBuffer = new byte[5];

    /**
     * 会话完成或标题更改时收到通知的回调。
     */
    TerminalSessionClient mClient;

    /**
     * shell 进程的 pid。如果未启动为0，如果已完成运行为-1。
     */
    int mShellPid;

    /**
     * shell 进程的退出状态。仅当 ${@link #mShellPid} 为 -1 时有效。
     */
    int mShellExitStatus;

    /**
     * 引用伪终端对主控端的 文件描述符，通过调用
     * {@link JNI#createSubprocess(String, String, String[], String[], int[], int, int, int, int)} 获得。
     */
    private int mTerminalFileDescriptor;

    /**
     * 由应用程序设置用于会话的用户标识，而不是由终端设置。
     */
    public String mSessionName;

    final Handler mMainThreadHandler = new MainThreadHandler(this);

    private final String mShellPath;
    private final String mCwd;
    private final String[] mArgs;
    private final String[] mEnv;
    private final Integer mTranscriptRows;
    private final Context context;
    private final String stdin;


    public TerminalSession(
        Context context,
        String shellPath,
        String cwd,
        String[] args,
        String[] env,
        String stdin,
        Integer transcriptRows,
        TerminalSessionClient client
    ) {
        this.context = context;
        this.mShellPath = shellPath;
        this.mCwd = cwd;
        this.mArgs = args;
        this.mEnv = env;
        this.stdin = stdin;
        this.mTranscriptRows = transcriptRows;
        this.mClient = client;
    }

    /**
     * @param client {@link TerminalSessionClient} 接口实现，允许
     *               {@link TerminalSession} 与其客户端之间进行通信。
     */
    public void updateTerminalSessionClient(TerminalSessionClient client) {
        mClient = client;

        if (mEmulator != null)
            mEmulator.updateTerminalSessionClient(client);
    }

    /**
     * 通知连接的 pty 新的大小，并重新布局或初始化模拟器。
     */
    public void updateSize(int columns, int rows, int cellWidthPixels, int cellHeightPixels) {
        if (mEmulator == null) {
            initializeEmulator(columns, rows, cellWidthPixels, cellHeightPixels);
        } else {
            JNI.setPtyWindowSize(mTerminalFileDescriptor, rows, columns, cellWidthPixels, cellHeightPixels);
            mEmulator.resize(columns, rows, cellWidthPixels, cellHeightPixels);
        }
    }

    /**
     * 通过转义序列设置的终端标题，如果未设置则为 null。
     */
    public String getTitle() {
        return (mEmulator == null) ? null : mEmulator.getTitle();
    }

    /**
     * 设置终端模拟器的窗口大小并开始终端模拟。
     *
     * @param columns 终端窗口中的列数。
     * @param rows    终端窗口中的行数。
     */
    public void initializeEmulator(int columns, int rows, int cellWidthPixels, int cellHeightPixels) {
        mEmulator = new TerminalEmulator(
            this,
            columns,
            rows,
            cellWidthPixels,
            cellHeightPixels,
            mTranscriptRows,
            mClient
        );

        Log.e(mShellPath, String.join(" ", mArgs));
        int[] processId = new int[1];
        mTerminalFileDescriptor = JNI.createSubprocess(
            mShellPath,
            mCwd,
            mArgs,
            mEnv,
            processId,
            rows,
            columns,
            cellWidthPixels,
            cellHeightPixels
        );
        mShellPid = processId[0];

        if (stdin != null && !stdin.isEmpty()) {
            JNI.setStdinEcho(mTerminalFileDescriptor, false); // 禁用回显，解决多余的命令显示
            write(stdin + "\n");
        }

        try {
            final ParcelFileDescriptor terminalFD = ParcelFileDescriptor.fromFd(mTerminalFileDescriptor);
            new Thread("TermSessionInputReader[pid=" + mShellPid + "]") {
                @Override
                public void run() {
                    try (InputStream termIn = new ParcelFileDescriptor.AutoCloseInputStream(terminalFD)) {
                        final byte[] buffer = new byte[4096];
                        while (true) {
                            int read = termIn.read(buffer);
                            if (read == -1) return;
                            if (!mProcessToTerminalIOQueue.write(buffer, 0, read)) return;
                            mMainThreadHandler.sendEmptyMessage(MSG_NEW_INPUT);
                        }
                    } catch (Exception e) {
                        // 忽略，只是关闭。
                    }
                }
            }.start();

            new Thread("TermSessionOutputWriter[pid=" + mShellPid + "]") {
                @Override
                public void run() {
                    final byte[] buffer = new byte[4096];
                    try (FileOutputStream termOut = new ParcelFileDescriptor.AutoCloseOutputStream(terminalFD)) {
                        while (true) {
                            int bytesToWrite = mTerminalToProcessIOQueue.read(buffer, true);
                            if (bytesToWrite == -1) return;
                            termOut.write(buffer, 0, bytesToWrite);
                        }
                    } catch (IOException e) {
                        // 忽略。
                    }
                }
            }.start();
        } catch (Exception e) {
            byte[] byteToWrite = Objects.requireNonNull(e.getMessage()).getBytes();
            mEmulator.append(byteToWrite, byteToWrite.length);
            notifyScreenUpdate();
            mClient.onSessionFinished(this);
        }

        new Thread("TermSessionWaiter[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                int processExitCode = JNI.waitFor(mShellPid);
                mMainThreadHandler.sendMessage(
                    mMainThreadHandler.obtainMessage(
                        MSG_PROCESS_EXITED,
                        processExitCode
                    )
                );
            }
        }.start();

    }

    /**
     * 向 shell 进程写入数据。
     */
    @Override
    public void write(byte[] data, int offset, int count) {
        if (mShellPid > 0) mTerminalToProcessIOQueue.write(data, offset, count);
    }

    /**
     * 将 Unicode 代码点以 UTF-8 编码写入终端。
     */
    public void writeCodePoint(boolean prependEscape, int codePoint) {
        if (codePoint > 1114111 || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
            // 1114111 (= 2**16 + 1024**2 - 1) 是最高代码点，[0xD800,0xDFFF] 是代理范围。
            throw new IllegalArgumentException("Invalid code point: " + codePoint);
        }

        int bufferPosition = 0;
        if (prependEscape) mUtf8InputBuffer[bufferPosition++] = 27;

        if (codePoint <= /* 7 位 */0b1111111) {
            mUtf8InputBuffer[bufferPosition++] = (byte) codePoint;
        } else if (codePoint <= /* 11 位 */0b11111111111) {
            /* 110xxxxx 前导字节，带前导 5 位 */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b11000000 | (codePoint >> 6));
            /* 10xxxxxx 延续字节，带后续 6 位 */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | (codePoint & 0b111111));
        } else if (codePoint <= /* 16 位 */0b1111111111111111) {
            /* 1110xxxx 前导字节，带前导 4 位 */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b11100000 | (codePoint >> 12));
            /* 10xxxxxx 延续字节，带后续 6 位 */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | ((codePoint >> 6) & 0b111111));
            /* 10xxxxxx 延续字节，带后续 6 位 */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | (codePoint & 0b111111));
        } else { /* 我们已经检查了 codePoint <= 1114111，所以我们有最大 21 位 = 0b111111111111111111111 */
            /* 11110xxx 前导字节，带前导 3 位 */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b11110000 | (codePoint >> 18));
            /* 10xxxxxx 延续字节，带后续 6 位 */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | ((codePoint >> 12) & 0b111111));
            /* 10xxxxxx 延续字节，带后续 6 位 */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | ((codePoint >> 6) & 0b111111));
            /* 10xxxxxx 延续字节，带后续 6 位 */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | (codePoint & 0b111111));
        }
        write(mUtf8InputBuffer, 0, bufferPosition);
    }

    public TerminalEmulator getEmulator() {
        return mEmulator;
    }

    /**
     * 通知 {@link #mClient} 屏幕已更改。
     */
    private void notifyScreenUpdate() {
        mClient.onTextChanged(this);
    }

    /**
     * 重置终端模拟器状态。
     */
    public void reset() {
        mEmulator.reset();
        notifyScreenUpdate();
    }

    /**
     * 通过向 shell 发送 SIGKILL 来结束此终端会话。
     */
    public void finishIfRunning() {
        if (isRunning()) {
            try {
                Os.kill(mShellPid, OsConstants.SIGKILL);
            } catch (ErrnoException ignored) {
            }
        }
    }

    /**
     * 进程退出时清理资源。
     */
    void cleanupResources(int exitStatus) {
        synchronized (this) {
            mShellPid = -1;
            mShellExitStatus = exitStatus;
        }

        // 停止读写线程，并关闭 I/O 流
        mTerminalToProcessIOQueue.close();
        mProcessToTerminalIOQueue.close();
        JNI.close(mTerminalFileDescriptor);
    }

    @Override
    public void titleChanged(String oldTitle, String newTitle) {
        mClient.onTitleChanged(this);
    }

    public synchronized boolean isRunning() {
        return mShellPid != -1;
    }

    /**
     * 仅在未 {@link #isRunning()} 时有效。
     */
    public synchronized int getExitStatus() {
        return mShellExitStatus;
    }

    @Override
    public void onCopyTextToClipboard(String text) {
        mClient.onCopyTextToClipboard(this, text);
    }

    @Override
    public void onPasteTextFromClipboard() {
        mClient.onPasteTextFromClipboard(this);
    }

    @Override
    public void onColorsChanged() {
        mClient.onColorsChanged(this);
    }

    public int getPid() {
        return mShellPid;
    }

    private static class MainThreadHandler extends Handler {

        private final WeakReference<TerminalSession> terminalSessionWeakReference;

        MainThreadHandler(TerminalSession terminalSession) {
            super(Looper.getMainLooper());
            terminalSessionWeakReference = new WeakReference<>(terminalSession);
        }

        final byte[] mReceiveBuffer = new byte[4 * 1024];

        @Override
        public void handleMessage(@NonNull Message msg) {
            TerminalSession session = terminalSessionWeakReference.get();
            int bytesRead = session.mProcessToTerminalIOQueue.read(mReceiveBuffer, false);
            if (bytesRead > 0) {
                session.mEmulator.append(mReceiveBuffer, bytesRead);
                session.notifyScreenUpdate();
            }

            if (msg.what == MSG_PROCESS_EXITED) {
                int exitCode = (Integer) msg.obj;
                session.cleanupResources(exitCode);

                byte[] bytesToWrite = session.context.getString(
                    R.string.session_exit_message,
                    exitCode < 0 ? "signal" : "code",
                    exitCode
                ).getBytes();
                session.mEmulator.append(bytesToWrite, bytesToWrite.length);
                session.notifyScreenUpdate();
                session.mClient.onSessionFinished(session);
            }
        }
    }
}
