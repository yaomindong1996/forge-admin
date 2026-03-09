package com.mdframe.forge.starter.excel.service.impl;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.model.GenericRowData;
import com.mdframe.forge.starter.excel.model.ImportErrorRecord;
import com.mdframe.forge.starter.excel.model.ImportResult;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GenericRowData 导入监听器
 * 用于处理动态列的 Excel 导入
 */
@Slf4j
public class GenericRowDataListener extends AnalysisEventListener<GenericRowData> {

    private final Map<String, ExcelColumnConfig> columnConfigMap;
    private final ImportResult<GenericRowData> result;
    private int rowCount = 0;

    public GenericRowDataListener(Map<String, ExcelColumnConfig> columnConfigMap, ImportResult<GenericRowData> result) {
        this.columnConfigMap = columnConfigMap;
        this.result = result;
    }

    @Override
    public void invoke(GenericRowData data, AnalysisContext context) {
        rowCount++;
        
        try {
            // 数据校验
            boolean hasError = false;
            
            // 遍历列配置进行校验
            for (Map.Entry<String, ExcelColumnConfig> entry : columnConfigMap.entrySet()) {
                ExcelColumnConfig config = entry.getValue();
                String columnName = config.getColumnName();
                Object value = data.getField(columnName);
                
                // 必填校验
                if (Boolean.TRUE.equals(config.getRequired()) && (value == null || "".equals(value.toString().trim()))) {
                    hasError = true;
                    ImportErrorRecord error = new ImportErrorRecord();
                    error.setRowNum(rowCount);
                    error.setColumnName(columnName);
                    error.setErrorType("必填校验");
                    error.setErrorMessage(config.getColumnName() + "不能为空");
                    error.setSuggestion("请输入" + config.getColumnName());
                    result.addError(error);
                }
                
                // 格式校验（正则表达式）
                if (value != null && config.getValidationRule() != null && !config.getValidationRule().isEmpty()) {
                    String stringValue = value.toString();
                    if (!stringValue.matches(config.getValidationRule())) {
                        hasError = true;
                        ImportErrorRecord error = new ImportErrorRecord();
                        error.setRowNum(rowCount);
                        error.setColumnName(columnName);
                        error.setErrorType("格式错误");
                        error.setErrorMessage(config.getValidationMessage() != null ? config.getValidationMessage() : config.getColumnName() + "格式不正确");
                        error.setSuggestion("请按照正确格式输入");
                        result.addError(error);
                    }
                }
            }
            
            // 如果没有错误，添加到成功列表
            if (!hasError) {
                result.getSuccessData().add(data);
            }
            
        } catch (Exception e) {
            log.warn("第{}行数据校验失败：{}", rowCount, e.getMessage());
            ImportErrorRecord error = new ImportErrorRecord();
            error.setRowNum(rowCount);
            error.setErrorType("校验错误");
            error.setErrorMessage(e.getMessage());
            result.addError(error);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("所有数据解析完成，共{}行", rowCount);
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        // 记录错误
        ImportErrorRecord error = new ImportErrorRecord();
        int rowNum = context.readRowHolder() != null ? context.readRowHolder().getRowIndex() + 1 : rowCount + 1;
        error.setRowNum(rowNum);
        
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException convertEx = (ExcelDataConvertException) exception;
            error.setColumnName("第" + (convertEx.getColumnIndex() + 1) + "列");
            error.setErrorType("类型转换错误");
            error.setErrorMessage("单元格数据类型错误");
            error.setSuggestion("请检查单元格数据类型");
        } else {
            error.setErrorType("解析错误");
            error.setErrorMessage(exception.getMessage());
        }
        
        result.addError(error);
        
        // 不抛出异常，继续处理下一行
        log.warn("第{}行解析失败：{}", error.getRowNum(), exception.getMessage());
    }

    public int getRowCount() {
        return rowCount;
    }
}
