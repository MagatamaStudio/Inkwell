package com.kujoushiori.inkwell;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * 把一整段文字切成若干“书页”。
 *
 * <p>上限是原版《书与笔》编辑界面的真实限制（26.3 里 BookEditScreen.init 写死的）：
 * <b>每页 1024 个字符、14 行</b>，整本最多 100 页。超过任意一条，游戏会静默截断。</p>
 *
 * <p>规则：</p>
 * <ol>
 *   <li>换行统一成 \n；</li>
 *   <li>遇到“单独一行的 ---”强制翻页；</li>
 *   <li>否则按字符数和行数双重限制装页，优先在换行处断开；</li>
 *   <li>单行本身超过一页才硬切；</li>
 *   <li>空页丢弃；超过 maxPages 截断并标记。</li>
 * </ol>
 */
public final class TextPager {
    /** 原版：WritableBookContent.PAGE_EDIT_LENGTH */
    public static final int VANILLA_PAGE_CHARS = 1024;
    /** 原版：BookEditScreen.init 里 setLineLimit(126 / 9) */
    public static final int VANILLA_PAGE_LINES = 14;
    /** 原版：WritableBookContent.MAX_PAGES */
    public static final int VANILLA_MAX_PAGES = 100;

    private TextPager() {
    }

    public record Result(java.util.List<String> pages, boolean truncated, int sourceChars) {
    }

    public static Result paginate(String raw, int maxChars, int maxLines, int maxPages, String breakLine) {
        String text = raw == null ? "" : raw.replace("\r\n", "\n").replace('\r', '\n');
        int src = text.length();
        java.util.List<String> pages = new java.util.ArrayList<>();
        if (text.isBlank()) {
            return new Result(pages, false, src);
        }
        int charLimit = Math.max(1, maxChars);
        int lineLimit = maxLines <= 0 ? Integer.MAX_VALUE : Math.max(1, maxLines);

        StringBuilder cur = new StringBuilder();
        int curLines = 0;

        for (String line : text.split("\n", -1)) {
            if (breakLine != null && !breakLine.isBlank() && line.trim().equals(breakLine)) {
                flush(pages, cur);
                curLines = 0;
                continue;
            }
            // 单行超过一页：先冲掉当前页，再硬切
            while (line.length() > charLimit) {
                flush(pages, cur);
                curLines = 0;
                pages.add(line.substring(0, charLimit));
                line = line.substring(charLimit);
            }
            boolean needBreak = cur.length() > 0
                    && (cur.length() + 1 + line.length() > charLimit || curLines + 1 > lineLimit);
            if (needBreak) {
                flush(pages, cur);
                curLines = 0;
            }
            if (cur.length() > 0) {
                cur.append('\n');
            }
            cur.append(line);
            curLines++;
        }
        flush(pages, cur);

        boolean truncated = false;
        if (pages.size() > maxPages) {
            pages = new java.util.ArrayList<>(pages.subList(0, maxPages));
            truncated = true;
        }
        return new Result(pages, truncated, src);
    }

    private static void flush(java.util.List<String> pages, StringBuilder cur) {
        String s = cur.toString().strip();
        if (!s.isEmpty()) {
            pages.add(s);
        }
        cur.setLength(0);
    }

    /** 解码结果，encoding 用来在日志里说明为什么不是 UTF-8。 */
    public record Decoded(String text, String encoding) {
    }

    /**
     * 文本文件解码：先认 BOM，再严格按 UTF-8 试；不是合法 UTF-8 就按 GB18030（中文 Windows 的 ANSI）读。
     * 记事本默认存 ANSI / 带 BOM 的 Unicode，直接当 UTF-8 读会整篇乱码。
     */
    public static Decoded decode(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) {
            return new Decoded(new String(b, 3, b.length - 3, StandardCharsets.UTF_8), "UTF-8");
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
            return new Decoded(new String(b, 2, b.length - 2, StandardCharsets.UTF_16LE), "UTF-16LE");
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
            return new Decoded(new String(b, 2, b.length - 2, StandardCharsets.UTF_16BE), "UTF-16BE");
        }
        String utf8 = strictDecode(b, StandardCharsets.UTF_8);
        if (utf8 != null) {
            return new Decoded(utf8, "UTF-8");
        }
        String gb = strictDecode(b, Charset.forName("GB18030"));
        if (gb != null) {
            return new Decoded(gb, "GB18030");
        }
        return new Decoded(new String(b, StandardCharsets.UTF_8), "UTF-8");
    }

    private static String strictDecode(byte[] b, Charset cs) {
        try {
            return cs.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(b))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- 自测
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 40; i++) {
            sb.append("第").append(i).append("段：这是一段测试文字，用来验证分页逻辑是否正常。\n");
        }
        Report("40 段（1024 字 / 14 行）", paginate(sb.toString(), 1024, 14, 100, "---"));

        StringBuilder manyLines = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            manyLines.append("短行").append(i + 1).append("\n");
        }
        Report("40 行短句（行数上限先触发）", paginate(manyLines.toString(), 1024, 14, 100, "---"));

        Report("--- 强制翻页", paginate("A\n---\nB\n---\nC", 1024, 14, 100, "---"));
        Report("5000 字 限 3 页", paginate("x".repeat(5000), 1024, 14, 3, "---"));
        Report("空文本", paginate("   \n\n  ", 1024, 14, 100, "---"));
    }

    private static void Report(String name, Result r) {
        int maxLen = r.pages().stream().mapToInt(String::length).max().orElse(0);
        int maxLines = r.pages().stream().mapToInt(p -> p.split("\n", -1).length).max().orElse(0);
        System.out.println(name + " -> " + r.pages().size() + " 页, 单页最长 " + maxLen
                + " 字 / " + maxLines + " 行, 截断=" + r.truncated());
    }
}
