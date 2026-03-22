package com.mdframe.forge.starter.flow.config;

import com.mdframe.forge.starter.flow.listener.FlowTaskEventListener;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Flowable 流程引擎自动配置
 *
 * <p>Flowable 核心组件由 flowable-spring-boot-starter 自动配置，包括：</p>
 * <ul>
 *     <li>ProcessEngine - 流程引擎</li>
 *     <li>RepositoryService - 资源管理服务</li>
 *     <li>RuntimeService - 运行时服务</li>
 *     <li>TaskService - 任务服务</li>
 *     <li>HistoryService - 历史服务</li>
 *     <li>ManagementService - 管理服务</li>
 * </ul>
 *
 * <p>配置通过 application.yml 中的 flowable.* 前缀进行，例如：</p>
 * <pre>
 * flowable:
 *   database-schema-update: true
 *   async-executor-activate: true
 *   history-level: full
 * </pre>
 *
 * @see org.flowable.spring.boot.FlowableAutoConfiguration
 */
@Slf4j
@AutoConfiguration
public class FlowAutoConfiguration {

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private FlowTaskEventListener flowTaskEventListener;

    /**
     * 应用启动完成后注册事件监听器
     * 通过 RuntimeService.addEventListener() 方法动态添加监听器
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerEventListeners() {
        log.info("========== 注册 Flowable 事件监听器 ==========");
        log.info("ProcessEngineConfiguration 类型: {}", processEngineConfiguration.getClass().getName());
        log.info("RuntimeService 实例: {}", runtimeService);
        log.info("FlowTaskEventListener 实例: {}", flowTaskEventListener);
        
        // 获取现有的事件监听器列表
        List<FlowableEventListener> existingListeners = processEngineConfiguration.getEventListeners();
        log.info("现有事件监听器数量: {}", existingListeners != null ? existingListeners.size() : 0);
        
        if (existingListeners != null) {
            for (FlowableEventListener listener : existingListeners) {
                log.info("现有监听器: {}", listener.getClass().getName());
                if (listener instanceof FlowTaskEventListener) {
                    log.info("FlowTaskEventListener 已注册，跳过");
                    log.info("============================================");
                    return;
                }
            }
        }
        
        // 通过 RuntimeService 添加事件监听器
        // RuntimeService.addEventListener() 可以在运行时动态添加监听器
        runtimeService.addEventListener(flowTaskEventListener);
        log.info("通过 RuntimeService 添加 FlowTaskEventListener 成功");
        
        // 验证注册结果
        List<FlowableEventListener> listenersAfterAdd = processEngineConfiguration.getEventListeners();
        log.info("注册后事件监听器数量: {}", listenersAfterAdd != null ? listenersAfterAdd.size() : 0);
        
        // 列出所有已注册的监听器
        if (listenersAfterAdd != null) {
            for (FlowableEventListener listener : listenersAfterAdd) {
                log.info("已注册监听器: {}", listener.getClass().getName());
            }
        }
        log.info("============================================");
    }
    
}
