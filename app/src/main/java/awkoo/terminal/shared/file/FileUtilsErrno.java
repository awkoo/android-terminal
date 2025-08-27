package awkoo.terminal.shared.file;

import awkoo.terminal.shared.errors.Errno;

/**
 * The {@link Class} that defines FileUtils error messages and codes.
 */
public class FileUtilsErrno extends Errno {

    public static final String TYPE = "FileUtils Error";


    /* Errors for invalid or not found files at path (150-200) */
    public static final Errno ERRNO_FILE_NOT_FOUND_AT_PATH = new Errno(TYPE, 150, "The %1$s not found at path \"%2$s\".");

    public static final Errno ERRNO_NON_DIRECTORY_FILE_FOUND = new Errno(TYPE, 154, "Non-directory file found at %1$s path \"%2$s\".");

    public static final Errno ERRNO_FILE_NOT_AN_ALLOWED_FILE_TYPE = new Errno(TYPE, 158, "The %1$s found at path \"%2$s\" of type \"%3$s\" is not one of allowed file types \"%4$s\".");

    public static final Errno ERRNO_VALIDATE_DIRECTORY_EXISTENCE_AND_PERMISSIONS_FAILED_WITH_EXCEPTION = new Errno(TYPE, 161, "Validating directory existence and permissions of %1$s at path \"%2$s\" failed.\nException: %3$s");


    /* Errors for file creation (200-250) */
    public static final Errno ERRNO_CREATING_FILE_FAILED = new Errno(TYPE, 200, "Creating %1$s at path \"%2$s\" failed.");


    public static final Errno ERRNO_DELETING_FILE_FAILED_WITH_EXCEPTION = new Errno(TYPE, 301, "Deleting %1$s at path \"%2$s\" failed.\nException: %3$s");
    public static final Errno ERRNO_FILE_STILL_EXISTS_AFTER_DELETING = new Errno(TYPE, 303, "The %1$s still exists after deleting it from \"%2$s\".");


    /* Errors for invalid file permissions (400-450) */
    public static final Errno ERRNO_INVALID_FILE_PERMISSIONS_STRING_TO_CHECK = new Errno(TYPE, 400, "The file permission string to check is invalid.");
    public static final Errno ERRNO_FILE_NOT_READABLE = new Errno(TYPE, 401, "The %1$s at path \"%2$s\" is not readable. Permission Denied.");
    public static final Errno ERRNO_FILE_NOT_WRITABLE = new Errno(TYPE, 403, "The %1$s at path \"%2$s\" is not writable. Permission Denied.");
    public static final Errno ERRNO_FILE_NOT_EXECUTABLE = new Errno(TYPE, 405, "The %1$s at path \"%2$s\" is not executable. Permission Denied.");


    FileUtilsErrno(final String type, final int code, final String message) {
        super(type, code, message);
    }


}
