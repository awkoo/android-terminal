package awkoo.terminal.terminal;

import java.nio.charset.StandardCharsets;

/**
 * 一个客户端，它接收由向 {@link TerminalEmulator} 提供输入而触发的事件的回调。
 */
public abstract class TerminalOutput {

    /**
     * 使用 UTF-8 编码向终端客户端写入字符串。
     */
    public final void write(String data) {
        if (data == null) return;
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        write(bytes, 0, bytes.length);
    }

    /**
     * 将字节写入终端客户端。
     */
    public abstract void write(byte[] data, int offset, int count);

    /**
     * 通知终端客户端终端标题已更改。
     */
    public abstract void titleChanged(String oldTitle, String newTitle);

    /**
     * 通知终端客户端应将文本复制到剪贴板。
     */
    public abstract void onCopyTextToClipboard(String text);

    /**
     * 通知终端客户端应从剪贴板粘贴文本。
     */
    public abstract void onPasteTextFromClipboard();

    public abstract void onColorsChanged();

}
