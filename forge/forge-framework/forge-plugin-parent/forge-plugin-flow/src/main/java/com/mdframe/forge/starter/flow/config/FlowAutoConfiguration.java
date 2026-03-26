package com.mdframe.forge.starter.flow.config;

import com.mdframe.forge.starter.flow.listener.FlowTaskEventListener;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

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
@EnableAsync
@AutoConfiguration
public class FlowAutoConfiguration {

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private FlowTaskEventListener flowTaskEventListener;

    /**
     * 禁用 Spring Security 默认的 HTTP Basic 认证，将鉴权完全交给 Sa-Token 拦截器处理。
     * <p>
     * forge-plugin-flow 引入了 flowable-spring-boot-starter 中的 spring-boot-starter-security 依赖，
     * 如果不禁用默认行为，所有未带 HTTP Basic 凭证的请求会返回 401 空响应体，
     * 导致内部服务调用（FlowClient）无法解析响应。
     * </p>
     */
//    @Bean
//    public SecurityFilterChain flowSecurityFilterChain(HttpSecurity http) throws Exception {
//        http
//            // 关闭 CSRF（无状态服务无需）
//            .csrf(AbstractHttpConfigurer::disable)
//            // 无状态，不创建 HttpSession
//            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//            // 所有请求放行（登录校验由 Sa-Token 拦截器负责）
//            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
//            // 禁用 HTTP Basic 认证弹框
//            .httpBasic(AbstractHttpConfigurer::disable)
//            // 禁用表单登录
//            .formLogin(AbstractHttpConfigurer::disable);
//        return http.build();
//    }

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

    /**
     * 流程事件异步线程池
     * <p>专門用于 {@link com.mdframe.forge.starter.flow.event.FlowEventPublisher}
     * 和 {@link com.mdframe.forge.starter.flow.event.FlowWebhookNotifier} 的 {@code @Async} 方法，
     * 避免占用流程引擎线程。</p>
     */
    @Bean(name = "flowEventExecutor")
    public Executor flowEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("flow-event-");
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
    
}
