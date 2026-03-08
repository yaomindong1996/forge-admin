package com.mdframe.forge.plugin.job.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.plugin.job.executor.IJobExecutor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务执行器端点
 * 供调度中心远程调用，执行本地Handler
 */
@Slf4j
@RestController
@RequestMapping("/job/executor")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.job", name = "executor-enabled", havingValue = "true", matchIfMissing = true)
public class JobExecutorEndpoint {
    
    private final ApplicationContext applicationContext;
    
    /**
     * 执行任务
     */
    @PostMapping("/execute")
    public RespInfo<String> execute(@RequestBody ExecuteRequest request) {
        try {
            log.info("接收远程任务执行请求: handler={}, param={}",
                    request.getHandlerName(), request.getParam());
            
            // 获取Handler Bean
            IJobExecutor executor = applicationContext.getBean(request.getHandlerName(), IJobExecutor.class);
            
            // 执行任务
            String result = executor.execute(request.getParam());
            
            log.info("任务执行成功: {}", result);
            return RespInfo.success(result);
            
        } catch (Exception e) {
            log.error("任务执行失败", e);
            return RespInfo.error("执行失败: " + e.getMessage());
        }
    }
    
    @Data
    public static class ExecuteRequest {
        private String handlerName;
        private String param;
    }
}
