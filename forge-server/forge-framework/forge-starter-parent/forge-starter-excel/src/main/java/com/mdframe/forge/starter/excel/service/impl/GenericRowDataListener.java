package com.mdframe.forge.starter.excel.service.impl;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.model.GenericRowData;
import com.mdframe.forge.starter.excel.model.ImportErrorRecord;
import com.mdframe.forge.starter.excel.model.ImportResult;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GenericRowData 导入监听器
 * 按配置表头映射动态字段，并对字典列做反向翻译。
 */
@Slf4j
public class GenericRowDataListener extends AnalysisEventListener<Map<Integer, Object>> {

    private final List<ExcelColumnConfig> columnConfigs;
    private final ImportResult<GenericRowData> result;
    private final DictValueProvider dictValueProvider;
    private final Map<Integer, ExcelColumnConfig> headerMapping = new LinkedHashMap<>();
    private int rowCount = 0;

    public GenericRowDataListener(List<ExcelColumnConfig> columnConfigs,
                                  ImportResult<GenericRowData> result,
                                  DictValueProvider dictValueProvider) {
        this.columnConfigs = columnConfigs;
        this.result = result;
        this.dictValueProvider = dictValueProvider;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        headerMapping.clear();

        Map<String, ExcelColumnConfig> configByHeader = new LinkedHashMap<>();
        for (ExcelColumnConfig config : columnConfigs) {
            if (config == null || !Boolean.TRUE.equals(config.getImportable())) {
                continue;
            }
            if (config.getColumnName() != null) {
                configByHeader.put(normalizeHeader(config.getColumnName()), config);
            }
            if (config.getFieldName() != null) {
                configByHeader.putIfAbsent(normalizeHeader(config.getFieldName()), config);
            }
        }

        for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
            ExcelColumnConfig config = configByHeader.get(normalizeHeader(entry.getValue()));
            if (config != null) {
                headerMapping.put(entry.getKey(), config);
            }
        }

        if (headerMapping.isEmpty()) {
            result.addError(buildError(1, null, null, "模板错误",
                    "导入模板不匹配，未识别到可导入字段", "请下载最新导入模板"));
            return;
        }

        Set<String> presentFields = new LinkedHashSet<>();
        for (ExcelColumnConfig config : headerMapping.values()) {
            if (config.getFieldName() != null) {
                presentFields.add(config.getFieldName());
            }
        }
        for (ExcelColumnConfig config : columnConfigs) {
            if (config == null || !Boolean.TRUE.equals(config.getImportable())) {
                continue;
            }
            if (Boolean.TRUE.equals(config.getRequired()) && !presentFields.contains(config.getFieldName())) {
                result.addError(buildError(1, config.getColumnName(), null, "模板错误",
                        "导入模板缺少必填列: " + config.getColumnName(), "请下载最新导入模板"));
            }
        }
    }

    @Override
    public void invoke(Map<Integer, Object> rowData, AnalysisContext context) {
        rowCount++;
        int rowNum = context.readRowHolder() != null ? context.readRowHolder().getRowIndex() + 1 : rowCount + 1;
        GenericRowData data = new GenericRowData();
        data.setRowNum(rowNum);
        boolean hasError = false;

        for (Map.Entry<Integer, ExcelColumnConfig> entry : headerMapping.entrySet()) {
            ExcelColumnConfig config = entry.getValue();
            Object rawValue = rowData.get(entry.getKey());
            String textValue = toCellText(rawValue);

            if (textValue.isBlank()) {
                if (Boolean.TRUE.equals(config.getRequired())) {
                    hasError = true;
                    result.addError(buildError(rowNum, config.getColumnName(), null, "必填校验",
                            config.getColumnName() + "不能为空", "请输入" + config.getColumnName()));
                }
                continue;
            }

            if (config.getValidationRule() != null && !config.getValidationRule().isBlank()
                    && !textValue.matches(config.getValidationRule())) {
                hasError = true;
                result.addError(buildError(rowNum, config.getColumnName(), textValue, "格式错误",
                        config.getValidationMessage() != null && !config.getValidationMessage().isBlank()
                                ? config.getValidationMessage()
                                : config.getColumnName() + "格式不正确",
                        "请按照模板示例修正后重试"));
                continue;
            }

            Object convertedValue = textValue;
            if (config.getDictType() != null && !config.getDictType().isBlank()) {
                convertedValue = resolveDictValue(config, textValue);
                if (convertedValue == null) {
                    hasError = true;
                    result.addError(buildError(rowNum, config.getColumnName(), textValue, "字典转换错误",
                            "无法识别字典值: " + textValue, "请填写字典显示值或实际存储值"));
                    continue;
                }
            }

            data.setField(config.getFieldName(), convertedValue);
        }

        if (!hasError && !data.getFields().isEmpty()) {
            result.getSuccessData().add(data);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("所有数据解析完成，共{}行", rowCount);
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        ImportErrorRecord error = new ImportErrorRecord();
        int rowNum = context != null && context.readRowHolder() != null
                ? context.readRowHolder().getRowIndex() + 1 : rowCount + 1;
        error.setRowNum(rowNum);

        if (exception instanceof ExcelDataConvertException convertEx) {
            error.setColumnName("第" + (convertEx.getColumnIndex() + 1) + "列");
            error.setErrorType("类型转换错误");
            error.setErrorMessage("单元格数据类型错误");
            error.setSuggestion("请检查单元格数据类型");
        } else {
            error.setErrorType("解析错误");
            error.setErrorMessage("文件内容解析失败");
            error.setSuggestion("请检查文件格式或下载最新模板后重试");
        }

        result.addError(error);
        log.warn("第{}行解析失败", error.getRowNum(), exception);
    }

    public int getRowCount() {
        return rowCount;
    }

    private Object resolveDictValue(ExcelColumnConfig config, String textValue) {
        if (dictValueProvider == null) {
            return textValue;
        }
        try {
            return dictValueProvider.getValue(config.getDictType(), textValue);
        } catch (Exception e) {
            log.warn("字典反向翻译失败: dictType={}, value={}", config.getDictType(), textValue, e);
            return null;
        }
    }

    private ImportErrorRecord buildError(int rowNum,
                                         String columnName,
                                         String rawValue,
                                         String errorType,
                                         String errorMessage,
                                         String suggestion) {
        ImportErrorRecord error = new ImportErrorRecord();
        error.setRowNum(rowNum);
        error.setColumnName(columnName);
        error.setRawValue(rawValue);
        error.setErrorType(errorType);
        error.setErrorMessage(errorMessage);
        error.setSuggestion(suggestion);
        return error;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim();
    }

    private String toCellText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value).trim();
    }
}
