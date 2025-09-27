package awkoo.terminal.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import awkoo.terminal.Constants;
import awkoo.terminal.Constants.TERMUX_APP.TERMUX_SERVICE;
import awkoo.terminal.R;
import awkoo.terminal.shell.ShellCommand;
import awkoo.terminal.shell.TerminalShell;
import awkoo.terminal.core.TerminalEmulator;
import awkoo.terminal.core.TerminalSession;
import awkoo.terminal.core.TerminalSessionActivityClient;
import awkoo.terminal.core.TerminalSessionClient;
import awkoo.terminal.core.TerminalSessionClientBase;

/**
 * 一个后台服务，持有一系列 {@link TerminalShell}，并在运行时显示一个前台通知以防止被系统终止。
 * 用户通过 {@link MainActivity} 与会话交互，但此服务可能会在用户或系统销毁 Activity 后继续存在。
 * 在这种情况下，用户可以稍后重启 {@link MainActivity} 以再次访问这些会话。
 * <p/>
 * 为了根据用户的意愿，尽可能长时间地保持终端会话和衍生的子进程（它们可能比终端会话更长寿）存活，
 * 本服务是一个前台服务，通过 {@link Service#startForeground(int, Notification)} 启动。
 * <p/>
 * 服务还可以选择性地持有一个唤醒锁（wake lock）和一个Wi-Fi锁，并在通知中显示其状态 - 参见 {@link #buildNotification()}。
 */
public final class TerminalService extends Service {

    /**
     * 此服务仅在同一进程内部进行绑定，从不使用IPC（进程间通信）。
     */
    public class LocalBinder extends Binder {
        public final TerminalService service = TerminalService.this;
    }

    private final IBinder mBinder = new LocalBinder();


    /**
     * {@link TerminalSessionClient} 接口的完整实现，供 {@link TerminalSession} 使用，
     * 其中持有 Activity 的引用以处理与 Activity 相关的功能。
     * 注意，此服务通常比 Activity 的生命周期更长，因此需要注意在适当时机清除此引用。
     */
    private TerminalSessionActivityClient mTerminalSessionActivityClient;

    /**
     * {@link TerminalSessionClient} 接口的基础实现，供 {@link TerminalSession} 使用，
     * 它不持有任何引用。
     */
    private final TerminalSessionClientBase mNullTerminalSessionClient
        = new TerminalSessionClientBase();

    /**
     * 终端会话列表
     */
    public final List<TerminalShell> mTerminalShells = new ArrayList<>();
    private static int SHELL_ID = 0;

    /**
     * 唤醒锁和Wi-Fi锁总是被一起获取和释放。
     */
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;
    boolean mWantsToStop = false;

    /**
     * 服务创建时的回调。
     * 获取应用共享属性并启动前台服务。
     */
    @Override
    public void onCreate() {
        runStartForeground();
    }

    /**
     * 服务启动命令的回调。
     * 根据接收到的Intent Action执行相应操作，例如停止服务、获取/释放唤醒锁。
     *
     * @param intent  启动时传递的Intent。
     * @param flags   关于启动请求的附加数据。
     * @param startId 请求的唯一整数ID。
     * @return 返回服务的启动粘性，这里是 START_NOT_STICKY，表示服务被杀死后不会自动重启。
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 再次运行，以防服务已启动而onCreate()未被调用
        runStartForeground();

        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        if (action != null) {
            switch (action) {
                case TERMUX_SERVICE.ACTION_STOP_SERVICE:
                    actionStopService();
                    break;
                case TERMUX_SERVICE.ACTION_WAKE_LOCK:
                    actionAcquireWakeLock();
                    break;
                case TERMUX_SERVICE.ACTION_WAKE_UNLOCK:
                    actionReleaseWakeLock(true);
                    break;
                default:
                    break;
            }
        }

        // 如果此服务真的被杀死，自动重启没有意义 - 让用户在下次启动应用时手动启动。
        return Service.START_NOT_STICKY;
    }

    /**
     * 服务销毁时的回调。
     * 释放所有锁，杀死所有终端相关的执行命令，并停止前台服务状态。
     */
    @Override
    public void onDestroy() {
        actionReleaseWakeLock(false);
        if (!mWantsToStop)
            killAllTermuxExecutionCommands();

        runStopForeground();
    }

    /**
     * 当客户端通过 bindService() 绑定到服务时调用。
     *
     * @param intent 用于绑定的Intent。
     * @return 返回一个 IBinder 对象，客户端可以通过它与服务通信。
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * 当所有客户端都通过 unbindService() 与服务断开连接时调用。
     *
     * @param intent 用于绑定的Intent。
     * @return 返回一个布尔值，表示是否希望在客户端重新绑定时接收 onRebind() 回调。
     */
    @Override
    public boolean onUnbind(Intent intent) {
        // 由于我们不能保证 MainActivity.onDestroy() 总能执行完毕，
        // 我们也在这里取消设置客户端，以防万一，这样服务和会话就不会持有对已销毁Activity的引用。
        if (mTerminalSessionActivityClient != null)
            unsetTermuxTerminalSessionClient();
        return false;
    }

    /**
     * 将服务置于前台运行模式。
     */
    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(Constants.TERMUX_APP_NOTIFICATION_ID, buildNotification());
    }

    /**
     * 使服务离开前台模式。
     */
    private void runStopForeground() {
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
    }

    /**
     * 请求停止服务。
     */
    private void requestStopService() {
        runStopForeground();
        stopSelf();
    }

    /**
     * 处理停止服务的Action。
     */
    private void actionStopService() {
        mWantsToStop = true;
        killAllTermuxExecutionCommands();
        requestStopService();
    }

    /**
     * 通过向TermuxSessions和TermuxTasks的进程发送SIGKILL信号来杀死它们。
     * <p>
     * 对于TermuxSessions，所有会话都将被杀死，无论用户是手动退出Termux还是因为意外关闭导致onDestroy()被直接调用。
     * 只有在用户手动退出或会话由期望通过PendingIntent返回结果的插件启动时，才会处理结果。
     * <p>
     * 对于TermuxTasks，只有那些由期望通过PendingIntent返回结果的插件启动的任务才会被杀死。
     * 这些被杀死的任务的结果将总是被处理。其余的进程将继续运行，直到termux应用进程被Android系统杀死（例如OOM）。
     * <p>
     * 有些插件执行命令可能在服务被杀死前还未被处理并添加到mTermuxSessions和mTermuxTasks列表中，
     * 因此我们维护一个独立的mPendingPluginExecutionCommands列表来处理这些情况，以便通知pending intent的创建者执行已被取消。
     * <p>
     * 注意，如果用户没有手动退出Termux，并且onDestroy()因为意外关闭（如Android决定杀死服务）被直接调用，
     * 那么无法保证onDestroy()能够执行完毕，termux应用进程可能会在它完成前被杀死。
     * 这意味着在这些情况下，某些插件命令的结果可能无法发送回其创建者，但我们仍会尽力处理。
     * <p>
     * 我们为每个列表创建副本，因为在循环内部有元素被移除。
     */
    private synchronized void killAllTermuxExecutionCommands() {
        boolean processResult;

        List<TerminalShell> terminalShells = new ArrayList<>(mTerminalShells);

        for (int i = 0; i < terminalShells.size(); i++) {
            processResult = mWantsToStop;
            terminalShells.get(i).killIfExecuting(processResult);
            if (!processResult)
                mTerminalShells.remove(terminalShells.get(i));
        }
    }


    /**
     * 处理获取电源和Wi-Fi唤醒锁的Action。
     */
    @SuppressLint("WakelockTimeout")
    private void actionAcquireWakeLock() {
        if (mWakeLock != null) return;

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            Constants.TERMUX_APP_NAME.toLowerCase() + ":service-wakelock"
        );
        mWakeLock.acquire();

        // http://tools.android.com/tech-docs/lint-in-studio-2-3#TOC-WifiManager-Leak
        WifiManager wm = (WifiManager) getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(
            WifiManager.WIFI_MODE_FULL,
            Constants.TERMUX_APP_NAME.toLowerCase()
        );
        mWifiLock.acquire();

        updateNotification();
    }

    /**
     * 处理释放电源和Wi-Fi唤醒锁的Action。
     */
    private void actionReleaseWakeLock(boolean updateNotification) {
        if (mWakeLock == null && mWifiLock == null) return;
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        if (mWifiLock != null) {
            mWifiLock.release();
            mWifiLock = null;
        }

        if (updateNotification)
            updateNotification();
    }

    /**
     * 创建一个新的 {@link TerminalShell}。
     * 当前由 {@link TerminalSessionActivityClient#addNewSession(String)} 调用以添加新会话。
     *
     * @param executable   可执行文件的路径，如果为null则使用默认shell。
     * @param arguments        传递给可执行文件的参数。
     * @param stdin            发送到stdin的初始文本。
     * @param workingDirectory 会话的工作目录。
     * @param sessionName      会话的自定义名称。
     * @return 如果成功则返回创建的 {@link TerminalShell}，否则返回 null。
     */
    public TerminalShell createSession(
        String executable,
        String[] arguments,
        String stdin,
        String workingDirectory,
        String sessionName,
        boolean rootMode
    ) {
        ShellCommand shellCommand = new ShellCommand(
            SHELL_ID++,
            executable,
            arguments,
            stdin,
            workingDirectory,
            rootMode ? ShellCommand.Mode.ROOT : ShellCommand.Mode.APP
        );

        shellCommand.shellName = sessionName;

        shellCommand.terminalTranscriptRows = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS;

        TerminalShell newTerminalShell = new TerminalShell(
            this,
            shellCommand,
            getTermuxTerminalSessionClient(),
            this::onSessionExited,
            null
        );

        mTerminalShells.add(newTerminalShell);

        // 如果Activity在前台，通知TermuxSessionsListViewController会话列表已更新
        if (mTerminalSessionActivityClient != null)
            mTerminalSessionActivityClient.termuxSessionListNotifyUpdated();

        updateNotification();

        return newTerminalShell;
    }

    /**
     * 移除一个指定的TermuxSession。
     *
     * @param sessionToRemove 要移除的终端会话。
     * @return 返回被移除会话在列表中的索引，如果未找到则为-1或更小。
     */
    public synchronized int removeSession(TerminalSession sessionToRemove) {
        int index = getIndexOfSession(sessionToRemove);

        if (index >= 0)
            mTerminalShells.get(index).finish();

        return index;
    }

    /**
     * 当一个 {@link TerminalShell} 结束时收到的回调。
     *
     * @param terminalShell 已退出的会话。
     */
    public void onSessionExited(final TerminalShell terminalShell) {
        if (terminalShell != null) {
            mTerminalShells.remove(terminalShell);

            // 如果Activity在前台，通知TermuxSessionsListViewController会话列表已更新
            if (mTerminalSessionActivityClient != null)
                mTerminalSessionActivityClient.termuxSessionListNotifyUpdated();
        }

        updateNotification();
    }


    /**
     * 如果 {@link MainActivity} 尚未绑定到 {@link TerminalService} 或已被销毁，
     * 那么需要Activity的接口功能对终端会话应不可用，因此我们只返回 {@link #mNullTerminalSessionClient}。
     * 一旦收到 {@link MainActivity} 的绑定回调，它应调用 {@link #setTermuxTerminalSessionClient} 来设置
     * {@link TerminalService#mTerminalSessionActivityClient}，以便后续的终端会话能直接传递
     * 完整实现了 {@link TerminalSessionClient} 接口的 {@link TerminalSessionActivityClient} 对象。
     *
     * @return 如果 {@link MainActivity} 已绑定，则返回 {@link TerminalSessionActivityClient}，否则返回 {@link TerminalSessionClientBase}。
     */
    public synchronized TerminalSessionClientBase getTermuxTerminalSessionClient() {
        return Objects.requireNonNullElse(
            mTerminalSessionActivityClient,
            mNullTerminalSessionClient
        );
    }

    /**
     * 当 {@link MainActivity#onServiceConnected} 被调用时应调用此方法，
     * 以设置 {@link TerminalService#mTerminalSessionActivityClient} 变量，
     * 并更新 {@link TerminalSession} 和 {@link TerminalEmulator} 的客户端，
     * 以防它们之前被传递了 {@link TerminalSessionClientBase}。
     *
     * @param terminalSessionActivityClient 完整实现 {@link TerminalSessionClient} 接口的 {@link TerminalSessionActivityClient} 对象。
     */
    public synchronized void setTermuxTerminalSessionClient(
        TerminalSessionActivityClient terminalSessionActivityClient
    ) {
        mTerminalSessionActivityClient = terminalSessionActivityClient;

        for (int i = 0; i < mTerminalShells.size(); i++)
            mTerminalShells.get(i)
                .getTerminalSession()
                .updateTerminalSessionClient(mTerminalSessionActivityClient);
    }

    /**
     * 当 {@link MainActivity} 被销毁时以及在 {@link #onUnbind(Intent)} 中应调用此方法，
     * 以确保 {@link TerminalService}、{@link TerminalSession} 和 {@link TerminalEmulator}
     * 的客户端不持有对Activity的引用。
     */
    public synchronized void unsetTermuxTerminalSessionClient() {
        for (int i = 0; i < mTerminalShells.size(); i++)
            mTerminalShells.get(i)
                .getTerminalSession()
                .updateTerminalSessionClient(mNullTerminalSessionClient);

        mTerminalSessionActivityClient = null;
    }

    /**
     * 构建用于前台服务的通知。
     *
     * @return 返回构建好的 {@link Notification} 对象。
     */
    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        int sessionCount = getShellList().size();
        String notificationText;
        final boolean wakeLockHeld = mWakeLock != null;
        if (wakeLockHeld) notificationText = getString(
            R.string.notification_title_sessions_wakelock,
            sessionCount
        );
        else notificationText = getString(R.string.notification_title_sessions, sessionCount);

        Intent exitIntent = new Intent(this, TerminalService.class)
            .setAction(TERMUX_SERVICE.ACTION_STOP_SERVICE);
        PendingIntent exitPendingIntent = PendingIntent.getService(
            this,
            0,
            exitIntent,
            PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Action exitAction = new Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_delete),
            getString(R.string.notification_action_exit),
            exitPendingIntent
        ).build();

        String newWakeAction = wakeLockHeld ?
            TERMUX_SERVICE.ACTION_WAKE_UNLOCK :
            TERMUX_SERVICE.ACTION_WAKE_LOCK;
        Intent toggleWakeLockIntent = new Intent(this, TerminalService.class)
            .setAction(newWakeAction);
        String actionTitle = getString(
            wakeLockHeld ?
                R.string.notification_action_wake_unlock :
                R.string.notification_action_wake_lock
        );
        Icon actionIcon = Icon.createWithResource(
            this,
            wakeLockHeld ?
                android.R.drawable.ic_lock_idle_lock :
                android.R.drawable.ic_lock_lock
        );
        PendingIntent toggleWakeLockPendingIntent = PendingIntent.getService(
            this,
            0,
            toggleWakeLockIntent,
            PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Action toggleWakeLockAction = new Notification.Action.Builder(
            actionIcon, actionTitle, toggleWakeLockPendingIntent
        ).build();

        return new Notification.Builder(this, Constants.TERMUX_APP_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(notificationText)
            .setContentIntent(contentIntent)
            .setShowWhen(false)
            .setSmallIcon(R.drawable.ic_service_notification)
            .setOngoing(true)
            .addAction(exitAction)
            .addAction(toggleWakeLockAction)
            .build();
    }

    /**
     * 为服务通知设置通知渠道。
     * 这是 Android Oreo (API 26) 及以上版本所必需的。
     */
    private void setupNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
            Constants.TERMUX_APP_NOTIFICATION_CHANNEL_ID,
            Constants.TERMUX_APP_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        );
        manager.createNotificationChannel(channel);
    }

    /**
     * 在做出任何会影响前台服务通知的更改后，更新该通知。
     */
    public synchronized void updateNotification() {
        if (mWakeLock == null && mTerminalShells.isEmpty()) {
            // 如果在用户禁用了所有锁且没有会话或任务在运行时进行更新，则退出服务。
            requestStopService();
        } else {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(Constants.TERMUX_APP_NOTIFICATION_ID, buildNotification());
        }
    }

    /**
     * 获取可变的终端会话列表。
     *
     * @return 返回 {@link #mTerminalShells} 的引用。
     */
    public synchronized List<TerminalShell> getShellList() {
        return mTerminalShells;
    }

    /**
     * 根据索引获取一个终端会话。
     *
     * @param index 要获取的会话的索引。
     * @return 返回指定索引处的 {@link TerminalShell}，如果索引无效则返回 {@code null}。
     */
    @Nullable
    public synchronized TerminalShell getSession(int index) {
        if (index >= 0 && index < mTerminalShells.size())
            return mTerminalShells.get(index);
        else return null;
    }

    /**
     * 根据给定的 {@link TerminalSession} 获取包装它的 {@link TerminalShell}。
     *
     * @param terminalSession 内部的终端会话。
     * @return 返回匹配的 {@link TerminalShell}，如果未找到则返回 {@code null}。
     */
    @Nullable
    public synchronized TerminalShell getShellFromSession(TerminalSession terminalSession) {
        if (terminalSession == null) return null;

        for (int i = 0; i < mTerminalShells.size(); i++) {
            if (mTerminalShells.get(i).getTerminalSession().equals(terminalSession))
                return mTerminalShells.get(i);
        }

        return null;
    }

    /**
     * 获取最后一个（即最近创建的）终端会话。
     *
     * @return 返回列表中的最后一个 {@link TerminalShell}，如果列表为空则返回 {@code null}。
     */
    public synchronized TerminalShell getLastSession() {
        return mTerminalShells.isEmpty() ? null : mTerminalShells.get(mTerminalShells.size() - 1);
    }

    /**
     * 获取给定 {@link TerminalSession} 在列表中的索引。
     *
     * @param terminalSession 要查找的内部终端会话。
     * @return 返回会话的索引，如果未找到则返回 -1。
     */
    public synchronized int getIndexOfSession(TerminalSession terminalSession) {
        if (terminalSession == null) return -1;

        for (int i = 0; i < mTerminalShells.size(); i++) {
            if (mTerminalShells.get(i).getTerminalSession().equals(terminalSession))
                return i;
        }
        return -1;
    }

    /**
     * 根据会话句柄（handle）获取一个 {@link TerminalSession}。
     *
     * @param sessionHandle 要查找的会话句柄字符串。
     * @return 返回匹配的 {@link TerminalSession}，如果未找到则返回 {@code null}。
     */
    public synchronized TerminalSession getSessionFromHandle(String sessionHandle) {
        TerminalSession terminalSession;
        for (int i = 0, len = mTerminalShells.size(); i < len; i++) {
            terminalSession = mTerminalShells.get(i).getTerminalSession();
            if (terminalSession.mHandle.equals(sessionHandle))
                return terminalSession;
        }
        return null;
    }

    /**
     * 检查服务是否已被请求停止。
     *
     * @return 如果 {@link #mWantsToStop} 为真则返回 {@code true}。
     */
    public boolean wantsToStop() {
        return mWantsToStop;
    }

}
