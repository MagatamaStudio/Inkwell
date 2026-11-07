package com.kujoushiori.inkwell.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kujoushiori.inkwell.Actions;
import com.kujoushiori.inkwell.Inkwell;
import com.kujoushiori.inkwell.InkwellConfig;
import com.kujoushiori.inkwell.PageJumpScreen;
import com.kujoushiori.inkwell.SettingsScreen;
import com.kujoushiori.inkwell.TextLibraryScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * 书与笔界面：
 *   1) 右侧按钮：文本库 / 粘贴剪贴板 / 跳转页码 / 设置 / 签名
 *   2) 快捷键：Ctrl+V、Ctrl+Shift+V、Ctrl+Alt+V
 *   3) 中键粘贴（输入法吞掉 Ctrl+V 时的备用路径，见 MouseHandlerMixin）
 */
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin {
    private static final int GLFW_KEY_V = 86;
    private static final int BTN_W = 104;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 4;

    @Shadow
    private int currentPage;

    @Shadow
    private List<String> pages;

    @Inject(method = "init", at = @At("TAIL"))
    private void inkwellAddButtons(CallbackInfo ci) {
        try {
            BookEditScreen this0 = (BookEditScreen) (Object) this;
            BookEditScreenAccessor acc = (BookEditScreenAccessor) this0;
            int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int bl = acc.inkwellBackgroundLeft();
            int bt = acc.inkwellBackgroundTop();

            int x = bl + 192 + 6;
            if (x + BTN_W > w - 4) {
                x = bl - BTN_W - 6;
            }
            if (x < 4) {
                x = 4;
            }
            int y = Math.max(4, bt);

            addButton(x, y, "文本库 / 预览", b -> Minecraft.getInstance()
                    .setScreenAndShow(new TextLibraryScreen(this0)));
            addButton(x, y + BTN_H + BTN_GAP, "粘贴剪贴板", b -> {
                if (!Actions.importClipboard(this0, true)) {
                    Inkwell.log("剪贴板是空的，或者没有可导入的内容");
                }
            });
            addButton(x, y + (BTN_H + BTN_GAP) * 2, "跳转页码…", b -> Minecraft.getInstance()
                    .setScreenAndShow(new PageJumpScreen(this0)));
            addButton(x, y + (BTN_H + BTN_GAP) * 3, "设置", b -> Minecraft.getInstance()
                    .setScreenAndShow(new SettingsScreen(this0)));
            addButton(x, y + (BTN_H + BTN_GAP) * 4, "签名…", b -> {
                var sign = acc.inkwellSignScreen();
                if (sign != null) {
                    Minecraft.getInstance().setScreenAndShow(sign);
                } else {
                    Inkwell.log("拿不到签名界面");
                }
            });
        } catch (Throwable t) {
            Inkwell.log("加按钮失败：" + t);
        }
    }

    private void addButton(int x, int y, String label, Button.OnPress onPress) {
        Button button = Button.builder(Component.literal(label), onPress)
                .pos(x, y)
                .size(BTN_W, BTN_H)
                .build();
        addWidgetReflect(button);
    }

    /** Screen.addRenderableWidget 是 protected，mixin 里直接调不到，用反射。 */
    private void addWidgetReflect(GuiEventListener widget) {
        try {
            Method m = Screen.class.getDeclaredMethod("addRenderableWidget", GuiEventListener.class);
            m.setAccessible(true);
            m.invoke(this, widget);
            return;
        } catch (Throwable ignored) {
            // 退回直接往列表里塞
        }
        for (String name : new String[]{"children", "renderables", "narratables"}) {
            try {
                Field f = Screen.class.getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(this);
                if (v instanceof List<?> list) {
                    ((List) list).add(widget);
                }
            } catch (Throwable ignored) {
                // 忽略
            }
        }
    }

    // ---------------------------------------------------------------- 快捷键
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void inkwellKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        boolean vKey = event.shortcutKey() == 118 || event.key() == GLFW_KEY_V;
        boolean ctrl = event.hasControlDown();
        Inkwell.debug("key: key=" + event.key() + " shortcut=" + event.shortcutKey()
                + " mods=" + event.modifiers() + " ctrl=" + ctrl + " paste=" + event.isPaste());
        if (!(vKey && ctrl)) {
            return;
        }
        try {
            boolean shift = event.hasShiftDown();
            boolean alt = event.hasAltDown();
            BookEditScreen self = (BookEditScreen) (Object) this;
            if (alt) {
                if (Actions.importFile(self, InkwellConfig.get().file)) {
                    cir.setReturnValue(true);
                }
                return;
            }
            if (Actions.importClipboard(self, shift)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            Inkwell.log("导入出错：" + t);
        }
    }
}
