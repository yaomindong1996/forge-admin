package com.mdframe.forge.starter.apiconfig.registry;

import com.mdframe.forge.starter.apiconfig.config.ApiConfigProperties;
import com.mdframe.forge.starter.apiconfig.domain.entity.SysApiConfig;
import com.mdframe.forge.starter.apiconfig.mapper.SysApiConfigMapper;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API配置自动注册器
 * 在Spring容器启动完成后，扫描所有Controller中的接口，自动注册到数据库
 * 规则：数据库中不存在的接口则自动插入，已存在的则不覆盖（以数据库为准）
 */
@Slf4j
//@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.api-config", name = "auto-register", havingValue = "true", matchIfMissing = true)
public class ApiConfigAutoRegistrar implements ApplicationListener<ContextRefreshedEvent> {

    private final ApiConfigScanner apiConfigScanner;
    private final SysApiConfigMapper apiConfigMapper;
    private final ApiConfigProperties configProperties;
    private final IApiConfigManager apiConfigManager;
    
    // 创建线程池用于异步处理
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 异步执行，避免阻塞主线程
//        CompletableFuture.runAsync(this::registerApiConfigsAsync, executorService)
//            .thenRun(() -> log.info("API配置自动注册任务已提交到后台执行"))
//            .exceptionally(throwable -> {
//                log.error("API配置自动注册提交失败", throwable);
//                return null;
//            });
    }

    /**
     * 异步注册API配置
     */
    @IgnoreTenant
    public void registerApiConfigsAsync() {
        log.info("开始异步自动注册API配置...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 扫描所有接口
            List<ApiConfigScanner.ApiConfigMeta> apiList = apiConfigScanner.scanAllApis();
            log.info("扫描到 {} 个API接口", apiList.size());

            // 2. 批量处理优化
            processApiConfigsInBatches(apiList);

            // 3. 预热缓存（异步执行）
            if (configProperties.isCacheWarmUp()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        apiConfigManager.warmUpCache();
                        log.info("缓存预热完成");
                    } catch (Exception e) {
                        log.error("缓存预热失败", e);
                    }
                }, executorService);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("API配置自动注册任务完成，总耗时: {}ms", elapsed);

        } catch (Exception e) {
            log.error("API配置自动注册失败", e);
        }
    }

    /**
     * 批量处理API配置注册
     */
    private void processApiConfigsInBatches(List<ApiConfigScanner.ApiConfigMeta> apiList) {
        final int batchSize = 100; // 批处理大小
        final AtomicInteger totalCount = new AtomicInteger(apiList.size());
        final AtomicInteger processedCount = new AtomicInteger(0);
        final AtomicInteger insertCount = new AtomicInteger(0);
        final AtomicInteger skipCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);

        // 分批处理
        for (int i = 0; i < apiList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, apiList.size());
            List<ApiConfigScanner.ApiConfigMeta> batch = apiList.subList(i, endIndex);
            
            // 异步处理每个批次
            CompletableFuture.runAsync(() -> {
                processBatch(batch, insertCount, skipCount, errorCount, processedCount, totalCount);
            }, executorService);
        }
    }

    /**
     * 处理单个批次
     */
    protected void processBatch(List<ApiConfigScanner.ApiConfigMeta> batch, AtomicInteger insertCount,
            AtomicInteger skipCount, AtomicInteger errorCount, AtomicInteger processedCount, AtomicInteger totalCount) {
        try {
            for (ApiConfigScanner.ApiConfigMeta meta : batch) {
                try {
                    // 检查数据库是否已存在
                    SysApiConfig existingConfig = apiConfigMapper.selectByUrlAndMethod(
                        meta.getUrlPath(), meta.getReqMethod());

                    if (existingConfig == null) {
                        // 数据库不存在，插入新配置
                        SysApiConfig newConfig = meta.toEntity();
                        newConfig.setCreateTime(LocalDateTime.now());
                        newConfig.setUpdateTime(LocalDateTime.now());
                        newConfig.setRemark("系统自动注册");
                        apiConfigMapper.insert(newConfig);
                        insertCount.incrementAndGet();
                        log.debug("自动注册API配置: {} {}", meta.getReqMethod(), meta.getUrlPath());
                    } else {
                        // 数据库已存在，跳过（以数据库为准）
                        skipCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("注册API配置失败: {} {}, error={}",
                        meta.getReqMethod(), meta.getUrlPath(), e.getMessage());
                } finally {
                    int currentProcessed = processedCount.incrementAndGet();
                    if (currentProcessed % 50 == 0 || currentProcessed == totalCount.get()) {
                        log.info("处理进度: {}/{} (新增:{}, 跳过:{}, 错误:{})",
                            currentProcessed, totalCount.get(),
                            insertCount.get(), skipCount.get(), errorCount.get());
                    }
                }
            }
        } catch (Exception e) {
            log.error("批次处理失败", e);
        }
    }
}
