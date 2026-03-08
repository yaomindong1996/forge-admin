package com.mdframe.forge.starter.property.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 配置变更监听器
 * 定时检查数据库配置是否发生变更
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.config", name = "auto-refresh", havingValue = "true", matchIfMissing = true)
public class ConfigChangeListener {
    
    private final ConfigRefresher configRefresher;
    
    /**
     * 定时刷新配置
     * 默认每30秒检查一次
     */
    //@Scheduled(fixedDelayString = "${forge.config.refresh-interval:30000}")
    public void checkAndRefresh() {
        //log.debug("开始检查配置变更...");
        boolean refreshed = configRefresher.refresh();
        if (refreshed) {
            log.info("检测到配置变更，已自动刷新");
        }
    }
}
