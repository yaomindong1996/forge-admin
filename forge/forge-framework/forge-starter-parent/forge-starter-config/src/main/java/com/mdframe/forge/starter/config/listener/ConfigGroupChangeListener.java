package com.mdframe.forge.starter.config.listener;

import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import com.mdframe.forge.starter.config.service.ConfigSyncService;
import com.mdframe.forge.starter.property.event.ConfigRefreshEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 配置分组变更监听器
 * 监听配置分组的变更事件，并同步到sys_config表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigGroupChangeListener {

    private final ConfigSyncService configSyncService;
    

    /**
     * 监听配置刷新事件
     * 当通过其他方式触发配置刷新时，也同步配置分组
     */
    @Async
    @EventListener
    public void onConfigRefresh(ConfigRefreshEvent event) {
        log.info("检测到配置刷新事件，同步配置分组...");
        configSyncService.syncAllConfigs();
    }
}
