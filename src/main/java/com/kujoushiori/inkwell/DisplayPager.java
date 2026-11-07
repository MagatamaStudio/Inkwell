package com.kujoushiori.inkwell;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 按“实际显示”分页：用游戏字体量一遍，保证一页真的看得全。
 *
 * <p>原版阅读界面（BookViewScreen）的文本区是 <b>114 × 128 像素</b>，
 * 编辑界面是 114 × 126；行高 = font.lineHeight（默认 9），所以一页只有
 * <b>14 行</b>。中文一个字约 9px 宽，一行大约 12 个字，一页约 170 字；
 * 英文一行约 19 字，一页约 270 字 —— 远小于编辑框那个 1024 字的上限。</p>
 */
public final class DisplayPager {
    /** BookViewScreen.TEXT_WIDTH */
    public static final int TEXT_WIDTH = 114;
    /** 阅读界面文本区高度 */
    public static final int TEXT_HEIGHT = 128;

    private DisplayPager() {
    }

    public static List<String> paginate(String raw, Font font, int widthPx, int maxLines,
                                        int maxCharsPerPage, int maxPages, String breakLine) {
        String text = raw == null ? "" : raw.replace("\r\n", "\n").replace('\r', '\n');
        List<String> pages = new ArrayList<>();
        if (text.isBlank()) {
            return pages;
        }
        int lines = Math.max(1, maxLines);
        int chars = Math.max(1, maxCharsPerPage);

        StringBuilder cur = new StringBuilder();
        int usedLines = 0;

        for (String line : text.split("\n", -1)) {
            if (breakLine != null && !breakLine.isBlank() && line.trim().equals(breakLine)) {
                flush(pages, cur);
                usedLines = 0;
                continue;
            }
            int wrapped = wrapCount(font, line, widthPx);
            boolean tooManyLines = usedLines + wrapped > lines && cur.length() > 0;
            boolean tooManyChars = usedLines > 0 && cur.length() + 1 + line.length() > chars;
            if (tooManyLines || tooManyChars) {
                flush(pages, cur);
                usedLines = 0;
            }
            if (cur.length() > 0) {
                cur.append('\n');
            }
            cur.append(line);
            usedLines += wrapped;

            if (usedLines >= lines) {
                flush(pages, cur);
                usedLines = 0;
            }
        }
        flush(pages, cur);

        if (pages.size() > maxPages) {
            return new ArrayList<>(pages.subList(0, maxPages));
        }
        return pages;
    }

    /** 这一段在给定宽度下会折成几行。 */
    public static int wrapCount(Font font, String line, int widthPx) {
        if (line.isEmpty()) {
            return 1;
        }
        try {
            List<FormattedCharSequence> seq = font.split(Component.literal(line), Math.max(1, widthPx));
            return Math.max(1, seq.size());
        } catch (Throwable t) {
            // 字体没准备好就按字符数粗估
            return Math.max(1, (line.length() * 9) / Math.max(1, widthPx));
        }
    }

    private static void flush(List<String> pages, StringBuilder cur) {
        String s = cur.toString().strip();
        if (!s.isEmpty()) {
            pages.add(s);
        }
        cur.setLength(0);
    }
}
