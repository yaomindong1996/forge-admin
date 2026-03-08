package com.mdframe.forge.starter.config.service;

import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 配置中心服务
 * 处理分布式环境下的配置同步和一致性问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigCenterService {

    private final ConfigSyncService configSyncService;
    private final ApplicationEventPublisher eventPublisher;

    // 使用本地锁来防止并发配置同步
    private final Object syncLock = new Object();

    /**
     * 触发配置同步（本地锁保护）
     */
    public boolean triggerConfigSync(String triggerNode) {
        synchronized (syncLock) {
            try {
                log.info("节点[{}]开始同步配置", triggerNode);
                
                // 执行配置同步
                boolean syncResult = configSyncService.syncAllConfigs();
                
                return syncResult;
            } catch (Exception e) {
                log.error("配置同步过程中发生异常", e);
                return false;
            }
        }
    }

    /**
     * 强制同步配置
     */
    public boolean forceSyncConfig() {
        synchronized (syncLock) {
            log.info("强制同步配置开始");
            boolean result = configSyncService.syncAllConfigs();
            log.info("强制同步配置结束，结果: {}", result);
            return result;
        }
    }
}
