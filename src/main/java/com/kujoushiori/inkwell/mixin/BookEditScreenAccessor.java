package com.kujoushiori.inkwell.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;

/** BookEditScreen 的私有字段/方法访问器（mixin 接口，任何类都能用）。 */
@Mixin(BookEditScreen.class)
public interface BookEditScreenAccessor {
    @Accessor("pages")
    List<String> inkwellPages();

    @Accessor("currentPage")
    int inkwellCurrentPage();

    @Accessor("currentPage")
    void inkwellSetCurrentPage(int page);

    @Accessor("page")
    MultiLineEditBox inkwellPage();

    @Invoker("updatePageContent")
    void inkwellUpdatePageContent();

    @Invoker("updateButtonVisibility")
    void inkwellUpdateButtonVisibility();

    @Invoker("backgroundLeft")
    int inkwellBackgroundLeft();

    @Invoker("backgroundTop")
    int inkwellBackgroundTop();
}
