package com.kujoushiori.inkwell;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;

/** 页码跳转：输入第几页，回车或点“跳转”就到那一页。 */
public class PageJumpScreen extends Screen {
    private final BookEditScreen book;
    private EditBox input;

    public PageJumpScreen(BookEditScreen book) {
        super(Component.literal("跳转页码"));
        this.book = book;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int total = Math.max(1, BookWriter.pageCount(book));

        input = new EditBox(this.font, cx - 60, cy - 12, 120, 20, Component.literal("页码"));
        try {
            input.setMaxLength(4);
        } catch (Throwable ignored) {
            // 方法名变了也没关系，不影响跳转
        }
        input.setValue(Integer.toString(BookWriter.currentPage(book) + 1));
        addRenderableWidget(input);
        setInitialFocus(input);

        addRenderableWidget(Button.builder(Component.literal("跳转"), b -> doJump())
                .pos(cx - 60, cy + 14).size(58, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> back())
                .pos(cx + 2, cy + 14).size(58, 20).build());
    }

    private void doJump() {
        int n;
        try {
            n = Integer.parseInt(input.getValue().trim());
        } catch (NumberFormatException e) {
            Inkwell.log("页码不是数字：" + input.getValue());
            return;
        }
        if (BookWriter.jumpTo(book, n - 1)) {
            back();
        }
    }

    private void back() {
        Minecraft.getInstance().setScreenAndShow(book);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == 257 || event.key() == 335) {   // Enter / 小键盘 Enter
            doJump();
            return true;
        }
        if (event.key() == 256) {                          // Esc
            back();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractRenderState(g, mouseX, mouseY, partial);
        try {
            int total = Math.max(1, BookWriter.pageCount(book));
            int current = BookWriter.currentPage(book) + 1;
            String title = "跳转到第几页（共 " + total + " 页，当前第 " + current + " 页）";
            int w = this.font.width(title);
            g.text(this.font, Component.literal(title), (this.width - w) / 2, this.height / 2 - 40, 0xFFFFFFFF);
        } catch (Throwable t) {
            Inkwell.log("跳转界面绘制失败：" + t);
        }
    }
}
