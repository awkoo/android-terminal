package awkoo.terminal.shared.termux;

import android.annotation.SuppressLint;

import java.util.Arrays;
import java.util.List;

public final class TermuxConstants {

    /*
     * Termux and its plugin app and package names and urls.
     */

    /**
     * Termux app name
     */
    public static final String TERMUX_APP_NAME = "Termux"; // Default: "Termux"
    /**
     * Termux package name
     */
    public static final String TERMUX_PACKAGE_NAME = "awkoo.terminal"; // Default: "awkoo.terminal"




    /*
     * Termux app core directory paths.
     */

    /**
     * Termux app internal private app data directory path
     */
    @SuppressLint("SdCardPath")
    public static final String TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH = "/data/data/" + TERMUX_PACKAGE_NAME; // Default: "/data/data/awkoo.terminal"


    /**
     * Termux app Files directory path
     */
    public static final String TERMUX_FILES_DIR_PATH = TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files"; // Default: "/data/data/awkoo.terminal/files"


    /**
     * Termux app $HOME directory path
     */
    public static final String TERMUX_HOME_DIR_PATH = TERMUX_FILES_DIR_PATH + "/home"; // Default: "/data/data/awkoo.terminal/files/home"


    /**
     * Termux app config home directory path
     */
    public static final String TERMUX_CONFIG_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.config/termux"; // Default: "/data/data/awkoo.terminal/files/home/.config/termux"


    /**
     * Termux app data home directory path
     */
    public static final String TERMUX_DATA_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.termux"; // Default: "/data/data/awkoo.terminal/files/home/.termux"




    /*
     * Termux app and plugin preferences and properties file paths.
     */


    /**
     * Termux app properties primary file path
     */
    public static final String TERMUX_PROPERTIES_PRIMARY_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/termux.properties"; // Default: "/data/data/awkoo.terminal/files/home/.termux/termux.properties"

    /**
     * Termux app properties secondary file path
     */
    public static final String TERMUX_PROPERTIES_SECONDARY_FILE_PATH = TERMUX_CONFIG_HOME_DIR_PATH + "/termux.properties"; // Default: "/data/data/awkoo.terminal/files/home/.config/termux/termux.properties"

    /**
     * Termux app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent.
     */
    public static final List<String> TERMUX_PROPERTIES_FILE_PATHS_LIST = Arrays.asList(
        TERMUX_PROPERTIES_PRIMARY_FILE_PATH,
        TERMUX_PROPERTIES_SECONDARY_FILE_PATH);





    /*
     * Termux app and plugins notification variables.
     */

    /**
     * Termux app notification channel id used by {@link TERMUX_APP.TERMUX_SERVICE}
     */
    public static final String TERMUX_APP_NOTIFICATION_CHANNEL_ID = "termux_notification_channel";
    /**
     * Termux app notification channel name used by {@link TERMUX_APP.TERMUX_SERVICE}
     */
    public static final String TERMUX_APP_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " App";
    /**
     * Termux app unique notification id used by {@link TERMUX_APP.TERMUX_SERVICE}
     */
    public static final int TERMUX_APP_NOTIFICATION_ID = 1;
//    /** Termux app unique notification id used by {@link TERMUX_APP.RUN_COMMAND_SERVICE} */
//    public static final int TERMUX_RUN_COMMAND_NOTIFICATION_ID = 1338;





    /*
     * Termux app and plugins miscellaneous variables.
     */

    /**
     * Termux property defined in termux.properties file as a secondary check to PERMISSION_RUN_COMMAND
     * to allow 3rd party apps to run various commands in Termux app context
     */
    public static final String PROP_ALLOW_EXTERNAL_APPS = "allow-external-apps"; // Default: "allow-external-apps"


    /**
     * Termux app constants.
     */
    public static final class TERMUX_APP {


        /**
         * Termux app core service.
         */
        public static final class TERMUX_SERVICE {

            /**
             * Intent action to stop TERMUX_SERVICE
             */
            public static final String ACTION_STOP_SERVICE = TERMUX_PACKAGE_NAME + ".service_stop"; // Default: "awkoo.terminal.service_stop"


            /**
             * Intent action to make TERMUX_SERVICE acquire a wakelock
             */
            public static final String ACTION_WAKE_LOCK = TERMUX_PACKAGE_NAME + ".service_wake_lock"; // Default: "awkoo.terminal.service_wake_lock"


            /**
             * Intent action to make TERMUX_SERVICE release wakelock
             */
            public static final String ACTION_WAKE_UNLOCK = TERMUX_PACKAGE_NAME + ".service_wake_unlock"; // Default: "awkoo.terminal.service_wake_unlock"


        }
    }


}
