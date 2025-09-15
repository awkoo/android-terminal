package awkoo.terminal.shared.shell.command.environment;

import java.util.HashMap;

public class ShellEnvironment extends HashMap<String, String> {

    public static final String defaultWorkingPath = "/";
    public static final String defaultShell = "/system/bin/sh";

    public ShellEnvironment() {
        super();
        this.putAll(System.getenv());
        this.put("COLORTERM", "truecolor");
        this.put("TERM", "xterm-256color");
    }

}
