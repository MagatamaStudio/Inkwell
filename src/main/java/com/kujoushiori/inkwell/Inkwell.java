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
    private static int nextFileIndex = -1;

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

    /** 文件夹里所有 txt / md，按名字排序。 */
    public static List<Path> listTxt() {
        Path dir = InkwellConfig.get().folderPath();
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log("创建目录失败：" + e);
            }
            return out;
        }
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(Files::isRegularFile)
                    .filter(Inkwell::isTextFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(out::add);
        } catch (IOException e) {
            log("列目录失败：" + e);
        }
        return out;
    }

    /** 读取指定文件（默认 book.txt），失败返回 null。 */
    public static String readFile(String name) {
        InkwellConfig cfg = InkwellConfig.get();
        Path dir = cfg.folderPath();
        Path p = dir.resolve(name).normalize();
        if (!p.startsWith(dir.normalize())) {
            log("文件名不合法：" + name);
            return null;
        }
        if (!Files.isRegularFile(p)) {
            log("找不到文件：" + p + "（把 txt 放到 .minecraft/" + cfg.folder + "/ 下）");
            return null;
        }
        try {
            String text = TextPager.readUtf8(p, cfg.maxFileBytes);
            if (cfg.markdown && isMarkdown(p.getFileName().toString())) {
                String plain = Markdown.toPlain(text);
                log("已把 " + p.getFileName() + " 的 Markdown 语法清掉（" + text.length()
                        + " -> " + plain.length() + " 字）");
                return plain;
            }
            return text;
        } catch (IOException e) {
            log("读取失败：" + e);
            return null;
        }
    }

    /** 循环取下一个 txt / md，返回 [文件名, 内容]；没有文件返回 null。 */
    public static String[] readNextFile() {
        List<Path> files = listTxt();
        if (files.isEmpty()) {
            log("文件夹里没有 txt / md：" + InkwellConfig.get().folderPath());
            return null;
        }
        nextFileIndex = (nextFileIndex + 1) % files.size();
        Path p = files.get(nextFileIndex);
        String text = readFile(p.getFileName().toString());
        if (text == null) {
            return null;
        }
        return new String[]{p.getFileName().toString(), text};
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
                var font = net.minecraft.client.Minecraft.getInstance().font;
                pages = DisplayPager.paginate(text, font, DisplayPager.TEXT_WIDTH, cfg.linesPerPage,
                        cfg.charsPerPage, cfg.maxPages, cfg.pageBreakLine);
                if (pages.isEmpty()) {
                    log("内容为空，什么都没做");
                    return null;
                }
                StringBuilder sb = new StringBuilder("按显示宽度分页：")
                        .append(pages.size()).append(" 页 / 共 ").append(text.length()).append(" 字（每页 "
                                ).append(cfg.linesPerPage).append(" 行 × ").append(DisplayPager.TEXT_WIDTH).append("px）");
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
