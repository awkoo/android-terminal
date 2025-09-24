package awkoo.terminal.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShellEnvironment extends HashMap<String, String> {

    public static final String defaultWorkingPath = "/";
    public static final String defaultShell = "/system/bin/sh";

    public ShellEnvironment() {
        super();
        this.putAll(System.getenv());
        this.put("COLORTERM", "truecolor");
        this.put("TERM", "xterm-256color");
    }

    public String[] toArray() {
        List<String> environmentList = new ArrayList<>(this.size());
        for (String name : this.keySet())
            environmentList.add(name + "=" + this.get(name));
        return environmentList.toArray(new String[0]);
    }

}
