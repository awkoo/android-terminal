package com.termux.shared.termux.shell;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.termux.shared.shell.command.ExecutionCommand;

import java.util.ArrayList;
import java.util.List;

public class TermuxShellManager {

    private static TermuxShellManager shellManager;

    private static int SHELL_ID = 0;

    protected final Context mContext;

    /**
     * The foreground TermuxSessions which this service manages.
     * Note that this list is observed by an activity, like TermuxActivity.mTermuxSessionListViewController,
     * so any changes must be made on the UI thread and followed by a call to
     * {@link ArrayAdapter#notifyDataSetChanged()}.
     */
    public final List<TermuxSession> mTermuxSessions = new ArrayList<>();

    /**
     * The {@link ExecutionCommand.Runner#APP_SHELL} number after app process was started/restarted.
     */
    public static int APP_SHELL_NUMBER_SINCE_APP_START;

    /**
     * The {@link ExecutionCommand.Runner#TERMINAL_SESSION} number after app process was started/restarted.
     */
    public static int TERMINAL_SESSION_NUMBER_SINCE_APP_START;



    public TermuxShellManager(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Initialize the {@link #shellManager}.
     *
     * @param context The {@link Context} for operations.
     */
    public static void init(@NonNull Context context) {
        if (shellManager == null)
            shellManager = new TermuxShellManager(context);
    }

    /**
     * Get the {@link #shellManager}.
     *
     * @return Returns the {@link TermuxShellManager}.
     */
    public static TermuxShellManager getShellManager() {
        return shellManager;
    }

    public static void onAppExit(@NonNull Context context) {
        // Ensure any shells started after boot have valid ENV_SHELL_CMD__APP_SHELL_NUMBER_SINCE_APP_START and
        // ENV_SHELL_CMD__TERMINAL_SESSION_NUMBER_SINCE_APP_START exported
        APP_SHELL_NUMBER_SINCE_APP_START = 0;
        TERMINAL_SESSION_NUMBER_SINCE_APP_START = 0;
    }

    public static synchronized int getNextShellId() {
        return SHELL_ID++;
    }

}
