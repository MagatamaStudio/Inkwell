package com.kujoushiori.inkwell;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;

/** 书页里能触发的几个动作，键盘 / 按钮 / 鼠标都走这里。 */
public final class Actions {
    private Actions() {
    }

    /** 把剪贴板写进书里。force=true 时不看长短，一定接管。 */
    public static boolean importClipboard(BookEditScreen screen, boolean force) {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip == null || clip.isBlank()) {
            return false;
        }
        InkwellConfig cfg = InkwellConfig.get();
        if (!force && cfg.longClipboardOnly && clip.length() <= cfg.charsPerPage) {
            return false; // 短文本交给原版按光标粘贴
        }
        List<String> pages = Inkwell.paginate(clip);
        return pages != null && BookWriter.write(screen, pages);
    }

    /** 导入根目录下的某个文件（默认 book.txt）。 */
    public static boolean importFile(BookEditScreen screen, String name) {
        String text = Inkwell.readFile(name);
        if (text == null) {
            return false;
        }
        List<String> pages = Inkwell.paginate(text);
        return pages != null && BookWriter.write(screen, pages);
    }
}
