package com.kujoushiori.inkwell;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 文本库：文件夹导航 / 递归扫描 / 搜索 / 排序 / 预览 / 导入。
 */
public class TextLibraryScreen extends Screen {
    private static final int PER_PAGE = 8;
    private static final int LIST_X = 16;
    private static final int LIST_W = 150;

    private enum Kind { DIR, FILE, HEADER }

    private record Entry(Path path, Kind kind) {
        boolean dir() {
            return kind == Kind.DIR;
        }

        boolean header() {
            return kind == Kind.HEADER;
        }

        boolean file() {
            return kind == Kind.FILE;
        }
    }

    private final BookEditScreen book;
    private Path root;
    private Path dir;

    private final List<Entry> view = new ArrayList<>();
    private String filter = "";
    private int sortMode;          // 0=名称 1=修改时间 2=大小
    private boolean appendMode;    // 追加到末尾
    private boolean recursive;
    private int page;
    private int selected;

    private String content = "";
    private int chars;
    private int pages;

    // 布局（init 里按窗口算，绘制和列表复用）
    private boolean compact;
    private int listTop;
    private int rowStep;
    private int rowH;
    private int bottomY;

    public TextLibraryScreen(BookEditScreen book) {
        super(Component.literal("文本库"));
        this.book = book;
        this.root = Inkwell.root();
        this.dir = root;
        this.recursive = InkwellConfig.get().recursive;
        this.selected = -1;
        refreshView();
    }

    /** 从设置界面回来后重新读配置（目录 / 递归都可能变了）。 */
    public void reloadConfig() {
        InkwellConfig cfg = InkwellConfig.get();
        this.root = Inkwell.root();
        this.recursive = cfg.recursive;
        this.dir = root;
        this.filter = "";
        this.selected = -1;
        this.page = 0;
        this.appendMode = false;
        refreshView();
        loadPreview();
        rebuildWidgets();
    }

    // ---------------------------------------------------------------- 列表
    private void refreshView() {
        Path keep = selectedFile();
        view.clear();
        String f = filter == null ? "" : filter.trim().toLowerCase();
        boolean searching = !f.isEmpty();

        if (!recursive) {
            for (Path d : Inkwell.listDirs(dir)) {
                String name = d.getFileName().toString();
                if (f.isEmpty() || name.toLowerCase().contains(f)) {
                    view.add(new Entry(d, Kind.DIR));
                }
            }
        }

        List<Path> files = Inkwell.listFiles(dir, recursive);
        if (recursive && !searching) {
            // 递归模式：按文件夹分组，每组前面加一个标题行
            files.sort(Comparator.comparing(this::parentLabel).thenComparing(this::fileKey));
            Path lastParent = null;
            for (Path p : files) {
                Path parent = p.getParent();
                if (lastParent == null || !parent.equals(lastParent)) {
                    view.add(new Entry(parent, Kind.HEADER));
                    lastParent = parent;
                }
                view.add(new Entry(p, Kind.FILE));
            }
        } else {
            for (Path p : files) {
                String name = searching ? Inkwell.relativePath(dir, p) : p.getFileName().toString();
                if (searching && !name.toLowerCase().contains(f)) {
                    continue;
                }
                view.add(new Entry(p, Kind.FILE));
            }
            view.sort(Comparator.comparing((Entry e) -> fileKey(e.path())));
        }

        if (!recursive) {
            view.sort(Comparator.comparing((Entry e) -> !e.dir()).thenComparing(e -> fileKey(e.path())));
        }

        // 按路径恢复选中项；原文件被过滤掉就取消选中，避免“预览和选中项不是同一篇”
        selected = keep == null ? -1 : indexOfPath(keep);
        if (selected < 0) {
            selected = -1;
        }
        // 选中项落在分组标题上时，自动挪到下一个文件
        while (selected >= 0 && selected < view.size() && !view.get(selected).file()) {
            selected++;
        }
        if (selected >= view.size()) {
            selected = lastFileIndex();
        }

        int pageCount = (view.size() + PER_PAGE - 1) / PER_PAGE;
        if (page >= pageCount) {
            page = Math.max(0, pageCount - 1);
        }
    }

    private int indexOfPath(Path p) {
        for (int i = 0; i < view.size(); i++) {
            Entry e = view.get(i);
            if (e.file() && e.path().equals(p)) {
                return i;
            }
        }
        return -1;
    }

    private int lastFileIndex() {
        for (int i = view.size() - 1; i >= 0; i--) {
            if (view.get(i).file()) {
                return i;
            }
        }
        return -1;
    }

    private String parentLabel(Path p) {
        Path parent = p.getParent();
        if (parent == null || parent.equals(dir)) {
            return "";
        }
        return Inkwell.relativePath(dir, parent).toLowerCase();
    }

    private String fileKey(Path p) {
        switch (sortMode) {
            case 1:
                return String.format("%020d", Long.MAX_VALUE - modified(p));
            case 2:
                return String.format("%020d", Long.MAX_VALUE - size(p));
            default:
                return p.getFileName().toString().toLowerCase();
        }
    }

    private String label(Entry e) {
        if (e.header()) {
            Path parent = e.path();
            if (parent == null || parent.equals(dir)) {
                return "▾ " + dir.getFileName() + "/";
            }
            return "▾ " + Inkwell.relativePath(dir, parent) + "/";
        }
        if (e.dir()) {
            return "[" + e.path().getFileName() + "]";
        }
        return e.path().getFileName().toString();
    }

    private static long modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static long size(Path p) {
        try {
            return Files.size(p);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Entry selectedEntry() {
        return selected >= 0 && selected < view.size() ? view.get(selected) : null;
    }

    private Path selectedFile() {
        Entry e = selectedEntry();
        return e != null && e.file() ? e.path() : null;
    }

    // ---------------------------------------------------------------- 布局
    private void computeLayout() {
        compact = this.width < 520;
        listTop = compact ? 72 : 52;
        bottomY = this.height - 30;
        int usable = Math.max(PER_PAGE * 12, bottomY - 4 - listTop);
        rowStep = Math.max(12, Math.min(22, usable / PER_PAGE));
        rowH = Math.max(10, Math.min(20, rowStep - 2));
    }

    @Override
    protected void init() {
        try {
            computeLayout();
            int top = 28;
            int settingsW = 58;
            int settingsX = this.width - 16 - settingsW;

            if (compact) {
                // 窄窗口：搜索框 + 设置一行，排序/写入/递归第二行
                int searchW = Math.max(100, settingsX - 8 - LIST_X);
                EditBox search = new EditBox(this.font, LIST_X, top, searchW, 18, Component.literal("搜索"));
                search.setValue(filter);
                search.setHint(Component.literal("搜索文件名…"));
                search.setResponder(s -> {
                    filter = s;
                    page = 0;
                    refreshView();
                    loadPreview();
                    rebuildWidgets();
                });
                addRenderableWidget(search);

                int row2 = top + 22;
                int sortW = 84;
                int recW = 72;
                int recX = this.width - 16 - recW;
                int writeX = LIST_X + sortW + 4;
                int writeW = Math.max(80, recX - 4 - writeX);
                addRenderableWidget(Button.builder(Component.literal("排序：" + sortName()), b -> {
                    sortMode = (sortMode + 1) % 3;
                    refreshView();
                    loadPreview();
                    rebuildWidgets();
                }).pos(LIST_X, row2).size(sortW, 18).build());
                addRenderableWidget(Button.builder(
                        Component.literal(appendMode ? "写入：末尾追加" : "写入：从当前页"), b -> {
                            appendMode = !appendMode;
                            rebuildWidgets();
                        }).pos(writeX, row2).size(writeW, 18).build());
                addRenderableWidget(Button.builder(Component.literal(recursive ? "递归：开" : "递归：关"), b -> {
                    recursive = !recursive;
                    selected = -1;
                    page = 0;
                    refreshView();
                    loadPreview();
                    rebuildWidgets();
                }).pos(recX, row2).size(recW, 18).build());
            } else {
                EditBox search = new EditBox(this.font, LIST_X, top, 140, 18, Component.literal("搜索"));
                search.setValue(filter);
                search.setHint(Component.literal("搜索文件名…"));
                search.setResponder(s -> {
                    filter = s;
                    page = 0;
                    refreshView();
                    loadPreview();
                    rebuildWidgets();
                });
                addRenderableWidget(search);

                addRenderableWidget(Button.builder(Component.literal("排序：" + sortName()), b -> {
                    sortMode = (sortMode + 1) % 3;
                    refreshView();
                    loadPreview();
                    rebuildWidgets();
                }).pos(LIST_X + 144, top).size(92, 18).build());

                addRenderableWidget(Button.builder(
                        Component.literal(appendMode ? "写入：末尾追加" : "写入：从当前页"), b -> {
                            appendMode = !appendMode;
                            rebuildWidgets();
                        }).pos(LIST_X + 240, top).size(112, 18).build());

                addRenderableWidget(Button.builder(Component.literal(recursive ? "递归：开" : "递归：关"), b -> {
                    recursive = !recursive;
                    selected = -1;
                    page = 0;
                    refreshView();
                    loadPreview();
                    rebuildWidgets();
                }).pos(LIST_X + 356, top).size(76, 18).build());
            }

            addRenderableWidget(Button.builder(Component.literal("设置"), b -> Minecraft.getInstance()
                    .setScreenAndShow(new SettingsScreen(this)))
                    .pos(settingsX, 10).size(settingsW, 18).build());

            buildList();
            buildBottomBar();
        } catch (Throwable t) {
            Inkwell.log("文本库 init 失败：" + t);
        }
    }

    private void buildBottomBar() {
        boolean paging = view.size() > PER_PAGE;
        if (compact) {
            int importW = 92;
            int importX = this.width - 16 - importW;
            int backW = 56;
            int backX = importX - 4 - backW;

            int x = LIST_X;
            if (!dir.equals(root)) {
                addRenderableWidget(Button.builder(Component.literal("←上级"), b -> {
                    Path up = dir.getParent();
                    dir = (up != null && up.startsWith(root)) ? up : root;
                    selected = -1;
                    page = 0;
                    refreshView();
                    loadPreview();
                    rebuildWidgets();
                }).pos(x, bottomY).size(44, 20).build());
                x += 48;
            }
            if (paging) {
                if (page > 0) {
                    addRenderableWidget(Button.builder(Component.literal("上一页"), b -> turnPage(-1))
                            .pos(x, bottomY).size(40, 20).build());
                    x += 44;
                }
                if ((page + 1) * PER_PAGE < view.size()) {
                    addRenderableWidget(Button.builder(Component.literal("下一页"), b -> turnPage(1))
                            .pos(x, bottomY).size(40, 20).build());
                }
            }
            addRenderableWidget(Button.builder(Component.literal("返回"), b -> back())
                    .pos(backX, bottomY).size(backW, 20).build());
            addRenderableWidget(Button.builder(Component.literal("导入到书里"), b -> askImport())
                    .pos(importX, bottomY).size(importW, 20).build());
        } else {
            int px = LIST_X;
            if (!dir.equals(root)) {
                addRenderableWidget(Button.builder(Component.literal("← 上一级"), b -> {
                    Path up = dir.getParent();
                    dir = (up != null && up.startsWith(root)) ? up : root;
                    selected = -1;
                    page = 0;
                    refreshView();
                    loadPreview();
                    rebuildWidgets();
                }).pos(LIST_X, bottomY).size(72, 20).build());
                px = LIST_X + 80;
            }
            if (paging) {
                if (page > 0) {
                    addRenderableWidget(Button.builder(Component.literal("上一页"), b -> turnPage(-1))
                            .pos(px, bottomY).size(52, 20).build());
                    px += 56;
                }
                if ((page + 1) * PER_PAGE < view.size()) {
                    addRenderableWidget(Button.builder(Component.literal("下一页"), b -> turnPage(1))
                            .pos(px, bottomY).size(52, 20).build());
                }
            }
            addRenderableWidget(Button.builder(Component.literal("导入到书里"), b -> askImport())
                    .pos(this.width - 132, bottomY).size(120, 20).build());
            addRenderableWidget(Button.builder(Component.literal("返回"), b -> back())
                    .pos(this.width - 262, bottomY).size(120, 20).build());
        }
    }

    private void turnPage(int delta) {
        page += delta;
        if (selected >= 0 && (selected < page * PER_PAGE || selected >= (page + 1) * PER_PAGE)) {
            selected = -1;
            loadPreview();
        }
        rebuildWidgets();
    }

    private String sortName() {
        return switch (sortMode) {
            case 1 -> "修改时间";
            case 2 -> "文件大小";
            default -> "名称";
        };
    }

    private void buildList() {
        int from = page * PER_PAGE;
        int to = Math.min(view.size(), from + PER_PAGE);
        for (int i = from; i < to; i++) {
            final int idx = i;
            Entry e = view.get(i);
            String text = (i == selected ? "▶ " : "") + label(e);
            if (text.length() > 18) {
                text = text.substring(0, 17) + "…";
            }
            addRenderableWidget(Button.builder(Component.literal(text), b -> {
                if (e.dir() || e.header()) {
                    dir = e.path();
                    recursive = false;
                    selected = -1;
                    page = 0;
                    refreshView();
                    loadPreview();
                } else {
                    selected = idx;
                    loadPreview();
                }
                rebuildWidgets();
            }).pos(LIST_X, listTop + (i - from) * rowStep).size(LIST_W, rowH).build());
        }
    }

    private void loadPreview() {
        content = "";
        chars = 0;
        pages = 0;
        Path p = selectedFile();
        if (p == null) {
            return;
        }
        String text = Inkwell.readText(p);
        if (text == null) {
            return;
        }
        chars = text.length();
        List<String> pageList = Inkwell.paginate(text);
        if (pageList == null || pageList.isEmpty()) {
            return;
        }
        pages = pageList.size();
        String first = pageList.get(0);
        content = first.length() > 900 ? first.substring(0, 900) + "…" : first;
    }

    // ---------------------------------------------------------------- 导入
    private void askImport() {
        Path p = selectedFile();
        if (p == null) {
            Inkwell.log("没选中文件");
            return;
        }
        String text = Inkwell.readText(p);
        if (text == null) {
            return;
        }
        List<String> newPages = Inkwell.paginate(text);
        if (newPages == null || newPages.isEmpty()) {
            return;
        }
        int total = BookWriter.pageCount(book);
        int cur = BookWriter.currentPage(book) + 1;
        String name = p.getFileName().toString();
        String msg = appendMode
                ? "把《" + name + "》的 " + newPages.size() + " 页追加到末尾。\n当前共 " + total + " 页，追加后 "
                        + Math.min(BookWriter.MAX_PAGES, total + newPages.size()) + " 页。"
                : "把《" + name + "》的 " + newPages.size() + " 页写入书里。\n会从第 " + cur
                        + " 页开始覆盖，原来第 " + cur + " 页及之后的内容都没了（当前共 " + total + " 页）。";

        final List<String> pagesToWrite = newPages;
        final boolean append = appendMode;
        Minecraft.getInstance().setScreenAndShow(new ConfirmScreen(ok -> {
            if (ok) {
                boolean done = append
                        ? BookWriter.append(book, pagesToWrite)
                        : BookWriter.write(book, pagesToWrite);
                if (done) {
                    Inkwell.log("已从文本库导入 " + name);
                    Minecraft.getInstance().setScreenAndShow(book);
                } else {
                    Minecraft.getInstance().setScreenAndShow(this);
                }
            } else {
                Minecraft.getInstance().setScreenAndShow(this);
            }
        }, Component.literal("确认导入"), Component.literal(msg)));
    }

    private void back() {
        Minecraft.getInstance().setScreenAndShow(book);
    }

    // ---------------------------------------------------------------- 绘制
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractRenderState(g, mouseX, mouseY, partial);
        try {
            int left = 180;
            int top = listTop;
            int right = this.width - 16;
            int bottom = bottomY - 8;

            g.fill(left - 4, top - 4, right, bottom, 0xC0101010);

            String crumb = "inkwell" + (dir.equals(root) ? "" : " / " + Inkwell.relativePath(root, dir));
            if (crumb.length() > 46) {
                crumb = "…" + crumb.substring(crumb.length() - 45);
            }
            g.text(this.font, Component.literal(crumb + "   ·   " + view.size() + " 项"), LIST_X, 10, 0xFFFFFFFF);

            Entry sel = selectedEntry();
            if (sel == null) {
                g.text(this.font, Component.literal(view.isEmpty() ? "这里没有 txt / md 文件" : "没选中文件"),
                        left, top, 0xFFA0A0A0);
                return;
            }
            if (sel.dir() || sel.header()) {
                Path folder = sel.path();
                g.text(this.font, Component.literal("文件夹：" + folder.getFileName()), left, top, 0xFFFFE07A);
                g.text(this.font, Component.literal("点一下进入这个文件夹"), left, top + 14, 0xFF9AD0FF);
                return;
            }
            g.text(this.font, Component.literal("预览：" + Inkwell.relativePath(root, sel.path())),
                    left, top, 0xFFFFE07A);
            g.text(this.font, Component.literal("共 " + chars + " 字 · 约 " + pages + " 页（按实际显示宽度算）"),
                    left, top + 12, 0xFF9AD0FF);

            List<FormattedCharSequence> lines = this.font.split(Component.literal(content), right - left - 12);
            int y = top + 30;
            for (FormattedCharSequence line : lines) {
                if (y > bottom - 14) {
                    g.text(this.font, Component.literal("…"), left, y, 0xFF808080);
                    break;
                }
                g.text(this.font, line, left, y, 0xFFE0E0E0);
                y += 10;
            }
        } catch (Throwable t) {
            Inkwell.log("文本库绘制失败：" + t);
        }
    }
}
