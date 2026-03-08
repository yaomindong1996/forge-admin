package com.mdframe.forge.plugin.job.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 任务调度自动配置
 */
@Slf4j
@Configuration
@ComponentScan("com.mdframe.forge.plugin.job")
@EnableConfigurationProperties(JobProperties.class)
public class JobAutoConfiguration {

//    /**
//     * 默认日志存储
//     */
//    @Bean
//    @ConditionalOnMissingBean
//    public IJobLogStorage jobLogStorage() {
//        log.warn("使用默认日志存储（仅输出到日志），生产环境请实现IJobLogStorage接口");
//        return new DefaultJobLogStorage();
//    }
}
