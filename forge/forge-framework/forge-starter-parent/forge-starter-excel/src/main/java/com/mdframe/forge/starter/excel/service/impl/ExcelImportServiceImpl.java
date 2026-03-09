package com.mdframe.forge.starter.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.model.ExcelExportMetadata;
import com.mdframe.forge.starter.excel.model.GenericRowData;
import com.mdframe.forge.starter.excel.model.ImportErrorRecord;
import com.mdframe.forge.starter.excel.model.ImportResult;
import com.mdframe.forge.starter.excel.service.ExcelImportService;
import com.mdframe.forge.starter.excel.spi.ExcelConfigProvider;
import com.mdframe.forge.starter.excel.spi.ExcelMetadataProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            // 1. 加载元数据
            ExcelExportMetadata metadata = null;
            if (metadataProvider != null) {
                metadata = metadataProvider.getMetadata(configKey);
            }
            
            // 2. 加载列配置
            List<ExcelColumnConfig> columnConfigs = null;
            if (configProvider != null) {
                columnConfigs = configProvider.getColumnConfigs(configKey);
            }
            
            if (columnConfigs == null || columnConfigs.isEmpty()) {
                throw new RuntimeException("未找到导出配置：" + configKey);
            }
            
            // 3. 构建表头
            List<List<String>> headers = new ArrayList<>();
            List<Integer> widths = new ArrayList<>();
            
            for (ExcelColumnConfig config : columnConfigs) {
                if (Boolean.TRUE.equals(config.getImportable())) {
                    headers.add(Collections.singletonList(config.getColumnName()));
                    widths.add(config.getWidth() != null ? config.getWidth() : 20);
                }
            }
            
            // 4. 生成示例数据（可选）
            List<Map<String, Object>> sampleData = new ArrayList<>();
            if (metadata != null && Boolean.TRUE.equals(metadata.getIncludeSample())) {
                Map<String, Object> sampleRow = new LinkedHashMap<>();
                for (ExcelColumnConfig config : columnConfigs) {
                    if (Boolean.TRUE.equals(config.getImportable())) {
                        sampleRow.put(config.getColumnName(), config.getExampleValue() != null ? config.getExampleValue() : "");
                    }
                }
                sampleData.add(sampleRow);
            }
            
            // 5. 写入 Excel
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            EasyExcel.write(outputStream)
                    .head(headers)
                    .sheet("模板")
                    .doWrite(sampleData);
            
            log.info("生成导入模板：configKey={}", configKey);
            return outputStream.toByteArray();
            
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
            // 加载列配置用于校验
            Map<String, ExcelColumnConfig> columnConfigMap = new HashMap<>();
            if (configProvider != null) {
                List<ExcelColumnConfig> configs = configProvider.getColumnConfigs(configKey);
                if (configs != null) {
                    for (ExcelColumnConfig config : configs) {
                        columnConfigMap.put(config.getColumnName(), config);
                    }
                }
            }
            
            // 解析 Excel
            // 特殊处理 GenericRowData 类型，使用 Map 模式读取
            if (clazz == GenericRowData.class) {
                // 使用 EasyExcel 的 Map 读取模式
                @SuppressWarnings("unchecked")
                ImportResult<GenericRowData> genericResult = (ImportResult<GenericRowData>) result;
                GenericRowDataListener listener = new GenericRowDataListener(columnConfigMap, genericResult);
                EasyExcel.read(inputStream, null, listener).sheet().doRead();
                result.setTotalRows(listener.getRowCount());
            } else {
                // 使用普通对象模式
                ImportListener<T> listener = new ImportListener<>(clazz, columnConfigMap, result);
                EasyExcel.read(inputStream, clazz, listener).sheet().doRead();
                result.setTotalRows(listener.getRowCount());
            }
            
            // 设置汇总信息
            result.setSuccessRows(result.getSuccessData().size());
            result.setFailedRows(result.getErrors().size());
            result.buildSummary();
            result.setSuccess(result.getErrors().isEmpty());
            
            log.info("导入完成：{}", result.getSummary());
            
        } catch (Exception e) {
            log.error("导入失败", e);
            result.setSuccess(false);
            result.setSummary("导入失败：" + e.getMessage());
        }
        
        return result;
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
     * Excel 导入监听器
     */
    private static class ImportListener<T> extends AnalysisEventListener<T> {
        
        private final Class<T> clazz;
        private final Map<String, ExcelColumnConfig> columnConfigMap;
        private final ImportResult<T> result;
        private int rowCount = 0;
        
        public ImportListener(Class<T> clazz, Map<String, ExcelColumnConfig> columnConfigMap, ImportResult<T> result) {
            this.clazz = clazz;
            this.columnConfigMap = columnConfigMap;
            this.result = result;
        }
        
        @Override
        public void invoke(T data, AnalysisContext context) {
            rowCount++;
            // 默认全部成功，具体业务校验可在子类扩展
            result.getSuccessData().add(data);
        }
        
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            log.info("所有数据解析完成，共{}行", rowCount);
        }
        
        @Override
        public void onException(Exception exception, AnalysisContext context) throws Exception {
            // 记录错误
            ImportErrorRecord error = new ImportErrorRecord();
            error.setRowNum(context.readRowHolder().getRowIndex() + 1); // 行号从 1 开始
            error.setErrorType("解析错误");
            error.setErrorMessage(exception.getMessage());
            result.addError(error);
            
            // 不抛出异常，继续处理下一行
            log.warn("第{}行解析失败：{}", error.getRowNum(), exception.getMessage());
        }
        
        public int getRowCount() {
            return rowCount;
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
