package com.kujoushiori.inkwell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public static String readUtf8(Path p, long maxBytes) throws IOException {
        if (Files.size(p) > maxBytes) {
            throw new IOException("文件过大：" + Files.size(p) + " 字节（上限 " + maxBytes + "）");
        }
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
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
