package awkoo.terminal;

public final class Constants {

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
     * Termux app and plugins notification variables.
     */

    /**
     * Termux app notification channel id used by {@link TERMUX_APP.TERMUX_SERVICE}
     */
    public static final String TERMUX_APP_NOTIFICATION_CHANNEL_ID = "termux_notification_channel";
    /**
     * Termux app notification channel name used by {@link TERMUX_APP.TERMUX_SERVICE}
     */
    public static final String TERMUX_APP_NOTIFICATION_CHANNEL_NAME = Constants.TERMUX_APP_NAME + " App";
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



    /**
     * 终端光标闪烁速率。
     */
    public static final int DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE = 0;
    /**
     * 终端工具栏高度
     */
    public static final float DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR = 1;

}
