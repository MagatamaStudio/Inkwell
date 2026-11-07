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
 * 文本库：搜索 / 排序 / 选择 txt / 右侧预览 / 写入方式 / 导入前确认。
 */
public class TextLibraryScreen extends Screen {
    private static final int PER_PAGE = 8;
    private static final int LIST_X = 16;
    private static final int LIST_W = 150;

    private final BookEditScreen book;
    private final List<Path> all;

    private final List<Path> view = new ArrayList<>();
    private String filter = "";
    private int sortMode;          // 0=名称 1=修改时间 2=大小
    private boolean appendMode;    // 追加到末尾
    private int page;              // 列表第几页
    private int selected;          // 选中项在 view 里的下标

    private String content = "";
    private int chars;
    private int pages;

    public TextLibraryScreen(BookEditScreen book) {
        super(Component.literal("文本库"));
        this.book = book;
        this.all = Inkwell.listTxt();
        this.selected = -1;
        refreshView();
    }

    // ---------------------------------------------------------------- 列表
    private void refreshView() {
        view.clear();
        String f = filter == null ? "" : filter.trim().toLowerCase();
        for (Path p : all) {
            String name = p.getFileName().toString();
            if (f.isEmpty() || name.toLowerCase().contains(f)) {
                view.add(p);
            }
        }
        Comparator<Path> cmp;
        switch (sortMode) {
            case 1:
                cmp = Comparator.comparingLong(TextLibraryScreen::modified).reversed();
                break;
            case 2:
                cmp = Comparator.comparingLong(TextLibraryScreen::size).reversed();
                break;
            default:
                cmp = Comparator.comparing(p -> p.getFileName().toString().toLowerCase());
        }
        view.sort(cmp);
        if (selected >= view.size()) {
            selected = view.isEmpty() ? -1 : 0;
        }
        if (page * PER_PAGE > view.size()) {
            page = 0;
        }
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

    private Path selectedPath() {
        return selected >= 0 && selected < view.size() ? view.get(selected) : null;
    }

    // ---------------------------------------------------------------- 布局
    @Override
    protected void init() {
        try {
            EditBox search = new EditBox(this.font, LIST_X, 28, LIST_W, 18, Component.literal("搜索"));
            search.setValue(filter);
            search.setHint(Component.literal("搜索文件名…"));
            search.setResponder(s -> {
                filter = s;
                page = 0;
                refreshView();
                rebuildWidgets();
            });
            addRenderableWidget(search);

            addRenderableWidget(Button.builder(Component.literal("排序：" + sortName()), b -> {
                sortMode = (sortMode + 1) % 3;
                refreshView();
                rebuildWidgets();
            }).pos(LIST_X + LIST_W + 6, 28).size(92, 18).build());

            addRenderableWidget(Button.builder(
                    Component.literal(appendMode ? "写入：末尾追加" : "写入：从当前页"), b -> {
                        appendMode = !appendMode;
                        rebuildWidgets();
                    }).pos(LIST_X + LIST_W + 6 + 96, 28).size(112, 18).build());

            buildFileButtons();

            int bottom = this.height - 32;
            addRenderableWidget(Button.builder(Component.literal("导入到书里"), b -> askImport())
                    .pos(this.width - 132, bottom).size(120, 20).build());
            addRenderableWidget(Button.builder(Component.literal("返回"), b -> back())
                    .pos(this.width - 262, bottom).size(120, 20).build());

            if (view.size() > PER_PAGE) {
                if (page > 0) {
                    addRenderableWidget(Button.builder(Component.literal("上一页"), b -> {
                        page--;
                        rebuildWidgets();
                    }).pos(LIST_X, bottom).size(52, 20).build());
                }
                if ((page + 1) * PER_PAGE < view.size()) {
                    addRenderableWidget(Button.builder(Component.literal("下一页"), b -> {
                        page++;
                        rebuildWidgets();
                    }).pos(LIST_X + 56, bottom).size(52, 20).build());
                }
            }
        } catch (Throwable t) {
            Inkwell.log("文本库 init 失败：" + t);
        }
    }

    private String sortName() {
        return switch (sortMode) {
            case 1 -> "修改时间";
            case 2 -> "文件大小";
            default -> "名称";
        };
    }

    private void buildFileButtons() {
        int from = page * PER_PAGE;
        int to = Math.min(view.size(), from + PER_PAGE);
        for (int i = from; i < to; i++) {
            final int idx = i;
            String name = view.get(i).getFileName().toString();
            String label = (i == selected ? "▶ " : "") + name;
            if (label.length() > 17) {
                label = label.substring(0, 16) + "…";
            }
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                selected = idx;
                loadPreview();
                rebuildWidgets();
            }).pos(LIST_X, 52 + (i - from) * 22).size(LIST_W, 20).build());
        }
    }

    private void loadPreview() {
        content = "";
        chars = 0;
        pages = 0;
        Path p = selectedPath();
        if (p == null) {
            return;
        }
        String text = Inkwell.readFile(p.getFileName().toString());
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
        Path p = selectedPath();
        if (p == null) {
            Inkwell.log("没选中文件");
            return;
        }
        String text = Inkwell.readFile(p.getFileName().toString());
        if (text == null) {
            return;
        }
        List<String> newPages = Inkwell.paginate(text);
        if (newPages == null || newPages.isEmpty()) {
            return;
        }
        int total = BookWriter.pageCount(book);
        int cur = BookWriter.currentPage(book) + 1;
        String msg = appendMode
                ? "把《" + p.getFileName() + "》的 " + newPages.size() + " 页追加到末尾。\n当前共 " + total + " 页，追加后 "
                        + Math.min(BookWriter.MAX_PAGES, total + newPages.size()) + " 页。"
                : "把《" + p.getFileName() + "》的 " + newPages.size() + " 页写入书里。\n会从第 " + cur
                        + " 页开始覆盖，原来第 " + cur + " 页及之后的内容都没了（当前共 " + total + " 页）。";

        final List<String> pagesToWrite = newPages;
        final boolean append = appendMode;
        Minecraft.getInstance().setScreenAndShow(new ConfirmScreen(ok -> {
            if (ok) {
                boolean done = append
                        ? BookWriter.append(book, pagesToWrite)
                        : BookWriter.write(book, pagesToWrite);
                if (done) {
                    Inkwell.log("已从文本库导入 " + p.getFileName());
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
            int top = 52;
            int right = this.width - 16;
            int bottom = this.height - 44;

            g.fill(left - 4, top - 4, right, bottom, 0xC0101010);
            g.text(this.font, Component.literal("文本库 · " + view.size() + " / " + all.size() + " 个文件"),
                    LIST_X, 10, 0xFFFFFFFF);

            Path sel = selectedPath();
            if (sel == null) {
                g.text(this.font, Component.literal(all.isEmpty() ? "没有找到 txt / md 文件" : "没有匹配的文件"),
                        left, top, 0xFFFF8080);
                g.text(this.font, Component.literal(InkwellConfig.get().folderPath().toString()),
                        left, top + 14, 0xFFA0A0A0);
                return;
            }
            g.text(this.font, Component.literal("预览：" + sel.getFileName()), left, top, 0xFFFFE07A);
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
