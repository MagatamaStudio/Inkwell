package com.kujoushiori.inkwell;

import java.util.List;

import com.kujoushiori.inkwell.mixin.BookEditScreenAccessor;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;

/** 把分页结果真正写进《书与笔》里（书页列表 + 编辑框 + 页码刷新）。 */
public final class BookWriter {
    /** 原版 WritableBookContent.MAX_PAGES */
    public static final int MAX_PAGES = 100;

    private BookWriter() {
    }

    public static int pageCount(BookEditScreen screen) {
        return ((BookEditScreenAccessor) screen).inkwellPages().size();
    }

    public static int currentPage(BookEditScreen screen) {
        return ((BookEditScreenAccessor) screen).inkwellCurrentPage();
    }

    /** 跳转到第 page 页（0 起）。 */
    public static boolean jumpTo(BookEditScreen screen, int page) {
        if (screen == null) {
            return false;
        }
        BookEditScreenAccessor acc = (BookEditScreenAccessor) screen;
        int total = acc.inkwellPages().size();
        if (total <= 0) {
            return false;
        }
        int target = Math.max(0, Math.min(page, total - 1));
        acc.inkwellSetCurrentPage(target);
        acc.inkwellUpdatePageContent();
        acc.inkwellUpdateButtonVisibility();
        Inkwell.log("跳转到第 " + (target + 1) + " / " + total + " 页");
        return true;
    }

    /** 从当前页开始覆盖写入。 */
    public static boolean write(BookEditScreen screen, List<String> newPages) {
        if (screen == null || newPages == null || newPages.isEmpty()) {
            return false;
        }
        BookEditScreenAccessor acc = (BookEditScreenAccessor) screen;
        List<String> pages = acc.inkwellPages();
        int current = acc.inkwellCurrentPage();

        int start = Math.max(0, Math.min(current, Math.max(0, pages.size())));
        while (pages.size() > start) {
            pages.remove(pages.size() - 1);
        }
        int room = MAX_PAGES - pages.size();
        int add = Math.min(room, newPages.size());
        pages.addAll(newPages.subList(0, add));
        if (add < newPages.size()) {
            Inkwell.log("超过 " + MAX_PAGES + " 页上限，" + (newPages.size() - add) + " 页被丢掉");
        }

        int page = Math.min(start, pages.size() - 1);
        if (page < 0) {
            page = 0;
        }
        finish(acc, page);
        Inkwell.log("写入 " + add + " 页，当前共 " + pages.size() + " 页");
        return true;
    }

    /** 追加到末尾。 */
    public static boolean append(BookEditScreen screen, List<String> newPages) {
        if (screen == null || newPages == null || newPages.isEmpty()) {
            return false;
        }
        BookEditScreenAccessor acc = (BookEditScreenAccessor) screen;
        List<String> pages = acc.inkwellPages();
        int room = MAX_PAGES - pages.size();
        if (room <= 0) {
            Inkwell.log("已经有 " + pages.size() + " 页了，到上限了");
            return false;
        }
        int startIndex = pages.size();
        int add = Math.min(room, newPages.size());
        pages.addAll(newPages.subList(0, add));
        if (add < newPages.size()) {
            Inkwell.log("超过 " + MAX_PAGES + " 页上限，" + (newPages.size() - add) + " 页被丢掉");
        }
        finish(acc, startIndex);
        Inkwell.log("追加 " + add + " 页，当前共 " + pages.size() + " 页");
        return true;
    }

    private static void finish(BookEditScreenAccessor acc, int page) {
        List<String> pages = acc.inkwellPages();
        int p = Math.max(0, Math.min(page, pages.size() - 1));
        acc.inkwellSetCurrentPage(p);
        MultiLineEditBox box = acc.inkwellPage();
        if (box != null) {
            box.setValue(pages.get(p));
            int want = pages.get(p).length();
            int got = box.getValue().length();
            if (got < want) {
                Inkwell.log("警告：这一页在编辑框里被截断（" + want + " -> " + got + " 字）");
            }
        }
        acc.inkwellUpdatePageContent();
        acc.inkwellUpdateButtonVisibility();
    }
}
