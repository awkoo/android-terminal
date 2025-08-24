package com.termux.shared.termux;

import android.annotation.SuppressLint;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public final class TermuxConstants {

    /*
     * Termux and its plugin app and package names and urls.
     */

    /** Termux app name */
    public static final String TERMUX_APP_NAME = "Termux"; // Default: "Termux"
    /** Termux package name */
    public static final String TERMUX_PACKAGE_NAME = "com.termux"; // Default: "com.termux"




    /*
     * Termux app core directory paths.
     */

    /** Termux app internal private app data directory path */
    @SuppressLint("SdCardPath")
    public static final String TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH = "/data/data/" + TERMUX_PACKAGE_NAME; // Default: "/data/data/com.termux"


    /** Termux app Files directory path */
    public static final String TERMUX_FILES_DIR_PATH = TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files"; // Default: "/data/data/com.termux/files"


//    /** Termux app $PREFIX directory path */
//    public static final String TERMUX_PREFIX_DIR_PATH = TERMUX_FILES_DIR_PATH + "/usr"; // Default: "/data/data/com.termux/files/usr"


//    /** Termux app $PREFIX/tmp and $TMPDIR directory path */
//    public static final String TERMUX_TMP_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/tmp"; // Default: "/data/data/com.termux/files/usr/tmp"


    /** Termux app $HOME directory path */
    public static final String TERMUX_HOME_DIR_PATH = TERMUX_FILES_DIR_PATH + "/home"; // Default: "/data/data/com.termux/files/home"


    /** Termux app config home directory path */
    public static final String TERMUX_CONFIG_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.config/termux"; // Default: "/data/data/com.termux/files/home/.config/termux"


    /** Termux app data home directory path */
    public static final String TERMUX_DATA_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.termux"; // Default: "/data/data/com.termux/files/home/.termux"




    /*
     * Termux app and plugin preferences and properties file paths.
     */

    /** Termux app default SharedPreferences file basename without extension */
    public static final String TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_PACKAGE_NAME + "_preferences"; // Default: "com.termux_preferences"


    /** Termux app properties primary file path */
    public static final String TERMUX_PROPERTIES_PRIMARY_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/termux.properties"; // Default: "/data/data/com.termux/files/home/.termux/termux.properties"

    /** Termux app properties secondary file path */
    public static final String TERMUX_PROPERTIES_SECONDARY_FILE_PATH = TERMUX_CONFIG_HOME_DIR_PATH + "/termux.properties"; // Default: "/data/data/com.termux/files/home/.config/termux/termux.properties"

    /** Termux app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent. */
    public static final List<String> TERMUX_PROPERTIES_FILE_PATHS_LIST = Arrays.asList(
        TERMUX_PROPERTIES_PRIMARY_FILE_PATH,
        TERMUX_PROPERTIES_SECONDARY_FILE_PATH);


//    /** Termux app and Termux:Styling colors.properties file path */
//    public static final String TERMUX_COLOR_PROPERTIES_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/colors.properties"; // Default: "/data/data/com.termux/files/home/.termux/colors.properties"
//    /** Termux app and Termux:Styling colors.properties file */
//    public static final File TERMUX_COLOR_PROPERTIES_FILE = new File(TERMUX_COLOR_PROPERTIES_FILE_PATH);

//    /** Termux app and Termux:Styling font.ttf file path */
//    public static final String TERMUX_FONT_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/font.ttf"; // Default: "/data/data/com.termux/files/home/.termux/font.ttf"
//    /** Termux app and Termux:Styling font.ttf file */
//    public static final File TERMUX_FONT_FILE = new File(TERMUX_FONT_FILE_PATH);





    /*
     * Termux app and plugins notification variables.
     */

    /** Termux app notification channel id used by {@link TERMUX_APP.TERMUX_SERVICE} */
    public static final String TERMUX_APP_NOTIFICATION_CHANNEL_ID = "termux_notification_channel";
    /** Termux app notification channel name used by {@link TERMUX_APP.TERMUX_SERVICE} */
    public static final String TERMUX_APP_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " App";
    /** Termux app unique notification id used by {@link TERMUX_APP.TERMUX_SERVICE} */
    public static final int TERMUX_APP_NOTIFICATION_ID = 1337;
//    /** Termux app unique notification id used by {@link TERMUX_APP.RUN_COMMAND_SERVICE} */
//    public static final int TERMUX_RUN_COMMAND_NOTIFICATION_ID = 1338;





    /*
     * Termux app and plugins miscellaneous variables.
     */

    /** Termux property defined in termux.properties file as a secondary check to PERMISSION_RUN_COMMAND
     * to allow 3rd party apps to run various commands in Termux app context */
    public static final String PROP_ALLOW_EXTERNAL_APPS = "allow-external-apps"; // Default: "allow-external-apps"

    /** The broadcast action sent when Termux App opens */
    public static final String BROADCAST_TERMUX_OPENED = TERMUX_PACKAGE_NAME + ".app.OPENED";




    /**
     * Termux app constants.
     */
    public static final class TERMUX_APP {

        /**
         * Termux app core activity.
         */
        public static final class TERMUX_ACTIVITY {

            /** Intent extra for if termux failsafe session needs to be started and is used by {@link TERMUX_ACTIVITY} and {@link TERMUX_SERVICE#ACTION_STOP_SERVICE} */
            public static final String EXTRA_FAILSAFE_SESSION = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.failsafe_session"; // Default: "com.termux.app.failsafe_session"


//            /** Intent action to make termux reload its termux session styling */
//            public static final String ACTION_RELOAD_STYLE = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.reload_style"; // Default: "com.termux.app.reload_style"
        }


        /**
         * Termux app core service.
         */
        public static final class TERMUX_SERVICE {

            /** Intent action to stop TERMUX_SERVICE */
            public static final String ACTION_STOP_SERVICE = TERMUX_PACKAGE_NAME + ".service_stop"; // Default: "com.termux.service_stop"


            /** Intent action to make TERMUX_SERVICE acquire a wakelock */
            public static final String ACTION_WAKE_LOCK = TERMUX_PACKAGE_NAME + ".service_wake_lock"; // Default: "com.termux.service_wake_lock"


            /** Intent action to make TERMUX_SERVICE release wakelock */
            public static final String ACTION_WAKE_UNLOCK = TERMUX_PACKAGE_NAME + ".service_wake_unlock"; // Default: "com.termux.service_wake_unlock"


        }


//        /**
//         * Termux app run command service to receive commands sent by 3rd party apps.
//         */
//        public static final class RUN_COMMAND_SERVICE {
//
//        }
    }


}
