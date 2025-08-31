package awkoo.terminal.app.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
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
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import awkoo.terminal.R;
import awkoo.terminal.app.TerminalService;
import awkoo.terminal.app.terminal.TermuxSessionsListViewController;
import awkoo.terminal.app.terminal.TermuxTerminalSessionActivityClient;
import awkoo.terminal.app.terminal.TermuxTerminalViewClient;
import awkoo.terminal.app.terminal.io.TerminalToolbarViewPager;
import awkoo.terminal.app.terminal.io.TermuxTerminalExtraKeys;
import awkoo.terminal.databinding.ActivityMainBinding;
import awkoo.terminal.shared.activity.ActivityUtils;
import awkoo.terminal.shared.data.DataUtils;
import awkoo.terminal.shared.termux.extrakeys.ExtraKeysView;
import awkoo.terminal.shared.termux.interact.TextInputDialogUtils;
import awkoo.terminal.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import awkoo.terminal.shared.termux.settings.properties.TermuxAppSharedProperties;
import awkoo.terminal.shared.view.ViewUtils;
import awkoo.terminal.terminal.TerminalSession;
import awkoo.terminal.terminal.TerminalSessionClient;
import awkoo.terminal.utils.UI;
import awkoo.terminal.view.TerminalView;
import awkoo.terminal.view.TerminalViewClient;

/**
 * A terminal emulator activity.
 * <p/>
 * See
 * <ul>
 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>
 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>
 * </ul>
 * about memory leaks.
 */
public final class MainActivity extends AppCompatActivity implements ServiceConnection {

    ActivityMainBinding binding;

    /**
     * The connection to the {@link TerminalService}. Requested in {@link #onCreate(Bundle)} with a call to
     * {@link #bindService(Intent, ServiceConnection, int)}, and obtained and stored in
     * {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    TerminalService mTerminalService;

    /**
     * The {@link TerminalViewClient} interface implementation to allow for communication between
     * {@link TerminalView} and {@link MainActivity}.
     */
    TermuxTerminalViewClient mTermuxTerminalViewClient;

    /**
     * The {@link TerminalSessionClient} interface implementation to allow for communication between
     * {@link TerminalSession} and {@link MainActivity}.
     */
    TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    /**
     * Termux app shared preferences manager.
     */
    private TermuxAppSharedPreferences mPreferences;

    /**
     * Termux app SharedProperties loaded from termux.properties
     */
    private TermuxAppSharedProperties mProperties;

    /**
     * The terminal extra keys view.
     */
    ExtraKeysView mExtraKeysView;

    /**
     * The client for the {@link #mExtraKeysView}.
     */
    TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;

    /**
     * The termux sessions list controller.
     */
    TermuxSessionsListViewController mTermuxSessionListViewController;


    /**
     * If between onResume() and onStop(). Note that only one session is in the foreground of the terminal view at the
     * time, so if the session causing a change is not in the foreground it should probably be treated as background.
     */
    private boolean mIsVisible;

    /**
     * If onResume() was called after onCreate().
     */
    private boolean mIsOnResumeAfterOnCreate = false;

    private boolean mIsActivityRecreated = false;

    /**
     * The {@link MainActivity} is in an invalid state and must not be run.
     */
    private boolean mIsInvalidState;

    private float mTerminalToolbarDefaultHeight;


    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 2;
    private static final int CONTEXT_MENU_AUTOFILL_USERNAME = 3;
    private static final int CONTEXT_MENU_AUTOFILL_PASSWORD = 4;
    private static final int CONTEXT_MENU_RESET_TERMINAL_ID = 5;
    private static final int CONTEXT_MENU_KILL_PROCESS_ID = 6;
    private static final int CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON = 7;
    private static final int CONTEXT_MENU_SETTINGS_ID = 8;

    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    private static final String ARG_ACTIVITY_RECREATED = "activity_recreated";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        mIsOnResumeAfterOnCreate = true;

        if (savedInstanceState != null)
            mIsActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false);

        // Load Termux app SharedProperties from disk
        mProperties = TermuxAppSharedProperties.getProperties();
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
        // Load termux shared preferences
        // This will also fail if TermuxConstants.TERMUX_PACKAGE_NAME does not equal applicationId
        mPreferences = TermuxAppSharedPreferences.build(this);

        setMargins();
        setTermuxTerminalViewAndClients();
        setTerminalToolbarView(savedInstanceState);
        setSettingsButtonView();
        setNewSessionButtonView();
        setToggleKeyboardView();

        registerForContextMenu(binding.terminalView);

        try {
            // Start the {@link TerminalService} and make it run regardless of who is bound to it
            Intent serviceIntent = new Intent(this, TerminalService.class);
            startService(serviceIntent);

            // Attempt to bind to the service, this will call the {@link #onServiceConnected(ComponentName, IBinder)}
            // callback if it succeeds.
            if (!bindService(serviceIntent, this, 0))
                throw new RuntimeException("bindService() failed");
        } catch (Exception e) {
            UI.showToast(this,
                getString(e.getMessage() != null && e.getMessage().contains("app is in background") ?
                    R.string.error_termux_service_start_failed_bg : R.string.error_termux_service_start_failed_general),
                true);
            mIsInvalidState = true;
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, 0);

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = Math.max(systemBarsInsets.bottom, imeInsets.bottom);
            v.setLayoutParams(params);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mIsInvalidState) return;

        mIsVisible = true;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), (isGranted) -> {
                mTerminalService.updateNotification();
            }).launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsInvalidState) return;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onResume();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onResume();

        mIsOnResumeAfterOnCreate = false;
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mIsInvalidState) return;

        mIsVisible = false;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStop();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStop();
        binding.drawerLayout.closeDrawers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mIsInvalidState) return;

        if (mTerminalService != null) {
            // Do not leave service and session clients with references to activity.
            mTerminalService.unsetTermuxTerminalSessionClient();
            mTerminalService = null;
        }

        try {
            unbindService(this);
        } catch (Exception e) {
            // ignore.
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        saveTerminalToolbarTextInput(savedInstanceState);
        savedInstanceState.putBoolean(ARG_ACTIVITY_RECREATED, true);
    }


    /**
     * Part of the {@link ServiceConnection} interface. The service is bound with
     * {@link #bindService(Intent, ServiceConnection, int)} in {@link #onCreate(Bundle)} which will cause a call to this
     * callback method.
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        mTerminalService = ((TerminalService.LocalBinder) service).service;

        setTermuxSessionsListView();

        final Intent intent = getIntent();
        setIntent(null);

        if (mTerminalService.isTermuxSessionsEmpty()) {
            if (mIsVisible) {
                try {
                    mTermuxTerminalSessionActivityClient.addNewSession(null);
                } catch (WindowManager.BadTokenException e) {
                    // Activity finished - ignore.
                }
            } else {
                // The service connected while not in foreground - just bail out.
                finishActivityIfNotFinishing();
            }
        } else {
            // If termux was started from launcher "New session" shortcut and activity is recreated,
            // then the original intent will be re-delivered, resulting in a new session being re-added
            // each time.
            if (!mIsActivityRecreated && intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
                // Android 7.1 app shortcut from res/xml/shortcuts.xml.
                mTermuxTerminalSessionActivityClient.addNewSession(null);
            } else {
                mTermuxTerminalSessionActivityClient.setCurrentSession(mTermuxTerminalSessionActivityClient.getCurrentStoredSessionOrLast());
            }
        }

        // Update the {@link TerminalSession} and {@link TerminalEmulator} clients.
        mTerminalService.setTermuxTerminalSessionClient(mTermuxTerminalSessionActivityClient);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Respect being stopped from the {@link TerminalService} notification action.
        finishActivityIfNotFinishing();
    }


    private void reloadProperties() {
        mProperties.loadTermuxPropertiesFromDisk();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadProperties();
    }


    private void setMargins() {
        RelativeLayout relativeLayout = binding.activityTermuxRootRelativeLayout;
        int marginHorizontal = mProperties.getTerminalMarginHorizontal();
        int marginVertical = mProperties.getTerminalMarginVertical();
        ViewUtils.setLayoutMarginsInDp(relativeLayout, marginHorizontal, marginVertical, marginHorizontal, marginVertical);
    }


    private void setTermuxTerminalViewAndClients() {
        // Set termux terminal view and session clients
        mTermuxTerminalSessionActivityClient = new TermuxTerminalSessionActivityClient(this);
        mTermuxTerminalViewClient = new TermuxTerminalViewClient(this, mTermuxTerminalSessionActivityClient);

        // Set termux terminal view
        binding.terminalView.setTerminalViewClient(mTermuxTerminalViewClient);

        mTermuxTerminalViewClient.onCreate();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onCreate();
    }

    private void setTermuxSessionsListView() {
        ListView termuxSessionsListView = binding.terminalSessionsList;
        mTermuxSessionListViewController = new TermuxSessionsListViewController(this, mTerminalService.getTermuxSessions());
        termuxSessionsListView.setAdapter(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemClickListener(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemLongClickListener(mTermuxSessionListViewController);
    }


    private void setTerminalToolbarView(Bundle savedInstanceState) {
        mTermuxTerminalExtraKeys = new TermuxTerminalExtraKeys(
            this,
            binding.terminalView,
            mTermuxTerminalViewClient,
            mTermuxTerminalSessionActivityClient
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

        terminalToolbarViewPager.setAdapter(new TerminalToolbarViewPager.PageAdapter(this, savedTextInput));
        terminalToolbarViewPager.addOnPageChangeListener(new TerminalToolbarViewPager.OnPageChangeListener(this, terminalToolbarViewPager));
    }

    private void setTerminalToolbarHeight() {
        final ViewPager terminalToolbarViewPager = binding.terminalToolbarViewPager;

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *
            (mTermuxTerminalExtraKeys.getExtraKeysInfo() == null ? 0 : mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length) *
            mProperties.getTerminalToolbarHeightScaleFactor());
        terminalToolbarViewPager.setLayoutParams(layoutParams);
    }

    public void toggleTerminalToolbar() {
        final boolean showNow = mPreferences.toogleShowTerminalToolbar();
        UI.showToast(this, (showNow ? getString(R.string.msg_enabling_terminal_toolbar) : getString(R.string.msg_disabling_terminal_toolbar)), true);
        binding.terminalToolbarViewPager.setVisibility(showNow ? View.VISIBLE : View.GONE);
        if (showNow && isTerminalToolbarTextInputViewSelected()) {
            // Focus the text input view if just revealed.
            findViewById(R.id.terminal_toolbar_text_input).requestFocus();
        }
    }

    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        final EditText textInputView = findViewById(R.id.terminal_toolbar_text_input);
        if (textInputView != null) {
            String textInput = textInputView.getText().toString();
            if (!textInput.isEmpty())
                savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);
        }
    }


    private void setSettingsButtonView() {
        binding.settingsButton.setOnClickListener(
            v -> ActivityUtils.startActivity(
                this,
                new Intent(
                    this,
                    SettingsActivity.class
                )
            )
        );
    }

    private void setNewSessionButtonView() {
        View newSessionButton = binding.newSessionButton;
        newSessionButton.setOnClickListener(v -> mTermuxTerminalSessionActivityClient.addNewSession(null));
        newSessionButton.setOnLongClickListener(v -> {
            TextInputDialogUtils.textInput(
                MainActivity.this,
                R.string.title_create_named_session,
                null,
                R.string.action_create_named_session_confirm,
                text -> mTermuxTerminalSessionActivityClient.addNewSession(text),
                -1,
                null,
                -1,
                null,
                null
            );
            return true;
        });
    }

    private void setToggleKeyboardView() {
        binding.toggleKeyboardButton.setOnClickListener(v -> {
            mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
            binding.drawerLayout.closeDrawers();
        });

        binding.toggleKeyboardButton.setOnLongClickListener(v -> {
            toggleTerminalToolbar();
            return true;
        });
    }

    public void finishActivityIfNotFinishing() {
        // prevent duplicate calls to finish() if called from multiple places
        if (!MainActivity.this.isFinishing()) {
            finish();
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentSession();
        if (currentSession == null) return;

        boolean autoFillEnabled = binding.terminalView.isAutoFillEnabled();

        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(binding.terminalView.getStoredSelectedText()))
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_USERNAME, Menu.NONE, R.string.action_autofill_username);
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_PASSWORD, Menu.NONE, R.string.action_autofill_password);
        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE, getResources().getString(R.string.action_kill_process, getCurrentSession().getPid())).setEnabled(currentSession.isRunning());
        menu.add(Menu.NONE, CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON, Menu.NONE, R.string.action_toggle_keep_screen_on).setCheckable(true).setChecked(mPreferences.shouldKeepScreenOn());
        menu.add(Menu.NONE, CONTEXT_MENU_SETTINGS_ID, Menu.NONE, R.string.action_open_settings);
    }

    /**
     * Hook system menu to show context menu instead.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        binding.terminalView.showContextMenu();
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentSession();

        return switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_URL_ID -> {
                mTermuxTerminalViewClient.showUrlSelection();
                yield true;
            }
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID -> {
                mTermuxTerminalViewClient.shareSessionTranscript();
                yield true;
            }
            case CONTEXT_MENU_SHARE_SELECTED_TEXT -> {
                mTermuxTerminalViewClient.shareSelectedText();
                yield true;
            }
            case CONTEXT_MENU_AUTOFILL_USERNAME -> {
                binding.terminalView.requestAutoFillUsername();
                yield true;
            }
            case CONTEXT_MENU_AUTOFILL_PASSWORD -> {
                binding.terminalView.requestAutoFillPassword();
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
                ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
                yield true;
            }
            default -> super.onContextItemSelected(item);
        };
    }

    @Override
    public void onContextMenuClosed(@NonNull Menu menu) {
        super.onContextMenuClosed(menu);
        // onContextMenuClosed() is triggered twice if back button is pressed to dismiss instead of tap for some reason
        binding.terminalView.onContextMenuClosed();
    }

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

    private void onResetTerminalSession(TerminalSession session) {
        if (session != null) {
            session.reset();
            UI.showToast(this, getString(R.string.msg_terminal_reset), true);

            if (mTermuxTerminalSessionActivityClient != null)
                mTermuxTerminalSessionActivityClient.onResetTerminalSession();
        }
    }

    private void toggleKeepScreenOn() {
        if (binding.terminalView.getKeepScreenOn()) {
            binding.terminalView.setKeepScreenOn(false);
            mPreferences.setKeepScreenOn(false);
        } else {
            binding.terminalView.setKeepScreenOn(true);
            mPreferences.setKeepScreenOn(true);
        }
    }


    public ExtraKeysView getExtraKeysView() {
        return mExtraKeysView;
    }

    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {
        return mTermuxTerminalExtraKeys;
    }

    public void setExtraKeysView(ExtraKeysView extraKeysView) {
        mExtraKeysView = extraKeysView;
    }

    public DrawerLayout getDrawer() {
        return binding.drawerLayout;
    }


    public ViewPager getTerminalToolbarViewPager() {
        return binding.terminalToolbarViewPager;
    }

    public float getTerminalToolbarDefaultHeight() {
        return mTerminalToolbarDefaultHeight;
    }

    public boolean isTerminalViewSelected() {
        return binding.terminalToolbarViewPager.getCurrentItem() == 0;
    }

    public boolean isTerminalToolbarTextInputViewSelected() {
        return binding.terminalToolbarViewPager.getCurrentItem() == 1;
    }


    public void termuxSessionListNotifyUpdated() {
        mTermuxSessionListViewController.notifyDataSetChanged();
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public boolean isOnResumeAfterOnCreate() {
        return mIsOnResumeAfterOnCreate;
    }

    public boolean isActivityRecreated() {
        return mIsActivityRecreated;
    }


    public TerminalService getTermuxService() {
        return mTerminalService;
    }

    public TerminalView getTerminalView() {
        return binding.terminalView;
    }

    public TermuxTerminalSessionActivityClient getTermuxTerminalSessionClient() {
        return mTermuxTerminalSessionActivityClient;
    }

    @Nullable
    public TerminalSession getCurrentSession() {
        return binding.terminalView.getCurrentSession();
    }

    public TermuxAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxAppSharedProperties getProperties() {
        return mProperties;
    }

}
