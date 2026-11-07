package com.kujoushiori.inkwell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;

/**
 * 进游戏时先跑一遍：写一次配置、在日志里留一行“我加载了”，方便排查。
 */
public class InkwellClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("inkwell");

    @Override
    public void onInitializeClient() {
        InkwellConfig cfg = InkwellConfig.get();
        LOGGER.info("Book Paste 已加载 | 目录={} | 每页 {} 字 / {} 行 | 最多 {} 页 | debug={}",
                cfg.folderPath(), cfg.charsPerPage, cfg.linesPerPage, cfg.maxPages, cfg.debug);
    }
}
