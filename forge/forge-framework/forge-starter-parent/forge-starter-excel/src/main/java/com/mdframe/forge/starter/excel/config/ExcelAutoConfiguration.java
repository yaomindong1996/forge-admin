package com.mdframe.forge.starter.excel.config;

import com.mdframe.forge.starter.excel.controller.ExcelEnhancedController;
import com.mdframe.forge.starter.excel.core.DynamicExportEngine;
import com.mdframe.forge.starter.excel.core.ExcelExporter;
import com.mdframe.forge.starter.excel.service.AsyncExportService;
import com.mdframe.forge.starter.excel.service.ExcelImportService;
import com.mdframe.forge.starter.excel.service.impl.AsyncExportServiceImpl;
import com.mdframe.forge.starter.excel.service.impl.ExcelImportServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Excel 模块自动配置
 */
@Configuration
@EnableAsync
public class ExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExcelExporter excelExporter() {
        return new ExcelExporter();
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncExportService asyncExportService(DynamicExportEngine dynamicExportEngine) {
        return new AsyncExportServiceImpl(dynamicExportEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelImportService excelImportService() {
        return new ExcelImportServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelEnhancedController excelEnhancedController(ExcelImportService importService, AsyncExportService exportService) {
        return new ExcelEnhancedController(importService, exportService);
    }
}
