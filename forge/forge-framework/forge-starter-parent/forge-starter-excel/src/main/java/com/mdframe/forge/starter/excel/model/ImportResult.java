package com.mdframe.forge.starter.excel.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果
 */
@Data
public class ImportResult<T> {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 总行数
     */
    private Integer totalRows;
    
    /**
     * 成功行数
     */
    private Integer successRows;
    
    /**
     * 失败行数
     */
    private Integer failedRows;
    
    /**
     * 成功导入的数据
     */
    private List<T> successData = new ArrayList<>();
    
    /**
     * 错误记录列表
     */
    private List<ImportErrorRecord> errors = new ArrayList<>();
    
    /**
     * 错误报告文件路径（如果有）
     */
    private String errorReportPath;
    
    /**
     * 汇总信息
     */
    private String summary;
    
    /**
     * 添加错误记录
     */
    public void addError(ImportErrorRecord error) {
        this.errors.add(error);
        this.failedRows = this.errors.size();
    }
    
    /**
     * 构建汇总信息
     */
    public void buildSummary() {
        this.summary = String.format("共%d行，成功%d行，失败%d行", 
                totalRows, successRows, failedRows);
    }
}
