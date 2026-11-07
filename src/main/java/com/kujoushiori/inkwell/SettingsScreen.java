package com.kujoushiori.inkwell;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 游戏内的设置界面：改完点保存立即生效，直接写回 inkwell.properties。 */
public class SettingsScreen extends Screen {
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

    private int col2;

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
        int x = 24;
        int y = 40;
        int step = 24;
        col2 = Math.max(220, this.width / 2 + 8);

        folder = box(x, y, 150, c.folder);
        file = box(x, y + step, 150, c.file);
        breakLine = box(x, y + step * 2, 150, c.pageBreakLine == null ? "" : c.pageBreakLine);
        lines = box(x, y + step * 3, 60, Integer.toString(c.linesPerPage));
        chars = box(x, y + step * 4, 60, Integer.toString(c.charsPerPage));
        pages = box(x, y + step * 5, 60, Integer.toString(c.maxPages));
        maxMb = box(x, y + step * 6, 60, Long.toString(Math.max(1, c.maxFileBytes / (1024L * 1024L))));

        int y2 = y;
        toggle(col2, y2, "按显示宽度分页", displayPaging, v -> displayPaging = v);
        toggle(col2, y2 + step, "转换 Markdown", markdown, v -> markdown = v);
        toggle(col2, y2 + step * 2, "长文本才接管 Ctrl+V", longOnly, v -> longOnly = v);
        toggle(col2, y2 + step * 3, "文本库递归扫描", recursive, v -> recursive = v);
        toggle(col2, y2 + step * 4, "调试日志", debug, v -> debug = v);

        int bottom = this.height - 32;
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> save())
                .pos(this.width / 2 - 96, bottom).size(90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> back())
                .pos(this.width / 2 + 6, bottom).size(90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("恢复默认"), b -> {
            InkwellConfig.get().resetToDefaults();
            rebuildWidgets();
        }).pos(24, bottom).size(90, 20).build());
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
        }).pos(x, y).size(170, 20).build());
    }

    private void save() {
        InkwellConfig c = InkwellConfig.get();
        c.folder = blankTo(folder.getValue(), "inkwell");
        c.file = blankTo(file.getValue(), "book.txt");
        c.pageBreakLine = breakLine.getValue();
        c.linesPerPage = InkwellConfig.clamp(parseInt(lines.getValue(), c.linesPerPage), 0, 56);
        c.charsPerPage = InkwellConfig.clamp(parseInt(chars.getValue(), c.charsPerPage), 16, TextPager.VANILLA_PAGE_CHARS);
        c.maxPages = InkwellConfig.clamp(parseInt(pages.getValue(), c.maxPages), 1, TextPager.VANILLA_MAX_PAGES);
        c.maxFileBytes = Math.max(1, parseInt(maxMb.getValue(), (int) (c.maxFileBytes / (1024L * 1024L)))) * 1024L * 1024L;
        c.displayPaging = displayPaging;
        c.markdown = markdown;
        c.longClipboardOnly = longOnly;
        c.recursive = recursive;
        c.debug = debug;
        c.save();
        Inkwell.log("设置已保存：目录=" + c.folder + " 每页 " + c.linesPerPage + " 行 / " + c.charsPerPage
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
            int y = 40;
            int step = 24;
            g.text(this.font, Component.literal("Inkwell 设置"), 24, 14, 0xFFFFFFFF);
            label(g, 24, y, "文件目录");
            label(g, 24, y + step, "默认文件");
            label(g, 24, y + step * 2, "翻页标记");
            label(g, 24, y + step * 3, "每页行数");
            label(g, 24, y + step * 4, "每页字数");
            label(g, 24, y + step * 5, "最多页数");
            label(g, 24, y + step * 6, "大小上限(MB)");
            g.text(this.font, Component.literal("改完点保存，立即生效；配置文件在 config/inkwell.properties"),
                    24, this.height - 52, 0xFF909090);
        } catch (Throwable t) {
            Inkwell.log("设置界面绘制失败：" + t);
        }
    }

    private void label(GuiGraphicsExtractor g, int x, int y, String text) {
        g.text(this.font, Component.literal(text), x + 156, y + 5, 0xFFA0A0A0);
    }
}
