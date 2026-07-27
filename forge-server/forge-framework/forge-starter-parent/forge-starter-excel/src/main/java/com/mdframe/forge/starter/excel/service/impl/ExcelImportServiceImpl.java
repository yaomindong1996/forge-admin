package com.mdframe.forge.starter.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.mdframe.forge.starter.excel.core.ExcelImportTemplateWriter;
import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.model.ExcelExportMetadata;
import com.mdframe.forge.starter.excel.model.GenericRowData;
import com.mdframe.forge.starter.excel.model.ImportErrorRecord;
import com.mdframe.forge.starter.excel.model.ImportResult;
import com.mdframe.forge.starter.excel.model.ImportTemplateColumn;
import com.mdframe.forge.starter.excel.service.ExcelImportService;
import com.mdframe.forge.starter.excel.spi.ExcelConfigProvider;
import com.mdframe.forge.starter.excel.spi.ExcelMetadataProvider;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Excel 导入服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    @Autowired(required = false)
    private ExcelMetadataProvider metadataProvider;

    @Autowired(required = false)
    private ExcelConfigProvider configProvider;

    @Autowired(required = false)
    private DictValueProvider dictValueProvider;
    
    /**
     * 临时文件目录
     */
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/forge-excel-import/";
    
    static {
        try {
            Files.createDirectories(Paths.get(TEMP_DIR));
        } catch (IOException e) {
            log.warn("创建临时目录失败", e);
        }
    }

    @Override
    public byte[] downloadTemplate(String configKey) {
        try {
            ExcelExportMetadata metadata = loadMetadata(configKey);
            if (!isImportAllowed(metadata)) {
                throw new RuntimeException("当前配置未开启导入: " + configKey);
            }

            List<ExcelColumnConfig> columnConfigs = loadImportableColumnConfigs(configKey);
            if (columnConfigs.isEmpty()) {
                throw new RuntimeException("未找到导入列配置：" + configKey);
            }
            List<ImportTemplateColumn> templateColumns = columnConfigs.stream()
                    .map(this::toImportTemplateColumn)
                    .toList();
            byte[] workbook = ExcelImportTemplateWriter.write("导入数据", templateColumns);

            log.info("生成导入模板：configKey={}", configKey);
            return workbook;
            
        } catch (Exception e) {
            log.error("生成导入模板失败：{}", configKey, e);
            throw new RuntimeException("生成模板失败：" + e.getMessage(), e);
        }
    }

    @Override
    public <T> ImportResult<T> importData(MultipartFile file, String configKey, Class<T> clazz) {
        try (InputStream inputStream = file.getInputStream()) {
            return importData(inputStream, configKey, clazz);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败", e);
        }
    }

    @Override
    public <T> ImportResult<T> importData(InputStream inputStream, String configKey, Class<T> clazz) {
        ImportResult<T> result = new ImportResult<>();
        
        try {
            ExcelExportMetadata metadata = loadMetadata(configKey);
            if (!isImportAllowed(metadata)) {
                throw new RuntimeException("当前配置未开启导入: " + configKey);
            }

            List<ExcelColumnConfig> columnConfigs = loadImportableColumnConfigs(configKey);
            if (columnConfigs.isEmpty()) {
                throw new RuntimeException("未配置可导入列: " + configKey);
            }

            ImportResult<GenericRowData> genericResult = new ImportResult<>();
            GenericRowDataListener listener = new GenericRowDataListener(columnConfigs, genericResult, dictValueProvider);
            EasyExcel.read(inputStream, listener).sheet().doRead();
            genericResult.setTotalRows(listener.getRowCount());
            genericResult.setSuccessRows(genericResult.getSuccessData().size());
            genericResult.setFailedRows(genericResult.getErrors().size());
            genericResult.buildSummary();
            genericResult.setSuccess(genericResult.getErrors().isEmpty());

            if (clazz == GenericRowData.class) {
                @SuppressWarnings("unchecked")
                ImportResult<T> castResult = (ImportResult<T>) genericResult;
                return castResult;
            }

            result.setTotalRows(listener.getRowCount());
            mergeErrors(result, genericResult.getErrors());
            Map<String, ExcelColumnConfig> fieldConfigMap = buildFieldConfigMap(columnConfigs);
            mapGenericRowsToPojo(clazz, genericResult.getSuccessData(), fieldConfigMap, result);
            
            // 设置汇总信息
            result.setSuccessRows(result.getSuccessData().size());
            result.setFailedRows(result.getErrors().size());
            result.buildSummary();
            result.setSuccess(result.getErrors().isEmpty());
            
            log.info("导入完成：{}", result.getSummary());
            
        } catch (Exception e) {
            log.error("导入失败", e);
            result.setSuccess(false);
            result.setSummary(ImportResult.PUBLIC_FAILURE_MESSAGE);
        }
        
        return result;
    }

    private ExcelExportMetadata loadMetadata(String configKey) {
        return metadataProvider != null ? metadataProvider.getMetadata(configKey) : null;
    }

    private List<ExcelColumnConfig> loadImportableColumnConfigs(String configKey) {
        if (configProvider == null) {
            return List.of();
        }
        List<ExcelColumnConfig> configs = configProvider.getColumnConfigs(configKey);
        if (configs == null || configs.isEmpty()) {
            return List.of();
        }
        return configs.stream()
                .filter(Objects::nonNull)
                .filter(config -> !Boolean.FALSE.equals(config.getImportable()))
                .sorted(Comparator.comparingInt(config -> config.getOrderNum() != null ? config.getOrderNum() : Integer.MAX_VALUE))
                .toList();
    }

    private boolean isImportAllowed(ExcelExportMetadata metadata) {
        if (metadata == null) {
            return true;
        }
        if (Integer.valueOf(0).equals(metadata.getStatus())) {
            return false;
        }
        if ("EXPORT".equalsIgnoreCase(metadata.getConfigType())) {
            return false;
        }
        return metadata.getAllowImport() == null || Boolean.TRUE.equals(metadata.getAllowImport());
    }

    private ImportTemplateColumn toImportTemplateColumn(ExcelColumnConfig config) {
        return new ImportTemplateColumn(
                config.getFieldName(),
                config.getColumnName(),
                Boolean.TRUE.equals(config.getRequired()),
                resolveTemplateExample(config),
                resolveTemplateDescription(config)
        );
    }

    private String resolveTemplateExample(ExcelColumnConfig config) {
        if (config.getExampleValue() != null && !config.getExampleValue().isBlank()) {
            return config.getExampleValue();
        }
        if (config.getDictType() != null && !config.getDictType().isBlank()) {
            return "字典选项示例";
        }
        if (config.getDateFormat() != null && !config.getDateFormat().isBlank()) {
            return config.getDateFormat().contains("HH") ? "2026-07-15 09:30:00" : "2026-07-15";
        }
        if (config.getNumberFormat() != null && !config.getNumberFormat().isBlank()) {
            return "100";
        }
        return normalizeTemplateText(config.getColumnName(), "示例值") + "示例";
    }

    private String resolveTemplateDescription(ExcelColumnConfig config) {
        List<String> descriptions = new ArrayList<>();
        if (config.getDictType() != null && !config.getDictType().isBlank()) {
            descriptions.add("填写字典标签或字典值，字典类型：" + config.getDictType());
        }
        if (config.getDateFormat() != null && !config.getDateFormat().isBlank()) {
            descriptions.add("日期格式：" + config.getDateFormat());
        }
        if (config.getNumberFormat() != null && !config.getNumberFormat().isBlank()) {
            descriptions.add("数字格式：" + config.getNumberFormat());
        }
        if (config.getValidationMessage() != null && !config.getValidationMessage().isBlank()) {
            descriptions.add(config.getValidationMessage());
        } else if (config.getValidationRule() != null && !config.getValidationRule().isBlank()) {
            descriptions.add("需符合字段校验规则");
        }
        if (descriptions.isEmpty()) {
            descriptions.add("按" + normalizeTemplateText(config.getColumnName(), config.getFieldName()) + "的业务含义填写");
        }
        return String.join("；", descriptions);
    }

    private String normalizeTemplateText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private <T> void mapGenericRowsToPojo(Class<T> clazz,
                                          List<GenericRowData> rows,
                                          Map<String, ExcelColumnConfig> fieldConfigMap,
                                          ImportResult<T> result) {
        for (GenericRowData row : rows) {
            boolean hasError = false;
            try {
                T instance = newInstance(clazz);
                for (Map.Entry<String, Object> entry : row.getFields().entrySet()) {
                    ExcelColumnConfig config = fieldConfigMap.get(entry.getKey());
                    try {
                        setProperty(instance, entry.getKey(), entry.getValue());
                    } catch (Exception ex) {
                        log.warn("导入第{}行字段映射失败：field={}", row.getRowNum(), entry.getKey(), ex);
                        hasError = true;
                        result.addError(buildPojoFieldError(row.getRowNum(), config, entry.getValue()));
                    }
                }
                if (!hasError) {
                    result.getSuccessData().add(instance);
                }
            } catch (Exception e) {
                log.warn("导入第{}行对象映射失败", row.getRowNum(), e);
                result.addError(buildPojoError(row.getRowNum()));
            }
        }
    }

    private void mergeErrors(ImportResult<?> target, List<ImportErrorRecord> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        for (ImportErrorRecord error : errors) {
            target.addError(error);
        }
    }

    private ImportErrorRecord buildPojoError(Integer rowNum) {
        ImportErrorRecord error = new ImportErrorRecord();
        error.setRowNum(rowNum);
        error.setErrorType("对象映射错误");
        error.setErrorMessage("数据无法映射到目标对象");
        error.setSuggestion("请检查导入字段与目标对象字段类型是否一致");
        return error;
    }

    private ImportErrorRecord buildPojoFieldError(Integer rowNum,
                                                  ExcelColumnConfig config,
                                                  Object rawValue) {
        ImportErrorRecord error = new ImportErrorRecord();
        error.setRowNum(rowNum);
        error.setColumnName(config != null ? config.getColumnName() : null);
        error.setRawValue(rawValue != null ? String.valueOf(rawValue) : null);
        error.setErrorType("对象映射错误");
        error.setErrorMessage("字段值与目标类型不匹配");
        error.setSuggestion("请检查导入字段与目标对象字段类型是否一致");
        return error;
    }

    private Map<String, ExcelColumnConfig> buildFieldConfigMap(List<ExcelColumnConfig> columnConfigs) {
        Map<String, ExcelColumnConfig> map = new HashMap<>();
        for (ExcelColumnConfig config : columnConfigs) {
            if (config != null && config.getFieldName() != null) {
                map.put(config.getFieldName(), config);
            }
        }
        return map;
    }

    private <T> T newInstance(Class<T> clazz) throws Exception {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void setProperty(Object target, String fieldName, Object value) throws Exception {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return;
        }
        Method setter = findSetter(target.getClass(), fieldName);
        if (setter != null) {
            Object convertedValue = convertValue(value, setter.getParameterTypes()[0]);
            setter.invoke(target, convertedValue);
            return;
        }
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            throw new RuntimeException("目标对象不存在字段: " + fieldName);
        }
        field.setAccessible(true);
        field.set(target, convertValue(value, field.getType()));
    }

    private Method findSetter(Class<?> clazz, String fieldName) {
        String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }

        if (targetType == String.class) {
            return text;
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(text);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(normalizeNumberText(text));
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(normalizeNumberText(text));
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.valueOf(normalizeNumberText(text));
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.valueOf(normalizeNumberText(text));
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.valueOf(normalizeNumberText(text));
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.valueOf(normalizeNumberText(text));
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return parseBoolean(text);
        }
        if (targetType == LocalDateTime.class) {
            return parseLocalDateTime(text);
        }
        if (targetType == LocalDate.class) {
            return parseLocalDate(text);
        }
        if (targetType == LocalTime.class) {
            return parseLocalTime(text);
        }
        return value;
    }

    private Boolean parseBoolean(String text) {
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return Set.of("1", "true", "y", "yes", "是").contains(normalized);
    }

    private String normalizeNumberText(String text) {
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private LocalDateTime parseLocalDateTime(String text) {
        String normalized = text.replace("/", "-").replace("T", " ");
        if (normalized.length() == 10) {
            normalized = normalized + " 00:00:00";
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(text);
            } catch (DateTimeParseException ex) {
                throw new RuntimeException("无法转换为LocalDateTime: " + text);
            }
        }
    }

    private LocalDate parseLocalDate(String text) {
        String normalized = text.replace("/", "-");
        if (normalized.length() > 10) {
            normalized = normalized.substring(0, 10);
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new RuntimeException("无法转换为LocalDate: " + text);
        }
    }

    private LocalTime parseLocalTime(String text) {
        try {
            return LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(text);
            } catch (DateTimeParseException ex) {
                throw new RuntimeException("无法转换为LocalTime: " + text);
            }
        }
    }

    @Override
    public byte[] downloadErrorReport(String taskId) {
        // 从临时目录读取错误报告
        try {
            Path path = Paths.get(TEMP_DIR + taskId + "_error.xlsx");
            if (!Files.exists(path)) {
                throw new RuntimeException("错误报告不存在");
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("读取错误报告失败", e);
        }
    }
    
    /**
     * 生成错误报告 Excel
     */
    public String generateErrorReport(String taskId, List<ImportErrorRecord> errors) {
        try {
            String filePath = TEMP_DIR + taskId + "_error.xlsx";
            
            // 构建表头
            List<List<String>> headers = Arrays.asList(
                    Arrays.asList("行号"),
                    Arrays.asList("列名"),
                    Arrays.asList("原始值"),
                    Arrays.asList("错误类型"),
                    Arrays.asList("错误信息"),
                    Arrays.asList("建议修正")
            );
            
            // 构建数据
            List<List<String>> data = new ArrayList<>();
            for (ImportErrorRecord error : errors) {
                data.add(Arrays.asList(
                        String.valueOf(error.getRowNum()),
                        error.getColumnName() != null ? error.getColumnName() : "",
                        error.getRawValue() != null ? error.getRawValue() : "",
                        error.getErrorType() != null ? error.getErrorType() : "",
                        error.getErrorMessage() != null ? error.getErrorMessage() : "",
                        error.getSuggestion() != null ? error.getSuggestion() : ""
                ));
            }
            
            // 写入文件
            EasyExcel.write(filePath)
                    .head(headers)
                    .sheet("导入错误报告")
                    .doWrite(data);
            
            log.info("生成错误报告：taskId={}, errorCount={}", taskId, errors.size());
            return filePath;
            
        } catch (Exception e) {
            log.error("生成错误报告失败", e);
            return null;
        }
    }
    
    /**
     * 导入错误数据类
     */
    @Data
    public static class ErrorDataRow {
        private Integer rowNum;
        private Map<String, String> data;
        private List<ImportErrorRecord> errors;
    }
}
