package com.mdframe.forge.starter.core.config;

import com.mdframe.forge.starter.core.exception.GlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 异常处理自动配置类
 */
@Slf4j
@AutoConfiguration
public class ExceptionAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        log.info("全局异常处理器已启用");
        return new GlobalExceptionHandler();
    }
}
