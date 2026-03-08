package com.mdframe.forge.plugin.job.executor.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.executor.IJobExecutorRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 远程执行器路由（分布式模式）
 * 通过HTTP调用远程服务执行任务
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "forge.job", name = "deploy-mode", havingValue = "DISTRIBUTED")
@RequiredArgsConstructor
public class RemoteJobExecutorRouter implements IJobExecutorRouter {
    
    private final JobProperties jobProperties;
    
    @Override
    public String route(String executeMode,
                       String executorBean,
                       String executorMethod,
                       String executorHandler,
                       String executorService,
                       String jobParam) throws Exception {
        
        if (!"RPC".equals(executeMode)) {
            throw new UnsupportedOperationException("远程路由仅支持RPC模式");
        }
        
        if (executorService == null || executorService.isEmpty()) {
            throw new RuntimeException("未指定执行器服务名称");
        }
        
        return executeRpcMode(executorService, executorHandler, jobParam);
    }
    
    @Override
    public boolean support(String executeMode) {
        return "RPC".equals(executeMode);
    }
    
    /**
     * RPC模式执行
     */
    private String executeRpcMode(String serviceName, String handlerName, String param) throws Exception {
        // 构建请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("handlerName", handlerName);
        requestBody.put("param", param);
        
        String url = buildServiceUrl(serviceName);
        
        log.info("远程调用任务执行器: service={}, handler={}, url={}", serviceName, handlerName, url);
        
        // 发起HTTP调用（支持重试）
        int retryCount = jobProperties.getDistributed().getRetryCount();
        Exception lastException = null;
        
        for (int i = 0; i <= retryCount; i++) {
            try {
                HttpResponse response = HttpRequest.post(url)
                        .body(JSONObject.toJSONString(requestBody))
                        .timeout(jobProperties.getDistributed().getTimeout())
                        .execute();
                
                if (response.isOk()) {
                    String result = response.body();
                    log.info("远程执行成功: {}", result);
                    return result;
                } else {
                    throw new RuntimeException("远程调用失败: " + response.getStatus());
                }
            } catch (Exception e) {
                lastException = e;
                if (i < retryCount) {
                    log.warn("远程调用失败，第{}次重试: {}", i + 1, e.getMessage());
                    Thread.sleep(1000 * (i + 1)); // 递增延迟
                }
            }
        }
        
        throw new RuntimeException("远程调用失败，已重试" + retryCount + "次", lastException);
    }
    
    /**
     * 构建服务URL
     * 支持从注册中心获取服务地址（Nacos/Eureka）
     */
    private String buildServiceUrl(String serviceName) {
        // TODO: 集成服务发现（Nacos/Eureka/Consul）
        // 这里先简化处理，实际应该从注册中心获取服务实例
        
        // 格式：http://{serviceName}/job/executor/execute
        return "http://" + serviceName + "/job/executor/execute";
    }
}
