package awkoo.terminal.activities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import awkoo.terminal.Constants;
import awkoo.terminal.R;
import awkoo.terminal.TerminalService;
import awkoo.terminal.databinding.ActivityMainBinding;
import awkoo.terminal.extrakeys.ExtraKeysView;
import awkoo.terminal.terminal.SessionsListViewController;
import awkoo.terminal.terminal.TerminalSession;
import awkoo.terminal.terminal.TerminalSessionActivityClient;
import awkoo.terminal.terminal.TerminalViewClient;
import awkoo.terminal.terminal.io.TerminalToolbarViewPager;
import awkoo.terminal.terminal.io.TermuxTerminalExtraKeys;
import awkoo.terminal.utils.UI;
import awkoo.terminal.utils.data.DataUtils;
import awkoo.terminal.utils.interact.TextInputDialogUtils;
import awkoo.terminal.utils.preferences.TermuxAppSharedPreferences;
import awkoo.terminal.view.TerminalView;

/**
 * 终端模拟器 Activity。
 * <p/>
 * 关于内存泄漏问题，请参考
 * <ul>
 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>
 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>
 * </ul>
 */
public final class MainActivity extends AppCompatActivity implements ServiceConnection {

    public ActivityMainBinding binding;

    /**
     * 与 {@link TerminalService} 的连接。在 {@link #onCreate(Bundle)} 中通过调用
     * {@link #bindService(Intent, ServiceConnection, int)} 请求，并在
     * {@link #onServiceConnected(ComponentName, IBinder)} 中获取和存储。
     */
    TerminalService mTerminalService;

    /**
     * 用于 {@link TerminalView} 和 {@link MainActivity} 之间的通信。
     */
    TerminalViewClient mTerminalViewClient;

    /**
     * 用于 {@link TerminalSession} 和 {@link MainActivity} 之间的通信。
     */
    TerminalSessionActivityClient mTerminalSessionActivityClient;

    /**
     * Termux 应用的共享首选项管理器。
     */
    private TermuxAppSharedPreferences mPreferences;

    /**
     * 终端的额外按键视图。
     */
    ExtraKeysView mExtraKeysView;

    /**
     * {@link #mExtraKeysView} 的客户端。
     */
    TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;

    /**
     * Termux 会话列表的控制器。
     */
    SessionsListViewController mTermuxSessionListViewController;


    /**
     * 标记是否处于 onResume() 和 onStop() 之间。注意，在终端视图的前台只有一个会话，
     * 因此如果引起更改的会话不在前台，则应将其视为后台。
     */
    private boolean mIsVisible;

    /**
     * 标记 onResume() 是否在 onCreate() 之后被调用。
     */
    private boolean mIsOnResumeAfterOnCreate = false;

    private boolean mIsActivityRecreated = false;

    /**
     * 标记 {@link MainActivity} 是否处于无效状态，不能运行。
     */
    private boolean mIsInvalidState;

    private float mTerminalToolbarDefaultHeight;


    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 2;
    private static final int CONTEXT_MENU_RESET_TERMINAL_ID = 3;
    private static final int CONTEXT_MENU_KILL_PROCESS_ID = 4;
    private static final int CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON = 5;
    private static final int CONTEXT_MENU_SETTINGS_ID = 6;

    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    private static final String ARG_ACTIVITY_RECREATED = "activity_recreated";

    /**
     * Activity 创建时的回调。
     *
     * @param savedInstanceState 如果 Activity 被重新创建，则包含先前保存的状态。
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        mIsOnResumeAfterOnCreate = true;

        if (savedInstanceState != null)
            mIsActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false);

        reloadProperties();

        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(Gravity.LEFT))
                    binding.drawerLayout.closeDrawers();
                else finishActivityIfNotFinishing();
            }
        });
        // 加载 Termux 的共享首选项
        // 如果 Constants.TERMUX_PACKAGE_NAME 与 applicationId 不等，此操作也会失败
        mPreferences = TermuxAppSharedPreferences.build(this);

        setTermuxTerminalViewAndClients();
        setTerminalToolbarView(savedInstanceState);
        setSettingsButtonView();
        setNewSessionButtonView();
        setToggleKeyboardView();

        registerForContextMenu(binding.terminalView);

        try {
            // 尝试绑定到服务，如果成功，将调用 {@link #onServiceConnected(ComponentName, IBinder)} 回调
            Intent serviceIntent = new Intent(this, TerminalService.class);
            if (!bindService(serviceIntent, this, 0))
                throw new RuntimeException("bindService() failed");
        } catch (Exception e) {
            UI.showToast(this,
                getString(
                    e.getMessage() != null &&
                        e.getMessage().contains("app is in background") ?
                        R.string.error_termux_service_start_failed_bg :
                        R.string.error_termux_service_start_failed_general
                ),
                true);
            mIsInvalidState = true;
        }

//        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
//            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
//
//            v.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, 0);
//
//            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
//            params.bottomMargin = Math.max(systemBarsInsets.bottom, imeInsets.bottom);
//            v.setLayoutParams(params);
//            return WindowInsetsCompat.CONSUMED;
//        });
    }

    /**
     * Activity 变为可见时的回调。
     */
    @Override
    public void onStart() {
        super.onStart();
        if (mIsInvalidState) return;

        mIsVisible = true;

        if (mTerminalSessionActivityClient != null)
            mTerminalSessionActivityClient.onStart();
    }

    /**
     * Activity 进入前台时的回调。
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mIsInvalidState) return;

        if (mTerminalViewClient != null)
            mTerminalViewClient.onResume();

        mIsOnResumeAfterOnCreate = false;
    }

    /**
     * Activity 变为不可见时的回调。
     */
    @Override
    protected void onStop() {
        super.onStop();

        if (mIsInvalidState) return;

        mIsVisible = false;

        if (mTerminalSessionActivityClient != null)
            mTerminalSessionActivityClient.onStop();

        if (mTerminalViewClient != null)
            mTerminalViewClient.onStop();
        binding.drawerLayout.closeDrawers();
    }

    /**
     * Activity 销毁前的回调。
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mIsInvalidState) return;

        if (mTerminalService != null) {
            // 不要让服务和会话客户端持有对 Activity 的引用
            mTerminalService.unsetTermuxTerminalSessionClient();
            mTerminalService = null;
        }

        try {
            unbindService(this);
        } catch (Exception e) {
            // 忽略异常
        }
    }

    /**
     * 保存实例状态的回调。
     *
     * @param savedInstanceState 用于保存状态的 Bundle。
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        saveTerminalToolbarTextInput(savedInstanceState);
        savedInstanceState.putBoolean(ARG_ACTIVITY_RECREATED, true);
    }


    /**
     * {@link ServiceConnection} 接口的一部分。在 {@link #onCreate(Bundle)} 中通过
     * {@link #bindService(Intent, ServiceConnection, int)} 绑定服务，这将导致此回调方法的调用。
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        mTerminalService = ((TerminalService.LocalBinder) service).service;

        setTermuxSessionsListView();

        setIntent(null);

        if (mTerminalService.isTermuxSessionsEmpty()) {
            if (mIsVisible) {
                try {
                    mTerminalSessionActivityClient.addNewSession(null);
                } catch (WindowManager.BadTokenException e) {
                    // Activity 已结束 - 忽略
                }
            } else {
                // 服务在非前台时连接 - 直接退出
                finishActivityIfNotFinishing();
            }
        } else {
            mTerminalSessionActivityClient.setCurrentSession(
                mTerminalSessionActivityClient.getCurrentStoredSessionOrLast()
            );
        }

        // 更新 {@link TerminalSession} 和 {@link TerminalEmulator} 的客户端
        mTerminalService.setTermuxTerminalSessionClient(mTerminalSessionActivityClient);
    }

    /**
     * 服务断开连接时的回调。
     *
     * @param name 断开连接的服务组件名称。
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        // 响应从 {@link TerminalService} 通知操作中停止的请求
        finishActivityIfNotFinishing();
    }

    /**
     * 重新加载属性。
     */
    private void reloadProperties() {
        if (mTerminalViewClient != null)
            mTerminalViewClient.onReloadProperties();
    }

    /**
     * 设置 Termux 终端视图和客户端。
     */
    private void setTermuxTerminalViewAndClients() {
        // 设置 Termux 终端视图和会话客户端
        mTerminalSessionActivityClient = new TerminalSessionActivityClient(this);
        mTerminalViewClient = new TerminalViewClient(
            this, mTerminalSessionActivityClient
        );

        // 设置 Termux 终端视图
        binding.terminalView.setTerminalViewClient(mTerminalViewClient);

        mTerminalViewClient.onCreate();

        if (mTerminalSessionActivityClient != null)
            mTerminalSessionActivityClient.onCreate();
    }

    /**
     * 设置 Termux 会话列表视图。
     */
    private void setTermuxSessionsListView() {
        ListView termuxSessionsListView = binding.terminalSessionsList;
        mTermuxSessionListViewController = new SessionsListViewController(
            this, mTerminalService.getTermuxSessions()
        );
        termuxSessionsListView.setAdapter(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemClickListener(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemLongClickListener(mTermuxSessionListViewController);
    }

    /**
     * 设置终端工具栏视图。
     *
     * @param savedInstanceState 如果 Activity 被重新创建，则包含先前保存的状态。
     */
    private void setTerminalToolbarView(Bundle savedInstanceState) {
        mTermuxTerminalExtraKeys = new TermuxTerminalExtraKeys(
            this,
            binding.terminalView,
            mTerminalViewClient,
            mTerminalSessionActivityClient
        );

        final ViewPager terminalToolbarViewPager = binding.terminalToolbarViewPager;
        if (mPreferences.shouldShowTerminalToolbar())
            terminalToolbarViewPager.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        mTerminalToolbarDefaultHeight = layoutParams.height;

        setTerminalToolbarHeight();

        String savedTextInput = null;
        if (savedInstanceState != null)
            savedTextInput = savedInstanceState.getString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT);

        terminalToolbarViewPager.setAdapter(
            new TerminalToolbarViewPager.PageAdapter(this, savedTextInput)
        );
        terminalToolbarViewPager.addOnPageChangeListener(
            new TerminalToolbarViewPager.OnPageChangeListener(this)
        );
    }

    /**
     * 设置终端工具栏高度。
     */
    private void setTerminalToolbarHeight() {
        final ViewPager terminalToolbarViewPager = binding.terminalToolbarViewPager;

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *
            (
                mTermuxTerminalExtraKeys.getExtraKeysInfo() == null ?
                    0 :
                    mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length
            ) * Constants.DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR);
        terminalToolbarViewPager.setLayoutParams(layoutParams);
    }

    /**
     * 切换终端工具栏的可见性。
     */
    public void toggleTerminalToolbar() {
        final boolean showNow = mPreferences.toogleShowTerminalToolbar();
        UI.showToast(this, (
            showNow ?
                getString(R.string.msg_enabling_terminal_toolbar) :
                getString(R.string.msg_disabling_terminal_toolbar)
        ), true);
        binding.terminalToolbarViewPager.setVisibility(showNow ? View.VISIBLE : View.GONE);
        if (showNow && isTerminalToolbarTextInputViewSelected()) {
            // 如果刚刚显示，则聚焦文本输入视图
            findViewById(R.id.terminal_toolbar_text_input).requestFocus();
        }
    }

    /**
     * 保存终端工具栏的文本输入。
     *
     * @param savedInstanceState 用于保存状态的 Bundle。
     */
    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        final EditText textInputView = findViewById(R.id.terminal_toolbar_text_input);
        if (textInputView != null) {
            String textInput = textInputView.getText().toString();
            if (!textInput.isEmpty())
                savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);
        }
    }

    /**
     * 设置设置按钮视图。
     */
    private void setSettingsButtonView() {
        binding.settingsButton.setOnClickListener(
            v -> startActivity(new Intent(this, SettingsActivity.class))
        );
    }

    /**
     * 设置新会话按钮视图。
     */
    private void setNewSessionButtonView() {
        View newSessionButton = binding.newSessionButton;
        newSessionButton.setOnClickListener(
            v -> mTerminalSessionActivityClient.addNewSession(null)
        );
        newSessionButton.setOnLongClickListener(v -> {
            TextInputDialogUtils.textInput(
                MainActivity.this,
                R.string.title_create_named_session,
                null,
                R.string.action_create_named_session_confirm,
                text -> mTerminalSessionActivityClient.addNewSession(text),
                -1,
                null,
                -1,
                null,
                null
            );
            return true;
        });
    }

    /**
     * 设置切换键盘按钮视图。
     */
    private void setToggleKeyboardView() {
        binding.toggleKeyboardButton.setOnClickListener(v -> {
            mTerminalViewClient.onToggleSoftKeyboardRequest();
            binding.drawerLayout.closeDrawers();
        });

        binding.toggleKeyboardButton.setOnLongClickListener(v -> {
            toggleTerminalToolbar();
            return true;
        });
    }

    /**
     * 如果 Activity 未结束，则结束它。
     */
    public void finishActivityIfNotFinishing() {
        // 防止在从多个地方调用时重复调用 finish()
        if (!MainActivity.this.isFinishing())
            finish();
    }


    /**
     * 创建上下文菜单时的回调。
     *
     * @param menu     上下文菜单。
     * @param v        视图。
     * @param menuInfo 菜单信息。
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentSession();
        if (currentSession == null) return;

        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(binding.terminalView.getStoredSelectedText()))
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE, getString(R.string.action_kill_process, getCurrentSession().getPid())).setEnabled(currentSession.isRunning());
        menu.add(Menu.NONE, CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON, Menu.NONE, R.string.action_toggle_keep_screen_on)
            .setCheckable(true).setChecked(mPreferences.shouldKeepScreenOn());
        menu.add(Menu.NONE, CONTEXT_MENU_SETTINGS_ID, Menu.NONE, R.string.action_open_settings);
    }

    /**
     * Hook 系统菜单以显示上下文菜单。
     *
     * @param menu 选项菜单。
     * @return 总是返回 false，因为我们显示的是上下文菜单。
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        binding.terminalView.showContextMenu();
        return false;
    }

    /**
     * 上下文菜单项被选中时的回调。
     *
     * @param item 被选中的菜单项。
     * @return 如果事件被处理则返回 true，否则返回 false。
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentSession();

        return switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_URL_ID -> {
                mTerminalViewClient.showUrlSelection();
                yield true;
            }
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID -> {
                mTerminalViewClient.shareSessionTranscript();
                yield true;
            }
            case CONTEXT_MENU_SHARE_SELECTED_TEXT -> {
                mTerminalViewClient.shareSelectedText();
                yield true;
            }
            case CONTEXT_MENU_RESET_TERMINAL_ID -> {
                onResetTerminalSession(session);
                yield true;
            }
            case CONTEXT_MENU_KILL_PROCESS_ID -> {
                showKillSessionDialog(session);
                yield true;
            }
            case CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON -> {
                toggleKeepScreenOn();
                yield true;
            }
            case CONTEXT_MENU_SETTINGS_ID -> {
                startActivity(new Intent(this, SettingsActivity.class));
                yield true;
            }
            default -> super.onContextItemSelected(item);
        };
    }

    /**
     * 上下文菜单关闭时的回调。
     *
     * @param menu 关闭的菜单。
     */
    @Override
    public void onContextMenuClosed(@NonNull Menu menu) {
        super.onContextMenuClosed(menu);
        // 如果按返回键关闭，onContextMenuClosed() 会被触发两次，原因未知
        binding.terminalView.onContextMenuClosed();
    }

    /**
     * 显示杀死会话的对话框。
     *
     * @param session 要杀死的会话。
     */
    private void showKillSessionDialog(TerminalSession session) {
        if (session == null) return;

        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.title_confirm_kill_process);
        b.setPositiveButton(R.string.yes, (dialog, id) -> {
            dialog.dismiss();
            session.finishIfRunning();
        });
        b.setNegativeButton(R.string.no, null);
        b.show();
    }

    /**
     * 重置终端会话。
     *
     * @param session 要重置的会话。
     */
    private void onResetTerminalSession(TerminalSession session) {
        if (session != null) {
            session.reset();
            UI.showToast(this, getString(R.string.msg_terminal_reset), true);

            if (mTerminalSessionActivityClient != null)
                mTerminalSessionActivityClient.onResetTerminalSession();
        }
    }

    /**
     * 切换保持屏幕常亮。
     */
    private void toggleKeepScreenOn() {
        if (binding.terminalView.getKeepScreenOn()) {
            binding.terminalView.setKeepScreenOn(false);
            mPreferences.setKeepScreenOn(false);
        } else {
            binding.terminalView.setKeepScreenOn(true);
            mPreferences.setKeepScreenOn(true);
        }
    }

    /**
     * 获取额外按键视图。
     *
     * @return 额外按键视图。
     */
    public ExtraKeysView getExtraKeysView() {
        return mExtraKeysView;
    }

    /**
     * 获取 Termux 终端额外按键。
     *
     * @return Termux 终端额外按键。
     */
    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {
        return mTermuxTerminalExtraKeys;
    }

    /**
     * 设置额外按键视图。
     *
     * @param extraKeysView 额外按键视图。
     */
    public void setExtraKeysView(ExtraKeysView extraKeysView) {
        mExtraKeysView = extraKeysView;
    }

    /**
     * 获取抽屉布局。
     *
     * @return 抽屉布局。
     */
    public DrawerLayout getDrawer() {
        return binding.drawerLayout;
    }

    /**
     * 获取终端工具栏的 ViewPager。
     *
     * @return 终端工具栏的 ViewPager。
     */
    public ViewPager getTerminalToolbarViewPager() {
        return binding.terminalToolbarViewPager;
    }

    /**
     * 获取终端工具栏的默认高度。
     *
     * @return 终端工具栏的默认高度。
     */
    public float getTerminalToolbarDefaultHeight() {
        return mTerminalToolbarDefaultHeight;
    }

    /**
     * 检查终端视图是否被选中。
     *
     * @return 如果终端视图被选中则返回 true，否则返回 false。
     */
    public boolean isTerminalViewSelected() {
        return binding.terminalToolbarViewPager.getCurrentItem() == 0;
    }

    /**
     * 检查终端工具栏的文本输入视图是否被选中。
     *
     * @return 如果终端工具栏的文本输入视图被选中则返回 true，否则返回 false。
     */
    public boolean isTerminalToolbarTextInputViewSelected() {
        return binding.terminalToolbarViewPager.getCurrentItem() == 1;
    }

    /**
     * 通知 Termux 会话列表已更新。
     */
    public void termuxSessionListNotifyUpdated() {
        mTermuxSessionListViewController.notifyDataSetChanged();
    }

    /**
     * 检查 Activity 是否可见。
     *
     * @return 如果 Activity 可见则返回 true，否则返回 false。
     */
    public boolean isVisible() {
        return mIsVisible;
    }

    /**
     * 检查 onResume 是否在 onCreate 之后调用。
     *
     * @return 如果 onResume 在 onCreate 之后调用则返回 true，否则返回 false。
     */
    public boolean isOnResumeAfterOnCreate() {
        return mIsOnResumeAfterOnCreate;
    }

    /**
     * 检查 Activity 是否被重新创建。
     *
     * @return 如果 Activity 被重新创建则返回 true，否则返回 false。
     */
    public boolean isActivityRecreated() {
        return mIsActivityRecreated;
    }

    /**
     * 获取 Termux 服务。
     *
     * @return Termux 服务。
     */
    public TerminalService getTermuxService() {
        return mTerminalService;
    }

    /**
     * 获取终端视图。
     *
     * @return 终端视图。
     */
    public TerminalView getTerminalView() {
        return binding.terminalView;
    }

    /**
     * 获取 Termux 终端会话客户端。
     *
     * @return Termux 终端会话客户端。
     */
    public TerminalSessionActivityClient getTermuxTerminalSessionClient() {
        return mTerminalSessionActivityClient;
    }

    /**
     * 获取当前会话。
     *
     * @return 当前会话，如果不存在则返回 null。
     */
    @Nullable
    public TerminalSession getCurrentSession() {
        return binding.terminalView.getCurrentSession();
    }

    /**
     * 获取应用的首选项。
     *
     * @return 应用的首选项。
     */
    public TermuxAppSharedPreferences getPreferences() {
        return mPreferences;
    }

}
