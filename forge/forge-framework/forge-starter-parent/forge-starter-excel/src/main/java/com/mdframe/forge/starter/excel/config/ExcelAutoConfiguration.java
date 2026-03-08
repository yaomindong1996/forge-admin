package com.mdframe.forge.starter.excel.config;

import com.mdframe.forge.starter.excel.core.ExcelExporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Excel模块自动配置
 */
@Configuration
public class ExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExcelExporter excelExporter() {
        return new ExcelExporter();
    }
}
