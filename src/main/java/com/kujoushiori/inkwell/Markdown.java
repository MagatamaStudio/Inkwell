package com.kujoushiori.inkwell;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Markdown → 纯文本。书页里显示不了任何 md 语法，所以直接清掉，只留内容。
 *
 * <p>保留：正文、换行、单独一行的 {@code ---}（当作强制翻页）。</p>
 * <p>去掉：标题井号、列表符号（换成 · ）、引用符号、表格竖线、代码围栏、
 * 粗体/斜体/删除线/行内代码标记、链接语法（只留文字）。</p>
 */
public final class Markdown {
    private static final Pattern HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s+");
    private static final Pattern BULLET = Pattern.compile("^\\s{0,3}[-*+]\\s+");
    private static final Pattern QUOTE = Pattern.compile("^\\s{0,3}>+\\s?");
    private static final Pattern HR = Pattern.compile("^\\s{0,3}(\\*\\s*){3,}$|^\\s{0,3}(_\\s*){3,}$");
    private static final Pattern FENCE = Pattern.compile("^\\s*(```|~~~).*$");
    private static final Pattern IMAGE = Pattern.compile("!\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\([^)]*\\)");
    private static final Pattern AUTOLINK = Pattern.compile("<((?:https?|ftp)://[^>]+)>");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD = Pattern.compile("(\\*\\*|__)(.+?)\\1");
    private static final Pattern ITALIC = Pattern.compile("(?<![\\w*])([*_])(?!\\s)(.+?)(?<!\\s)\\1(?![\\w*])");
    private static final Pattern STRIKE = Pattern.compile("~~(.+?)~~");
    private static final Pattern BLANK_RUN = Pattern.compile("\\n{3,}");

    private Markdown() {
    }

    public static String toPlain(String src) {
        if (src == null || src.isEmpty()) {
            return "";
        }
        String text = src.replace("\r\n", "\n").replace('\r', '\n');
        List<String> out = new ArrayList<>();
        boolean inFence = false;

        for (String raw : text.split("\n", -1)) {
            if (FENCE.matcher(raw).matches()) {
                inFence = !inFence;
                continue;                       // 代码围栏本身不显示，内容保留
            }
            String line = raw;
            if (!inFence) {
                if (HR.matcher(line).matches()) {
                    continue;
                }
                if (isTableSeparator(line)) {
                    continue;
                }
                line = HEADING.matcher(line).replaceFirst("");
                line = QUOTE.matcher(line).replaceFirst("");
                if (BULLET.matcher(line).find()) {
                    line = BULLET.matcher(line).replaceFirst("· ");
                }
                if (line.indexOf('|') >= 0) {
                    line = line.replace('|', ' ').replaceAll(" {2,}", " ").strip();
                }
                line = inline(line);
            }
            out.add(line);
        }
        String joined = String.join("\n", out);
        joined = BLANK_RUN.matcher(joined).replaceAll("\n\n");
        return joined.strip();
    }

    /** 只把真正的表格分隔行（含 | 且只有 - : | 空格）丢掉，避免误伤 --- 翻页标记。 */
    private static boolean isTableSeparator(String line) {
        if (line.indexOf('|') < 0) {
            return false;
        }
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c != '|' && c != '-' && c != ':' && c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    private static String inline(String s) {
        String line = s;
        line = IMAGE.matcher(line).replaceAll("[图片]");
        line = LINK.matcher(line).replaceAll("$1");
        line = AUTOLINK.matcher(line).replaceAll("$1");
        line = INLINE_CODE.matcher(line).replaceAll("$1");
        line = BOLD.matcher(line).replaceAll("$2");
        line = STRIKE.matcher(line).replaceAll("$1");
        line = ITALIC.matcher(line).replaceAll("$2");
        return line;
    }
}
