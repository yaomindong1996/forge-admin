package com.mdframe.forge.starter.excel.core;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.mdframe.forge.starter.excel.annotation.ExcelColumn;
import com.mdframe.forge.starter.excel.annotation.ExcelExport;
import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.model.ExcelExportConfig;
import com.mdframe.forge.starter.excel.spi.ExcelConfigProvider;
import com.mdframe.forge.starter.trans.manager.TransManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Excel导出核心类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelExporter {

    @Autowired(required = false)
    private ExcelConfigProvider excelConfigProvider;

    @Autowired(required = false)
    private TransManager transManager; // 动态注入 TransManager（避免硬依赖）

    /**
     * 导出到响应流（最常用）
     */
    public <T> void export(HttpServletResponse response, List<T> data, Class<T> clazz, ExcelExportConfig config) {
        if (data == null || data.isEmpty()) {
            log.warn("导出数据为空");
            return;
        }

        try {
            String fileName = config.getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = clazz.getSimpleName() + "_" + System.currentTimeMillis() + ".xlsx";
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);

            export(response.getOutputStream(), data, clazz, config);
        } catch (IOException e) {
            log.error("Excel导出失败", e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 导出到输出流
     */
    public <T> void export(OutputStream outputStream, List<T> data, Class<T> clazz, ExcelExportConfig config) {
        if (data == null || data.isEmpty()) {
            log.warn("导出数据为空");
            return;
        }

        // 1. 翻译字典
        if (config.getAutoTrans() && transManager != null) {
            translateData(data);
        }

        // 2. 构建EasyExcel Writer
        ExcelWriterBuilder writerBuilder = EasyExcel.write(outputStream);

        // 3. 解析列配置
        List<ColumnMeta> columnMetas = resolveColumns(clazz, config);

        // 4. 动态设置表头和列
        List<List<String>> headers = buildHeaders(columnMetas);
        List<Map<String, Object>> mappedData = mapData(data, columnMetas);

        // 5. 写入数据
        String sheetName = config.getSheetName() != null ? config.getSheetName() : "Sheet1";
        WriteSheet sheet = EasyExcel.writerSheet(sheetName).head(headers).build();
        writerBuilder.build().write(mappedData, sheet).finish();
    }

    /**
     * 翻译数据（集成 TransManager）
     */
    private <T> void translateData(List<T> data) {
        try {
            if (transManager != null) {
                transManager.translate(data);
            }
        } catch (Exception e) {
            log.warn("字典翻译失败", e);
        }
    }

    /**
     * 解析列配置（注解 + 数据库）
     */
    private <T> List<ColumnMeta> resolveColumns(Class<T> clazz, ExcelExportConfig config) {
        Map<String, ExcelColumnConfig> dbConfigMap = new HashMap<>();
        if (config.getUseDbConfig() && excelConfigProvider != null && config.getConfigKey() != null) {
            List<ExcelColumnConfig> dbConfigs = excelConfigProvider.getColumnConfigs(config.getConfigKey());
            if (dbConfigs != null) {
                for (ExcelColumnConfig cfg : dbConfigs) {
                    dbConfigMap.put(cfg.getFieldName(), cfg);
                }
            }
        }

        List<ColumnMeta> metas = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            if (annotation != null && !annotation.export()) {
                continue; // 跳过不导出的字段
            }

            ColumnMeta meta = new ColumnMeta();
            meta.setFieldName(field.getName());

            // 优先使用数据库配置
            ExcelColumnConfig dbConfig = dbConfigMap.get(field.getName());
            if (dbConfig != null) {
                meta.setColumnName(dbConfig.getColumnName());
                meta.setWidth(dbConfig.getWidth());
                meta.setOrder(dbConfig.getOrderNum() != null ? dbConfig.getOrderNum() : Integer.MAX_VALUE);
                meta.setDateFormat(dbConfig.getDateFormat());
                meta.setNumberFormat(dbConfig.getNumberFormat());
                meta.setDictType(dbConfig.getDictType());
            } else if (annotation != null) {
                meta.setColumnName(annotation.value().isEmpty() ? field.getName() : annotation.value());
                meta.setWidth(annotation.width());
                meta.setOrder(annotation.order());
                meta.setDateFormat(annotation.dateFormat());
                meta.setNumberFormat(annotation.numberFormat());
                meta.setDictType(annotation.dictType());
            } else {
                // 无注解，使用字段名
                meta.setColumnName(field.getName());
                meta.setWidth(20);
                meta.setOrder(Integer.MAX_VALUE);
            }

            meta.setField(field);
            metas.add(meta);
        }

        // 按 order 排序
        metas.sort(Comparator.comparingInt(ColumnMeta::getOrder));
        return metas;
    }

    /**
     * 构建表头
     */
    private List<List<String>> buildHeaders(List<ColumnMeta> metas) {
        List<List<String>> headers = new ArrayList<>();
        for (ColumnMeta meta : metas) {
            headers.add(Collections.singletonList(meta.getColumnName()));
        }
        return headers;
    }

    /**
     * 映射数据到Map（EasyExcel动态列）
     */
    private <T> List<Map<String, Object>> mapData(List<T> data, List<ColumnMeta> metas) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (T obj : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (ColumnMeta meta : metas) {
                try {
                    meta.getField().setAccessible(true);
                    Object value = meta.getField().get(obj);
                    row.put(meta.getFieldName(), value);
                } catch (Exception e) {
                    log.warn("读取字段值失败: {}", meta.getFieldName(), e);
                    row.put(meta.getFieldName(), null);
                }
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 列元数据
     */
    private static class ColumnMeta {
        private String fieldName;
        private String columnName;
        private Integer width;
        private Integer order;
        private String dateFormat;
        private String numberFormat;
        private String dictType;
        private Field field;

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
        public String getNumberFormat() { return numberFormat; }
        public void setNumberFormat(String numberFormat) { this.numberFormat = numberFormat; }
        public String getDictType() { return dictType; }
        public void setDictType(String dictType) { this.dictType = dictType; }
        public Field getField() { return field; }
        public void setField(Field field) { this.field = field; }
    }
}
