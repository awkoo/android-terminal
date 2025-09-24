package awkoo.terminal.utils.properties;

import awkoo.terminal.Constants;

public class TermuxAppSharedProperties extends TermuxSharedProperties {

    private static TermuxAppSharedProperties properties;


    private TermuxAppSharedProperties() {
        super(
            Constants.TERMUX_APP_NAME,
            TermuxPropertyConstants.TERMUX_APP_PROPERTIES_LIST,
            new TermuxSharedProperties.SharedPropertiesParserClient()
        );
    }

    /**
     * Initialize the {@link #properties} and load properties from disk.
     */
    public static void init() {
        if (properties == null)
            properties = new TermuxAppSharedProperties();
    }

    /**
     * Get the {@link #properties}.
     *
     * @return Returns the {@link TermuxAppSharedProperties}.
     */
    public static TermuxAppSharedProperties getProperties() {
        return properties;
    }

}
