package com.kujoushiori.inkwell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;

/** 读文件、找文件、打日志。 */
public final class Inkwell {
    private Inkwell() {
    }

    public static Path gameDir() {
        return Minecraft.getInstance().gameDirectory.toPath();
    }

    public static void log(String msg) {
        InkwellClient.LOGGER.info(msg);
    }

    public static void debug(String msg) {
        if (InkwellConfig.get().debug) {
            InkwellClient.LOGGER.info("[debug] " + msg);
        }
    }

    private static java.lang.reflect.Field SCREEN_FIELD;

    /** 当前打开的界面。26.3 的 Minecraft 里没有公开的 screen 字段，反射找一次缓存起来。 */
    public static net.minecraft.client.gui.screens.Screen currentScreen() {
        try {
            if (SCREEN_FIELD == null) {
                for (java.lang.reflect.Field f : Minecraft.class.getDeclaredFields()) {
                    if (net.minecraft.client.gui.screens.Screen.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        SCREEN_FIELD = f;
                        break;
                    }
                }
            }
            return SCREEN_FIELD == null ? null
                    : (net.minecraft.client.gui.screens.Screen) SCREEN_FIELD.get(Minecraft.getInstance());
        } catch (Throwable t) {
            return null;
        }
    }

    /** 文本根目录（配置项 folder）。 */
    public static Path root() {
        Path dir = InkwellConfig.get().folderPath();
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log("创建目录失败：" + e);
            }
        }
        return dir;
    }

    /** 某个目录下的子目录，按名字排序。 */
    public static List<Path> listDirs(Path dir) {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(out::add);
        } catch (IOException e) {
            log("列目录失败：" + e);
        }
        return out;
    }

    /**
     * 文本文件。recursive=true 时把子文件夹里的也一起列出来（按相对路径排序）。
     */
    public static List<Path> listFiles(Path dir, boolean recursive) {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> s = recursive ? Files.walk(dir) : Files.list(dir)) {
            s.filter(Files::isRegularFile)
                    .filter(Inkwell::isTextFile)
                    .forEach(out::add);
        } catch (IOException e) {
            log("列文件失败：" + e);
        }
        out.sort(Comparator.comparing(p -> relativePath(dir, p).toLowerCase()));
        return out;
    }

    /** 相对根目录的路径，用于显示。 */
    public static String relativePath(Path base, Path file) {
        try {
            return base.relativize(file).toString().replace('\\', '/');
        } catch (Exception e) {
            return file.getFileName().toString();
        }
    }

    /** 读取任意文本文件（带 Markdown 转换），失败返回 null。 */
    public static String readText(Path p) {
        InkwellConfig cfg = InkwellConfig.get();
        Path root = cfg.folderPath().normalize();
        Path abs = p.toAbsolutePath().normalize();
        if (!abs.startsWith(root)) {
            log("文件不在 " + root + " 里：" + abs);
            return null;
        }
        if (!Files.isRegularFile(abs)) {
            log("找不到文件：" + abs);
            return null;
        }
        try {
            String text = TextPager.readUtf8(abs, cfg.maxFileBytes);
            if (cfg.markdown && isMarkdown(abs.getFileName().toString())) {
                String plain = Markdown.toPlain(text);
                log("已把 " + abs.getFileName() + " 的 Markdown 语法清掉（" + text.length()
                        + " -> " + plain.length() + " 字）");
                return plain;
            }
            return text;
        } catch (IOException e) {
            log("读取失败：" + e);
            return null;
        }
    }

    /** 读取根目录下的某个文件（默认 book.txt）。 */
    public static String readFile(String name) {
        InkwellConfig cfg = InkwellConfig.get();
        Path dir = cfg.folderPath();
        Path p = dir.resolve(name).normalize();
        if (!p.startsWith(dir.normalize())) {
            log("文件名不合法：" + name);
            return null;
        }
        return readText(p);
    }

    public static boolean isMarkdown(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".md") || n.endsWith(".markdown");
    }

    public static boolean isTextFile(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".txt") || n.endsWith(".md") || n.endsWith(".markdown");
    }

    /** 分页；返回 null 表示没有可用内容。 */
    public static List<String> paginate(String text) {
        InkwellConfig cfg = InkwellConfig.get();
        List<String> pages;

        if (cfg.displayPaging) {
            try {
                var font = Minecraft.getInstance().font;
                pages = DisplayPager.paginate(text, font, DisplayPager.TEXT_WIDTH, cfg.linesPerPage,
                        cfg.charsPerPage, cfg.maxPages, cfg.pageBreakLine);
                if (pages.isEmpty()) {
                    log("内容为空，什么都没做");
                    return null;
                }
                StringBuilder sb = new StringBuilder("按显示宽度分页：")
                        .append(pages.size()).append(" 页 / 共 ").append(text.length()).append(" 字（每页 ")
                        .append(cfg.linesPerPage).append(" 行 × ").append(DisplayPager.TEXT_WIDTH).append("px）");
                if (pages.size() >= cfg.maxPages) {
                    sb.append("（可能被 ").append(cfg.maxPages).append(" 页上限截断）");
                }
                log(sb.toString());
                return pages;
            } catch (Throwable t) {
                log("按显示分页失败，退回按字符分页：" + t);
            }
        }

        TextPager.Result r = TextPager.paginate(text, cfg.charsPerPage, cfg.linesPerPage, cfg.maxPages, cfg.pageBreakLine);
        if (r.pages().isEmpty()) {
            log("内容为空，什么都没做");
            return null;
        }
        StringBuilder sb = new StringBuilder("分页完成：")
                .append(r.pages().size()).append(" 页 / 共 ").append(r.sourceChars()).append(" 字");
        if (r.truncated()) {
            sb.append("（超过 ").append(cfg.maxPages).append(" 页，后面被截掉了）");
        }
        log(sb.toString());
        return r.pages();
    }
}
