package awkoo.terminal.utils.errors;

import android.app.Activity;

import androidx.annotation.NonNull;

/**
 * The {@link Class} that defines error messages and codes.
 *
 * @param type    The errno type.
 * @param code    The errno code.
 * @param message The errno message.
 */
public record Errno(String type, int code, String message) {

    public static final String TYPE = "Error";


    public static final Errno ERRNO_SUCCESS = new Errno(TYPE, Activity.RESULT_OK, "Success");
    public static final Errno ERRNO_FAILED = new Errno(TYPE, Activity.RESULT_FIRST_USER + 1, "Failed");

    public Errno(@NonNull final String type, final int code, @NonNull final String message) {
        this.type = type;
        this.code = code;
        this.message = message;
    }

    @Override
    @NonNull
    public String message() {
        return message;
    }


}
