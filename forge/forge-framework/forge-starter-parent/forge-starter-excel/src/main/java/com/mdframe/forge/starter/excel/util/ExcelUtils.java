package com.mdframe.forge.starter.excel.util;

import com.mdframe.forge.starter.excel.annotation.ExcelExport;
import com.mdframe.forge.starter.excel.core.ExcelExporter;
import com.mdframe.forge.starter.excel.model.ExcelExportConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;

/**
 * Excel导出静态工具类
 * 提供便捷的静态方法
 */
@Component
public class ExcelUtils implements ApplicationContextAware {

    private static ExcelExporter excelExporter;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        excelExporter = applicationContext.getBean(ExcelExporter.class);
    }

    /**
     * 导出到响应流
     */
    public static <T> void export(HttpServletResponse response, List<T> data, Class<T> clazz, String fileName) {
        ExcelExportConfig config = buildConfig(clazz, fileName);
        excelExporter.export(response, data, clazz, config);
    }

    /**
     * 导出到输出流
     */
    public static <T> void export(OutputStream outputStream, List<T> data, Class<T> clazz) {
        ExcelExportConfig config = buildConfig(clazz, null);
        excelExporter.export(outputStream, data, clazz, config);
    }

    /**
     * 自定义配置导出
     */
    public static <T> void export(HttpServletResponse response, List<T> data, Class<T> clazz, ExcelExportConfig config) {
        if (config == null) {
            config = buildConfig(clazz, null);
        }
        excelExporter.export(response, data, clazz, config);
    }

    /**
     * 构建配置（自动识别注解）
     */
    private static <T> ExcelExportConfig buildConfig(Class<T> clazz, String fileName) {
        ExcelExport annotation = clazz.getAnnotation(ExcelExport.class);
        ExcelExportConfig.ExcelExportConfigBuilder builder = ExcelExportConfig.builder();

        if (annotation != null) {
            builder.sheetName(annotation.sheetName())
                   .autoTrans(annotation.autoTrans())
                   .filterNull(annotation.filterNull());
        }

        if (fileName != null && !fileName.isEmpty()) {
            builder.fileName(fileName);
        }

        return builder.build();
    }
}
