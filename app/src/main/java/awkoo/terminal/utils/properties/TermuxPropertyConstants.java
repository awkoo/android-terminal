package awkoo.terminal.utils.properties;


public final class TermuxPropertyConstants {
//    /**
//     * 定义了创建会话快捷方式的键。
//     */
//    public static final String KEY_SHORTCUT_CREATE_SESSION = "shortcut.create-session"; // 默认值: "shortcut.create-session"
//    /**
//     * 定义了下一个会话快捷方式的键。
//     */
//    public static final String KEY_SHORTCUT_NEXT_SESSION = "shortcut.next-session"; // 默认值: "shortcut.next-session"
//    /**
//     * 定义了上一个会话快捷方式的键。
//     */
//    public static final String KEY_SHORTCUT_PREVIOUS_SESSION = "shortcut.previous-session"; // 默认值: "shortcut.previous-session"
//    /**
//     * 定义了重命名会话快捷方式的键。
//     */
//    public static final String KEY_SHORTCUT_RENAME_SESSION = "shortcut.rename-session"; // 默认值: "shortcut.rename-session"
//
//    public static final int ACTION_SHORTCUT_CREATE_SESSION = 1;
//    public static final int ACTION_SHORTCUT_NEXT_SESSION = 2;
//    public static final int ACTION_SHORTCUT_PREVIOUS_SESSION = 3;
//    public static final int ACTION_SHORTCUT_RENAME_SESSION = 4;

//    /**
//     * 定义了会话快捷方式值及其内部操作的双向映射。
//     */
//    public static final ImmutableBiMap<String, Integer> MAP_SESSION_SHORTCUTS =
//        new ImmutableBiMap.Builder<String, Integer>()
//            .put(KEY_SHORTCUT_CREATE_SESSION, ACTION_SHORTCUT_CREATE_SESSION)
//            .put(KEY_SHORTCUT_NEXT_SESSION, ACTION_SHORTCUT_NEXT_SESSION)
//            .put(KEY_SHORTCUT_PREVIOUS_SESSION, ACTION_SHORTCUT_PREVIOUS_SESSION)
//            .put(KEY_SHORTCUT_RENAME_SESSION, ACTION_SHORTCUT_RENAME_SESSION)
//            .build();





    /* String */

    /**
     * 定义了返回键是作为转义键还是字面返回键的键。
     */
    public static final String KEY_BACK_KEY_BEHAVIOUR = "back-key"; // 默认值: "back-key"

    public static final String IVALUE_BACK_KEY_BEHAVIOUR_BACK = "back";
    public static final String IVALUE_BACK_KEY_BEHAVIOUR_ESCAPE = "escape";
    public static final String DEFAULT_IVALUE_BACK_KEY_BEHAVIOUR = IVALUE_BACK_KEY_BEHAVIOUR_BACK;


    /**
     * 定义了额外按键的键。
     */
    public static final String KEY_EXTRA_KEYS = "extra-keys"; // 默认值: "extra-keys"
    //public static final String DEFAULT_IVALUE_EXTRA_KEYS = "[[ESC, TAB, CTRL, ALT, {key: '-', popup: '|'}, DOWN, UP]]"; // Single row
    public static final String DEFAULT_IVALUE_EXTRA_KEYS = "[['ESC',{key: 'DRAWER', popup: 'PASTE'},'SCROLL','HOME','UP','END','PGUP'], ['TAB','CTRL','ALT','LEFT','DOWN','RIGHT','PGDN']]"; // 双行

    /**
     * 定义了额外按键样式的键。
     */
    public static final String KEY_EXTRA_KEYS_STYLE = "extra-keys-style"; // 默认值: "extra-keys-style"
    public static final String DEFAULT_IVALUE_EXTRA_KEYS_STYLE = "default";


    /**
     * 定义了软键盘切换请求是显示/隐藏还是启用/禁用键盘的键。
     */
    public static final String KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR = "soft-keyboard-toggle-behaviour"; // 默认值: "soft-keyboard-toggle-behaviour"

    public static final String IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_SHOW_HIDE = "show/hide";
    public static final String IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_ENABLE_DISABLE = "enable/disable";
    public static final String DEFAULT_IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR = IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_SHOW_HIDE;


    /**
     * 定义了音量键是作为虚拟键还是字面音量键的键。
     */
    public static final String KEY_VOLUME_KEYS_BEHAVIOUR = "volume-keys"; // 默认值: "volume-keys"

    public static final String IVALUE_VOLUME_KEY_BEHAVIOUR_VIRTUAL = "virtual";
    public static final String IVALUE_VOLUME_KEY_BEHAVIOUR_VOLUME = "volume";
    public static final String DEFAULT_IVALUE_VOLUME_KEYS_BEHAVIOUR = IVALUE_VOLUME_KEY_BEHAVIOUR_VIRTUAL;


}
