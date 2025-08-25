package com.termux.shared.file;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.io.RecursiveDeleteOption;
import com.termux.shared.data.DataUtils;
import com.termux.shared.errors.Error;
import com.termux.shared.errors.FunctionErrno;
import com.termux.shared.file.filesystem.FileType;
import com.termux.shared.file.filesystem.FileTypes;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class FileUtils {

    /**
     * Get canonical path.
     * <p>
     * If path is already an absolute path, then it is used as is to get canonical path.
     * If path is not an absolute path and {code prefixForNonAbsolutePath} is not {@code null}, then
     * {code prefixForNonAbsolutePath} + "/" is prefixed before path before getting canonical path.
     * If path is not an absolute path and {code prefixForNonAbsolutePath} is {@code null}, then
     * "/" is prefixed before path before getting canonical path.
     * <p>
     * If an exception is raised to get the canonical path, then absolute path is returned.
     *
     * @param path                     The {@code path} to convert.
     * @param prefixForNonAbsolutePath Optional prefix path to prefix before non-absolute paths. This
     *                                 can be set to {@code null} if non-absolute paths should
     *                                 be prefixed with "/". The call to {@link File#getCanonicalPath()}
     *                                 will automatically do this anyways.
     * @return Returns the {@code canonical path}.
     */
    public static String getCanonicalPath(String path, final String prefixForNonAbsolutePath) {
        if (path == null) path = "";

        String absolutePath;

        // If path is already an absolute path
        if (path.startsWith("/")) {
            absolutePath = path;
        } else {
            if (prefixForNonAbsolutePath != null)
                absolutePath = prefixForNonAbsolutePath + "/" + path;
            else
                absolutePath = "/" + path;
        }

        try {
            return new File(absolutePath).getCanonicalPath();
        } catch (Exception ignored) {
        }

        return absolutePath;
    }

    /**
     * Removes one or more forward slashes "//" with single slash "/"
     * Removes "./"
     * Removes trailing forward slash "/"
     *
     * @param path The {@code path} to convert.
     * @return Returns the {@code normalized path}.
     */
    @Nullable
    public static String normalizePath(String path) {
        if (path == null) return null;

        path = path.replaceAll("/+", "/");
        path = path.replaceAll("\\./", "");

        if (path.endsWith("/")) {
            path = path.replaceAll("/+$", "");
        }

        return path;
    }

    /**
     * Determines whether path is in {@code dirPath}. The {@code dirPath} is not canonicalized and
     * only normalized.
     *
     * @param path        The {@code path} to check.
     * @param dirPath     The {@code directory path} to check in.
     * @param ensureUnder If set to {@code true}, then it will be ensured that {@code path} is
     *                    under the directory and does not equal it.
     * @return Returns {@code true} if path in {@code dirPath}, otherwise returns {@code false}.
     */
    public static boolean isPathInDirPath(String path, final String dirPath, final boolean ensureUnder) {
        return isPathInDirPaths(path, Collections.singletonList(dirPath), ensureUnder);
    }

    /**
     * Determines whether path is in one of the {@code dirPaths}. The {@code dirPaths} are not
     * canonicalized and only normalized.
     *
     * @param path        The {@code path} to check.
     * @param dirPaths    The {@code directory paths} to check in.
     * @param ensureUnder If set to {@code true}, then it will be ensured that {@code path} is
     *                    under the directories and does not equal it.
     * @return Returns {@code true} if path in {@code dirPaths}, otherwise returns {@code false}.
     */
    public static boolean isPathInDirPaths(String path, final List<String> dirPaths, final boolean ensureUnder) {
        if (path == null || path.isEmpty() || dirPaths == null || dirPaths.isEmpty()) return false;

        try {
            path = new File(path).getCanonicalPath();
        } catch (Exception e) {
            return false;
        }

        boolean isPathInDirPaths;

        for (String dirPath : dirPaths) {
            String normalizedDirPath = normalizePath(dirPath);

            if (ensureUnder)
                isPathInDirPaths = !path.equals(normalizedDirPath) && path.startsWith(normalizedDirPath + "/");
            else
                isPathInDirPaths = path.startsWith(normalizedDirPath + "/");

            if (isPathInDirPaths) return true;
        }

        return false;
    }


    /**
     * Get the type of file that exists at {@code filePath}.
     * <p>
     * This function is a wrapper for
     * {@link FileTypes#getFileType(String, boolean)}
     *
     * @param filePath    The {@code path} for file to check.
     * @param followLinks The {@code boolean} that decides if symlinks will be followed while
     *                    finding type. If set to {@code true}, then type of symlink target will
     *                    be returned if file at {@code filePath} is a symlink. If set to
     *                    {@code false}, then type of file at {@code filePath} itself will be
     *                    returned.
     * @return Returns the {@link FileType} of file.
     */
    @NonNull
    public static FileType getFileType(final String filePath, final boolean followLinks) {
        return FileTypes.getFileType(filePath, followLinks);
    }


    /**
     * Validate the existence and permissions of directory file at path.
     * <p>
     * If the {@code parentDirPath} is not {@code null}, then creation of missing directory and
     * setting of missing permissions will only be done if {@code path} is under
     * {@code parentDirPath} or equals {@code parentDirPath}.
     *
     * @param label                               The optional label for the directory file. This can optionally be {@code null}.
     * @param filePath                            The {@code path} for file to validate or create. Symlinks will not be followed.
     * @param parentDirPath                       The optional {@code parent directory path} to restrict operations to.
     *                                            This can optionally be {@code null}. It is not canonicalized and only normalized.
     * @param createDirectoryIfMissing            The {@code boolean} that decides if directory file
     *                                            should be created if its missing.
     * @param permissionsToCheck                  The 3 character string that contains the "r", "w", "x" or "-" in-order.
     * @param setPermissions                      The {@code boolean} that decides if permissions are to be
     *                                            automatically set defined by {@code permissionsToCheck}.
     * @param setMissingPermissionsOnly           The {@code boolean} that decides if only missing permissions
     *                                            are to be set or if they should be overridden.
     * @param ignoreErrorsIfPathIsInParentDirPath The {@code boolean} that decides if existence
     *                                            and permission errors are to be ignored if path is
     *                                            in {@code parentDirPath}.
     * @param ignoreIfNotExecutable               The {@code boolean} that decides if missing executable permission
     *                                            error is to be ignored. This allows making an attempt to set
     *                                            executable permissions, but ignoring if it fails.
     * @return Returns the {@code error} if path is not a directory file, failed to create it,
     * or validating permissions failed, otherwise {@code null}.
     */
    public static Error validateDirectoryFileExistenceAndPermissions(String label, final String filePath, final String parentDirPath, final boolean createDirectoryIfMissing,
                                                                     final String permissionsToCheck, final boolean setPermissions, final boolean setMissingPermissionsOnly,
                                                                     final boolean ignoreErrorsIfPathIsInParentDirPath, final boolean ignoreIfNotExecutable) {
        label = (label == null || label.isEmpty() ? "" : label + " ");
        if (filePath == null || filePath.isEmpty())
            return FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getError(label + "directory file path", "validateDirectoryExistenceAndPermissions");

        try {
            File file = new File(filePath);
            FileType fileType = getFileType(filePath, false);

            // If file exists but not a directory file
            if (fileType != FileType.NO_EXIST && fileType != FileType.DIRECTORY) {
                return FileUtilsErrno.ERRNO_NON_DIRECTORY_FILE_FOUND.getError(label + "directory", filePath).setLabel(label + "directory");
            }

            boolean isPathInParentDirPath = false;
            if (parentDirPath != null) {
                // The path can be equal to parent directory path or under it
                isPathInParentDirPath = isPathInDirPath(filePath, parentDirPath, false);
            }

            if (createDirectoryIfMissing || setPermissions) {
                // If there is not parentDirPath restriction or path is in parentDirPath
                if (parentDirPath == null || (isPathInParentDirPath && getFileType(parentDirPath, false) == FileType.DIRECTORY)) {
                    // If createDirectoryIfMissing is enabled and no file exists at path, then create directory
                    if (createDirectoryIfMissing && fileType == FileType.NO_EXIST) {
                        //        logMessage(Log.VERBOSE, tag, message);
                        // Create directory and update fileType if successful, otherwise return with error
                        // It "might" be possible that mkdirs returns false even though directory was created
                        boolean result = file.mkdirs();
                        fileType = getFileType(filePath, false);
                        if (!result && fileType != FileType.DIRECTORY)
                            return FileUtilsErrno.ERRNO_CREATING_FILE_FAILED.getError(label + "directory file", filePath);
                    }

                    // If setPermissions is enabled and path is a directory
                    if (setPermissions && permissionsToCheck != null && fileType == FileType.DIRECTORY) {
                        if (setMissingPermissionsOnly)
                            setMissingFilePermissions(label + "directory", filePath, permissionsToCheck);
                        else
                            setFilePermissions(label + "directory", filePath, permissionsToCheck);
                    }
                }
            }

            // If there is not parentDirPath restriction or path is not in parentDirPath or
            // if existence or permission errors must not be ignored for paths in parentDirPath
            if (parentDirPath == null || !isPathInParentDirPath || !ignoreErrorsIfPathIsInParentDirPath) {
                // If path is not a directory
                // Directories can be automatically created so we can ignore if missing with above check
                if (fileType != FileType.DIRECTORY) {
                    label += "directory";
                    return FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH.getError(label, filePath).setLabel(label);
                }

                if (permissionsToCheck != null) {
                    // Check if permissions are missing
                    return checkMissingFilePermissions(label + "directory", filePath, permissionsToCheck, ignoreIfNotExecutable);
                }
            }
        } catch (Exception e) {
            return FileUtilsErrno.ERRNO_VALIDATE_DIRECTORY_EXISTENCE_AND_PERMISSIONS_FAILED_WITH_EXCEPTION.getError(e, label + "directory file", filePath, e.getMessage());
        }

        return null;
    }


    /**
     * Delete socket file at path.
     * <p>
     * This function is a wrapper for {@link #deleteFile(String, String, boolean, boolean, int)}.
     *
     * @param label                 The optional label for file to delete. This can optionally be {@code null}.
     * @param filePath              The {@code path} for file to delete.
     * @param ignoreNonExistentFile The {@code boolean} that decides if it should be considered an
     *                              error if file to deleted doesn't exist.
     * @return Returns the {@code error} if deletion was not successful, otherwise {@code null}.
     */
    public static Error deleteSocketFile(String label, final String filePath, final boolean ignoreNonExistentFile) {
        return deleteFile(label, filePath, ignoreNonExistentFile, false, FileType.SOCKET.getValue());
    }

    /**
     * Delete file at path.
     * <p>
     * The {@code filePath} must be the canonical path to the file to be deleted since symlinks will
     * not be followed.
     * If the {@code filePath} is a canonical path to a directory, then any symlink files found under
     * the directory will be deleted, but not their targets.
     *
     * @param label                 The optional label for file to delete. This can optionally be {@code null}.
     * @param filePath              The {@code path} for file to delete.
     * @param ignoreNonExistentFile The {@code boolean} that decides if it should be considered an
     *                              error if file to deleted doesn't exist.
     * @param ignoreWrongFileType   The {@code boolean} that decides if it should be considered an
     *                              error if file type is not one from {@code allowedFileTypeFlags}.
     * @param allowedFileTypeFlags  The flags that are matched against the file's {@link FileType} to
     *                              see if it should be deleted or not. This is a safety measure to
     *                              prevent accidental deletion of the wrong type of file, like a
     *                              directory instead of a regular file. You can pass
     *                              {@link FileTypes#FILE_TYPE_ANY_FLAGS} to allow deletion of any file type.
     * @return Returns the {@code error} if deletion was not successful, otherwise {@code null}.
     */
    public static Error deleteFile(String label, final String filePath, final boolean ignoreNonExistentFile, final boolean ignoreWrongFileType, int allowedFileTypeFlags) {
        label = (label == null || label.isEmpty() ? "" : label + " ");
        if (filePath == null || filePath.isEmpty())
            return FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getError(label + "file path", "deleteFile");

        try {
            File file = new File(filePath);
            FileType fileType = getFileType(filePath, false);

            //        logMessage(Log.VERBOSE, tag, message);

            // If file does not exist
            if (fileType == FileType.NO_EXIST) {
                // If delete is to be ignored if file does not exist
                if (ignoreNonExistentFile)
                    return null;
                    // Else return with error
                else {
                    label += "file meant to be deleted";
                    return FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH.getError(label, filePath).setLabel(label);
                }
            }

            // If the file type of the file does not exist in the allowedFileTypeFlags
            if ((allowedFileTypeFlags & fileType.getValue()) <= 0) {
                // If wrong file type is to be ignored
                if (ignoreWrongFileType) {
                    FileTypes.convertFileTypeFlagsToNamesString(allowedFileTypeFlags);
//        logMessage(Log.VERBOSE, tag, message);
                    return null;
                }

                // Else return with error
                return FileUtilsErrno.ERRNO_FILE_NOT_AN_ALLOWED_FILE_TYPE.getError(label + "file meant to be deleted", filePath, fileType.getName(), FileTypes.convertFileTypeFlagsToNamesString(allowedFileTypeFlags));
            }

            //        logMessage(Log.VERBOSE, tag, message);

            /*
             * Try to use {@link SecureDirectoryStream} if available for safer directory
             * deletion, it should be available for android >= 8.0
             * https://guava.dev/releases/24.1-jre/api/docs/com/google/common/io/MoreFiles.html#deleteRecursively-java.nio.file.Path-com.google.common.io.RecursiveDeleteOption...-
             * https://github.com/google/guava/issues/365
             * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r3:libcore/ojluni/src/main/java/sun/nio/fs/UnixSecureDirectoryStream.java
             *
             * MoreUtils is marked with the @Beta annotation so the API may be removed in
             * future but has been there for a few years now.
             *
             * If an exception is thrown, the exception message might not contain the full errors.
             * Individual failures get added to suppressed throwables which can be extracted
             * from the exception object by calling `Throwable[] getSuppressed()`. So just logging
             * the exception message and stacktrace may not be enough, the suppressed throwables
             * need to be logged as well, which the Logger class does if they are found in the
             * exception added to the Error that's returned by this function.
             * https://github.com/google/guava/blob/v30.1.1/guava/src/com/google/common/io/MoreFiles.java#L775
             */
            com.google.common.io.MoreFiles.deleteRecursively(file.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);

            // If file still exists after deleting it
            fileType = getFileType(filePath, false);
            if (fileType != FileType.NO_EXIST)
                return FileUtilsErrno.ERRNO_FILE_STILL_EXISTS_AFTER_DELETING.getError(label + "file meant to be deleted", filePath);
        } catch (Exception e) {
            return FileUtilsErrno.ERRNO_DELETING_FILE_FAILED_WITH_EXCEPTION.getError(e, label + "file", filePath, e.getMessage());
        }

        return null;
    }


    /**
     * Set permissions for file at path. Existing permission outside the {@code permissionsToSet}
     * will be removed.
     *
     * @param label            The optional label for the file. This can optionally be {@code null}.
     * @param filePath         The {@code path} for file to set permissions to.
     * @param permissionsToSet The 3 character string that contains the "r", "w", "x" or "-" in-order.
     */
    public static void setFilePermissions(String label, final String filePath, final String permissionsToSet) {
//        label = (label == null || label.isEmpty() ? "" : label + " ");
        if (filePath == null || filePath.isEmpty()) return;

        if (!isValidPermissionString(permissionsToSet)) {
            //        logMessage(Log.ERROR, tag, message);
            return;
        }

        File file = new File(filePath);

        if (permissionsToSet.contains("r")) {
            if (!file.canRead()) {
                //        logMessage(Log.VERBOSE, tag, message);
                file.setReadable(true);
            }
        } else {
            if (file.canRead()) {
                //        logMessage(Log.VERBOSE, tag, message);
                file.setReadable(false);
            }
        }


        if (permissionsToSet.contains("w")) {
            if (!file.canWrite()) {
                //        logMessage(Log.VERBOSE, tag, message);
                file.setWritable(true);
            }
        } else {
            if (file.canWrite()) {
                //        logMessage(Log.VERBOSE, tag, message);
                file.setWritable(false);
            }
        }


        if (permissionsToSet.contains("x")) {
            if (!file.canExecute()) {
                //        logMessage(Log.VERBOSE, tag, message);
                file.setExecutable(true);
            }
        } else {
            if (file.canExecute()) {
                //        logMessage(Log.VERBOSE, tag, message);
                file.setExecutable(false);
            }
        }
    }


    /**
     * Set missing permissions for file at path. Existing permission outside the {@code permissionsToSet}
     * will not be removed.
     *
     * @param label            The optional label for the file. This can optionally be {@code null}.
     * @param filePath         The {@code path} for file to set permissions to.
     * @param permissionsToSet The 3 character string that contains the "r", "w", "x" or "-" in-order.
     */
    public static void setMissingFilePermissions(String label, final String filePath, final String permissionsToSet) {
//        label = (label == null || label.isEmpty() ? "" : label + " ");
        if (filePath == null || filePath.isEmpty()) return;

        if (!isValidPermissionString(permissionsToSet)) {
            //        logMessage(Log.ERROR, tag, message);
            return;
        }

        File file = new File(filePath);

        if (permissionsToSet.contains("r") && !file.canRead()) {
            //        logMessage(Log.VERBOSE, tag, message);
            file.setReadable(true);
        }

        if (permissionsToSet.contains("w") && !file.canWrite()) {
            //        logMessage(Log.VERBOSE, tag, message);
            file.setWritable(true);
        }

        if (permissionsToSet.contains("x") && !file.canExecute()) {
            //        logMessage(Log.VERBOSE, tag, message);
            file.setExecutable(true);
        }
    }


    /**
     * Checking missing permissions for file at path.
     *
     * @param label                 The optional label for the file. This can optionally be {@code null}.
     * @param filePath              The {@code path} for file to check permissions for.
     * @param permissionsToCheck    The 3 character string that contains the "r", "w", "x" or "-" in-order.
     * @param ignoreIfNotExecutable The {@code boolean} that decides if missing executable permission
     *                              error is to be ignored.
     * @return Returns the {@code error} if validating permissions failed, otherwise {@code null}.
     */
    public static Error checkMissingFilePermissions(String label, final String filePath, final String permissionsToCheck, final boolean ignoreIfNotExecutable) {
        label = (label == null || label.isEmpty() ? "" : label + " ");
        if (filePath == null || filePath.isEmpty())
            return FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getError(label + "file path", "checkMissingFilePermissions");

        if (!isValidPermissionString(permissionsToCheck)) {
            //        logMessage(Log.ERROR, tag, message);
            return FileUtilsErrno.ERRNO_INVALID_FILE_PERMISSIONS_STRING_TO_CHECK.getError();
        }

        File file = new File(filePath);

        // If file is not readable
        if (permissionsToCheck.contains("r") && !file.canRead()) {
            return FileUtilsErrno.ERRNO_FILE_NOT_READABLE.getError(label + "file", filePath).setLabel(label + "file");
        }

        // If file is not writable
        if (permissionsToCheck.contains("w") && !file.canWrite()) {
            return FileUtilsErrno.ERRNO_FILE_NOT_WRITABLE.getError(label + "file", filePath).setLabel(label + "file");
        }
        // If file is not executable
        // This canExecute() will give "avc: granted { execute }" warnings for target sdk 29
        else if (permissionsToCheck.contains("x") && !file.canExecute() && !ignoreIfNotExecutable) {
            return FileUtilsErrno.ERRNO_FILE_NOT_EXECUTABLE.getError(label + "file", filePath).setLabel(label + "file");
        }

        return null;
    }


    /**
     * Checks whether string exactly matches the 3 character permission string that
     * contains the "r", "w", "x" or "-" in-order.
     *
     * @param string The {@link String} to check.
     * @return Returns {@code true} if string exactly matches a permission string, otherwise {@code false}.
     */
    public static boolean isValidPermissionString(final String string) {
        if (string == null || string.isEmpty()) return false;
        return Pattern.compile("^([r-])[w-][x-]$", 0).matcher(string).matches();
    }


    /**
     * Get file basename for file at {@code filePath}.
     *
     * @param filePath The {@code path} for file.
     * @return Returns the file basename if not {@code null}.
     */
    public static String getFileBasename(String filePath) {
        if (DataUtils.isNullOrEmpty(filePath)) return null;
        int lastSlash = filePath.lastIndexOf('/');
        return (lastSlash == -1) ? filePath : filePath.substring(lastSlash + 1);
    }

}
