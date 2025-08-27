package awkoo.terminal.shared.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import awkoo.terminal.shared.errors.Error;


public class ActivityUtils {

    /**
     * Wrapper for {@link #startActivity(Context, Intent, boolean, boolean)}.
     */
    public static void startActivity(@NonNull Context context, @NonNull Intent intent) {
        startActivity(context, intent, true, true);
    }

    /**
     * Start an {@link Activity}.
     *
     * @param context          The context for operations.
     * @param intent           The {@link Intent} to send to start the activity.
     * @param logErrorMessage  If an error message should be logged if failed to start activity.
     * @param showErrorMessage If an error message toast should be shown if failed to start activity
     *                         in addition to logging a message. The {@code context} must not be
     *                         {@code null}.
     * @return Returns the {@code error} if starting activity was not successful, otherwise {@code null}.
     */
    public static Error startActivity(Context context, @NonNull Intent intent,
                                      boolean logErrorMessage, boolean showErrorMessage) {
        Error error;
        String activityName = intent.getComponent() != null ? intent.getComponent().getClassName() : "Unknown";

        if (context == null) {
            error = ActivityErrno.ERRNO_STARTING_ACTIVITY_WITH_NULL_CONTEXT.getError(activityName);
            if (logErrorMessage)
                error.showToast(null);
            return error;
        }

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            error = ActivityErrno.ERRNO_START_ACTIVITY_FAILED_WITH_EXCEPTION.getError(e, activityName, e.getMessage());
            if (logErrorMessage)
                error.showToast(showErrorMessage ? context : null);
            return error;
        }

        return null;
    }


}
