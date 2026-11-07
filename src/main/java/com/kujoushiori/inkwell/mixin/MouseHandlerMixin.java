package com.kujoushiori.inkwell.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kujoushiori.inkwell.Actions;
import com.kujoushiori.inkwell.Inkwell;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * 中键粘贴：某些中文输入法会把 Ctrl+V 吃掉，鼠标中键不受影响，作为备用入口。
 * 只在《书与笔》界面里拦截，其他界面照常。
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void inkwellOnButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        try {
            if (action != 1 || info.button() != 2) {
                return;
            }
            if (Inkwell.currentScreen() instanceof BookEditScreen book) {
                if (Actions.importClipboard(book, true)) {
                    ci.cancel();
                }
            }
        } catch (Throwable t) {
            Inkwell.log("中键粘贴失败：" + t);
        }
    }
}
