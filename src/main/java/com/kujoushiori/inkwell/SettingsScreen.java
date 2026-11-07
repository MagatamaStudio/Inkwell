package com.kujoushiori.inkwell;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 游戏内的设置界面：改完点保存立即生效，直接写回 inkwell.properties。 */
public class SettingsScreen extends Screen {
    private static final int LABEL_W = 96;
    private static final int MARGIN = 24;

    private final Screen parent;

    private EditBox folder;
    private EditBox file;
    private EditBox breakLine;
    private EditBox lines;
    private EditBox chars;
    private EditBox pages;
    private EditBox maxMb;

    private boolean displayPaging;
    private boolean markdown;
    private boolean longOnly;
    private boolean recursive;
    private boolean debug;

    // 布局参数（init 里算，render 里复用）
    private int inputX;
    private int inputW;
    private int col2;
    private int toggleW;
    private int rowY;
    private int rowStep;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Inkwell 设置"));
        this.parent = parent;
        InkwellConfig c = InkwellConfig.get();
        this.displayPaging = c.displayPaging;
        this.markdown = c.markdown;
        this.longOnly = c.longClipboardOnly;
        this.recursive = c.recursive;
        this.debug = c.debug;
    }

    @Override
    protected void init() {
        InkwellConfig c = InkwellConfig.get();

        // 列宽随窗口自适应，窄窗口也放得下
        inputX = MARGIN + LABEL_W;
        int half = Math.max(200, this.width / 2);
        inputW = Math.min(160, Math.max(80, half - inputX - 16));
        col2 = Math.min(this.width - 16 - 110, Math.max(inputX + inputW + 16, half + 4));
        toggleW = Math.max(110, Math.min(176, this.width - col2 - 16));

        // 7 行 + 按钮 + 标题，按窗口高度压缩行距
        rowY = 34;
        rowStep = Math.max(16, Math.min(24, (this.height - 84) / 7));

        folder = box(inputX, rowY, inputW, c.folder);
        file = box(inputX, rowY + rowStep, inputW, c.file);
        breakLine = box(inputX, rowY + rowStep * 2, inputW, c.pageBreakLine == null ? "" : c.pageBreakLine);
        lines = box(inputX, rowY + rowStep * 3, Math.min(60, inputW), Integer.toString(c.linesPerPage));
        chars = box(inputX, rowY + rowStep * 4, Math.min(60, inputW), Integer.toString(c.charsPerPage));
        pages = box(inputX, rowY + rowStep * 5, Math.min(60, inputW), Integer.toString(c.maxPages));
        maxMb = box(inputX, rowY + rowStep * 6, Math.min(60, inputW),
                Long.toString(Math.max(1, c.maxFileBytes / (1024L * 1024L))));

        int ty = rowY;
        toggle(col2, ty, "按显示宽度分页", displayPaging, v -> displayPaging = v);
        toggle(col2, ty + rowStep, "转换 Markdown", markdown, v -> markdown = v);
        toggle(col2, ty + rowStep * 2, "长文本才接管 Ctrl+V", longOnly, v -> longOnly = v);
        toggle(col2, ty + rowStep * 3, "文本库平铺（递归）", recursive, v -> recursive = v);
        toggle(col2, ty + rowStep * 4, "调试日志", debug, v -> debug = v);

        int bottom = this.height - 30;
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> save())
                .pos(this.width / 2 - 92, bottom).size(88, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> back())
                .pos(this.width / 2 + 4, bottom).size(88, 20).build());
        addRenderableWidget(Button.builder(Component.literal("恢复默认"), b -> {
            InkwellConfig.get().resetToDefaults();
            rebuildWidgets();
        }).pos(MARGIN, bottom).size(88, 20).build());
    }

    private EditBox box(int x, int y, int w, String value) {
        EditBox e = new EditBox(this.font, x, y, w, 18, Component.literal(""));
        e.setValue(value == null ? "" : value);
        addRenderableWidget(e);
        return e;
    }

    private void toggle(int x, int y, String label, boolean value, java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(Button.builder(Component.literal(label + (value ? "：开" : "：关")), b -> {
            setter.accept(!value);
            rebuildWidgets();
        }).pos(x, y).size(toggleW, Math.max(18, rowStep - 2)).build());
    }

    private void save() {
        InkwellConfig c = InkwellConfig.get();
        c.folder = blankTo(folder.getValue(), "inkwell");
        c.file = blankTo(file.getValue(), "book.txt");
        c.pageBreakLine = breakLine.getValue();
        c.linesPerPage = InkwellConfig.clamp(parseInt(lines.getValue(), c.linesPerPage), 0, 56);
        c.charsPerPage = InkwellConfig.clamp(parseInt(chars.getValue(), c.charsPerPage), 16,
                TextPager.VANILLA_PAGE_CHARS);
        c.maxPages = InkwellConfig.clamp(parseInt(pages.getValue(), c.maxPages), 1, TextPager.VANILLA_MAX_PAGES);
        c.maxFileBytes = Math.max(1, parseInt(maxMb.getValue(), (int) (c.maxFileBytes / (1024L * 1024L))))
                * 1024L * 1024L;
        c.displayPaging = displayPaging;
        c.markdown = markdown;
        c.longClipboardOnly = longOnly;
        c.recursive = recursive;
        c.debug = debug;
        c.save();
        Inkwell.log("设置已保存：目录=" + c.folder + "，每页 " + c.linesPerPage + " 行 / " + c.charsPerPage
                + " 字，最多 " + c.maxPages + " 页");
        back();
    }

    private static String blankTo(String s, String def) {
        return s == null || s.isBlank() ? def : s.trim();
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private void back() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractRenderState(g, mouseX, mouseY, partial);
        try {
            g.text(this.font, Component.literal("Inkwell 设置"), MARGIN, 14, 0xFFFFFFFF);

            label(g, rowY, "文件目录");
            label(g, rowY + rowStep, "默认文件");
            label(g, rowY + rowStep * 2, "翻页标记");
            label(g, rowY + rowStep * 3, "每页行数");
            label(g, rowY + rowStep * 4, "每页字数");
            label(g, rowY + rowStep * 5, "最多页数");
            label(g, rowY + rowStep * 6, "大小上限(MB)");

            // 空间够才画底部提示，避免压住最后一行
            int hintY = rowY + rowStep * 7 + 6;
            if (hintY + 10 < this.height - 34) {
                g.text(this.font, Component.literal("保存后立即生效，同时写回 config/inkwell.properties"),
                        MARGIN, hintY, 0xFF909090);
            }
        } catch (Throwable t) {
            Inkwell.log("设置界面绘制失败：" + t);
        }
    }

    private void label(GuiGraphicsExtractor g, int y, String text) {
        g.text(this.font, Component.literal(text), MARGIN, y + 5, 0xFFB0B0B0);
    }
}
