package com.kujoushiori.inkwell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 配置：.minecraft/config/inkwell.properties
 * 第一次进游戏时自动生成；游戏里点「设置」也能改（改完立即生效）。
 */
public final class InkwellConfig {
    private static InkwellConfig instance;

    /** 放 txt 的文件夹（相对 .minecraft） */
    public String folder = "inkwell";
    /** Ctrl+Alt+V 默认导入的文件名 */
    public String file = "book.txt";
    /** 每页最多多少字符（原版 BookEditScreen 里 setCharacterLimit(1024)） */
    public int charsPerPage = TextPager.VANILLA_PAGE_CHARS;
    /** 每页最多多少行（原版 setLineLimit(126 / 9) = 14 行），0 表示不限 */
    public int linesPerPage = TextPager.VANILLA_PAGE_LINES;
    /** 最多多少页（原版上限 100） */
    public int maxPages = TextPager.VANILLA_MAX_PAGES;
    /** 单独一行的这个标记 = 强制翻页；留空表示不用 */
    public String pageBreakLine = "---";
    /** 只有超过一页的剪贴板内容，Ctrl+V 才走“整本导入”，否则交给原版按光标粘贴 */
    public boolean longClipboardOnly = true;
    /** 按“实际显示宽度”分页（推荐）；false 则只按字符数/行数 */
    public boolean displayPaging = true;
    /** 读 .md 时自动清掉 Markdown 语法 */
    public boolean markdown = true;
    /** 文本库默认是否递归扫描子文件夹 */
    public boolean recursive = false;
    /** 打印每个按键事件，排查用 */
    public boolean debug = false;
    /** 单个文件大小上限（字节） */
    public long maxFileBytes = 4L * 1024L * 1024L;

    private boolean loaded;

    public static InkwellConfig get() {
        if (instance == null) {
            instance = new InkwellConfig();
            instance.loadIfNeeded();
        }
        return instance;
    }

    public static Path configPath() {
        return Inkwell.gameDir().resolve("config").resolve("inkwell.properties");
    }

    public Path folderPath() {
        return Inkwell.gameDir().resolve(folder);
    }

    private void loadIfNeeded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path p = configPath();
        if (!Files.exists(p)) {
            save();
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
        } catch (IOException e) {
            Inkwell.log("读取配置失败，用默认值：" + e);
            return;
        }
        folder = props.getProperty("folder", folder);
        file = props.getProperty("file", file);
        charsPerPage = clamp(intOf(props, "charsPerPage", charsPerPage), 16, TextPager.VANILLA_PAGE_CHARS);
        linesPerPage = clamp(intOf(props, "linesPerPage", linesPerPage), 0, 56);
        maxPages = clamp(intOf(props, "maxPages", maxPages), 1, TextPager.VANILLA_MAX_PAGES);
        pageBreakLine = props.getProperty("pageBreakLine", pageBreakLine);
        longClipboardOnly = boolOf(props, "longClipboardOnly", longClipboardOnly);
        maxFileBytes = longOf(props, "maxFileBytes", maxFileBytes);
        displayPaging = boolOf(props, "displayPaging", displayPaging);
        markdown = boolOf(props, "markdown", markdown);
        recursive = boolOf(props, "recursive", recursive);
        debug = boolOf(props, "debug", debug);
    }

    /** 写回配置文件（游戏里改完会调它）。 */
    public void save() {
        Properties props = new Properties();
        props.setProperty("folder", folder);
        props.setProperty("file", file);
        props.setProperty("charsPerPage", Integer.toString(charsPerPage));
        props.setProperty("linesPerPage", Integer.toString(linesPerPage));
        props.setProperty("maxPages", Integer.toString(maxPages));
        props.setProperty("pageBreakLine", pageBreakLine == null ? "" : pageBreakLine);
        props.setProperty("longClipboardOnly", Boolean.toString(longClipboardOnly));
        props.setProperty("maxFileBytes", Long.toString(maxFileBytes));
        props.setProperty("displayPaging", Boolean.toString(displayPaging));
        props.setProperty("markdown", Boolean.toString(markdown));
        props.setProperty("recursive", Boolean.toString(recursive));
        props.setProperty("debug", Boolean.toString(debug));
        try {
            Path p = configPath();
            Files.createDirectories(p.getParent());
            try (OutputStream out = Files.newOutputStream(p)) {
                props.store(out, "inkwell - 书与笔导入；游戏里点「设置」改，改完立即生效");
            }
        } catch (IOException e) {
            Inkwell.log("写配置失败：" + e);
        }
    }

    /** 恢复出厂设置（不写盘）。 */
    public void resetToDefaults() {
        InkwellConfig d = new InkwellConfig();
        folder = d.folder;
        file = d.file;
        charsPerPage = d.charsPerPage;
        linesPerPage = d.linesPerPage;
        maxPages = d.maxPages;
        pageBreakLine = d.pageBreakLine;
        longClipboardOnly = d.longClipboardOnly;
        displayPaging = d.displayPaging;
        markdown = d.markdown;
        recursive = d.recursive;
        debug = d.debug;
        maxFileBytes = d.maxFileBytes;
    }

    private static int intOf(Properties p, String k, int def) {
        try {
            return Integer.parseInt(p.getProperty(k, Integer.toString(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static long longOf(Properties p, String k, long def) {
        try {
            return Long.parseLong(p.getProperty(k, Long.toString(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean boolOf(Properties p, String k, boolean def) {
        return Boolean.parseBoolean(p.getProperty(k, Boolean.toString(def)).trim());
    }

    public static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
