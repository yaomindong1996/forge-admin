package com.mdframe.forge.plugin.ai.multimodal.image;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicInteger;

/** 图像 I/O 不占用 ReAct 循环池；饱和拒绝，不能回退到请求线程执行。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiImageGenerationProperties.class)
public class AiImageExecutorConfiguration {

    @Bean("forgeAiImageTaskExecutor")
    ThreadPoolTaskExecutor forgeAiImageTaskExecutor(AiImageGenerationProperties properties) {
        if (properties.getMaxPoolSize() < properties.getCorePoolSize()) {
            throw new IllegalArgumentException("图像线程池最大线程数不能小于核心线程数");
        }
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("forge-ai-image-");
        var sequence = new AtomicInteger();
        executor.setThreadFactory(task -> new Thread(null, task,
                executor.getThreadNamePrefix() + sequence.incrementAndGet(), 0, false));
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
