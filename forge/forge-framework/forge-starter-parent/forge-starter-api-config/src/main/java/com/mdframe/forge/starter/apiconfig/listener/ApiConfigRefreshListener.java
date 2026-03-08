package com.mdframe.forge.starter.apiconfig.listener;

import com.mdframe.forge.starter.apiconfig.domain.event.ApiConfigRefreshEvent;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * API配置刷新事件监听器
 * 监听配置变更事件，刷新本地缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiConfigRefreshListener {

    private final IApiConfigManager apiConfigManager;

    /**
     * 监听配置刷新事件
     */
    @Async
    @EventListener
    public void onApiConfigRefresh(ApiConfigRefreshEvent event) {
        log.info("收到API配置刷新事件: type={}, reason={}", event.getRefreshType(), event.getReason());

        try {
            switch (event.getRefreshType()) {
                case SINGLE:
                    // 刷新单个接口配置
                    if (event.getUrlPath() != null && event.getMethod() != null) {
                        apiConfigManager.refreshApiConfig(event.getUrlPath(), event.getMethod());
                    } else if (event.getConfigId() != null) {
                        apiConfigManager.refreshApiConfigById(event.getConfigId());
                    }
                    break;

                case ALL:
                    // 刷新所有接口配置
                    apiConfigManager.refreshAllApiConfig();
                    break;

                case MODULE:
                    // 刷新指定模块的配置（暂未实现）
                    log.warn("MODULE类型的刷新暂未实现");
                    break;

                default:
                    log.warn("未知的刷新类型: {}", event.getRefreshType());
            }
        } catch (Exception e) {
            log.error("处理API配置刷新事件失败", e);
        }
    }
}
