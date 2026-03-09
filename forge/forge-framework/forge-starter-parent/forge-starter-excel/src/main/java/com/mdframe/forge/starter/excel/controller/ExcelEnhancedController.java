package com.mdframe.forge.starter.excel.controller;

import com.mdframe.forge.starter.excel.model.AsyncExportTask;
import com.mdframe.forge.starter.excel.model.ImportResult;
import com.mdframe.forge.starter.excel.service.AsyncExportService;
import com.mdframe.forge.starter.excel.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Excel 导入导出增强接口
 */
@Slf4j
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelEnhancedController {

    private final ExcelImportService importService;
    private final AsyncExportService exportService;

    // ==================== 导入相关接口 ====================

    /**
     * 下载导入模板
     */
    @GetMapping("/template/{configKey}")
    public void downloadTemplate(@PathVariable String configKey, HttpServletResponse response) {
        try {
            byte[] template = importService.downloadTemplate(configKey);
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode(configKey + "_模板.xlsx", StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
            response.getOutputStream().write(template);
            
            log.info("下载导入模板：configKey={}", configKey);
        } catch (Exception e) {
            log.error("下载模板失败", e);
            throw new RuntimeException("下载模板失败：" + e.getMessage(), e);
        }
    }

    /**
     * 导入 Excel 数据
     */
    @PostMapping("/import/{configKey}")
    public ResponseEntity<ImportResult<?>> importData(
            @PathVariable String configKey,
            @RequestParam("file") MultipartFile file) {
        
        log.info("导入 Excel 数据：configKey={}, fileName={}", configKey, file.getOriginalFilename());
        
        try {
            // 使用 Map 作为通用类型，具体业务校验由调用方处理
            ImportResult<Map<String, Object>> result = importService.importData(
                    file, configKey, Map.class);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("导入失败", e);
            ImportResult<?> errorResult = new ImportResult<>();
            errorResult.setSuccess(false);
            errorResult.setSummary("导入失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 下载导入错误报告
     */
    @GetMapping("/error-report/{taskId}")
    public void downloadErrorReport(@PathVariable String taskId, HttpServletResponse response) {
        try {
            byte[] report = importService.downloadErrorReport(taskId);
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("导入错误报告_" + taskId + ".xlsx", StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
            response.getOutputStream().write(report);
            
            log.info("下载错误报告：taskId={}", taskId);
        } catch (Exception e) {
            log.error("下载错误报告失败", e);
            throw new RuntimeException("下载错误报告失败：" + e.getMessage(), e);
        }
    }

    // ==================== 异步导出相关接口 ====================

    /**
     * 提交异步导出任务
     */
    @PostMapping("/async-export/{configKey}")
    public ResponseEntity<Map<String, Object>> submitAsyncExport(
            @PathVariable String configKey,
            @RequestBody(required = false) Map<String, Object> queryParams) {
        
        log.info("提交异步导出任务：configKey={}", configKey);
        
        try {
            String fileName = queryParams != null ? (String) queryParams.remove("fileName") : null;
            String taskId = exportService.submitExportTask(configKey, queryParams, fileName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("message", "导出任务已提交，请稍后查询状态");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("提交异步导出任务失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "提交任务失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 查询异步导出任务状态
     */
    @GetMapping("/async-export/status/{taskId}")
    public ResponseEntity<AsyncExportTask> getExportTaskStatus(@PathVariable String taskId) {
        AsyncExportTask task = exportService.getTaskStatus(taskId);
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(task);
    }

    /**
     * 下载异步导出文件
     */
    @GetMapping("/async-export/download/{taskId}")
    public void downloadAsyncExportFile(@PathVariable String taskId, HttpServletResponse response) {
        try {
            byte[] fileContent = exportService.downloadFile(taskId);
            
            AsyncExportTask task = exportService.getTaskStatus(taskId);
            String fileName = task != null && task.getFileName() != null
                    ? task.getFileName() : "export_" + taskId + ".xlsx";
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
            response.getOutputStream().write(fileContent);
            
            log.info("下载异步导出文件：taskId={}", taskId);
        } catch (Exception e) {
            log.error("下载导出文件失败", e);
            throw new RuntimeException("下载文件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 轮询导出状态并自动下载（前端友好）
     */
    @GetMapping("/async-export/{taskId}")
    public ResponseEntity<Map<String, Object>> getExportResult(@PathVariable String taskId) {
        AsyncExportTask task = exportService.getTaskStatus(taskId);
        
        if (task == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "任务不存在");
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("taskId", task.getTaskId());
        response.put("status", task.getStatus());
        response.put("statusText", getStatusText(task.getStatus()));
        response.put("fileName", task.getFileName());
        response.put("fileSize", task.getFileSize());
        response.put("dataCount", task.getDataCount());
        response.put("createTime", task.getCreateTime());
        response.put("finishTime", task.getFinishTime());
        
        if (task.getStatus() == 2) {
            response.put("errorMessage", task.getErrorMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "处理中";
            case 1: return "完成";
            case 2: return "失败";
            default: return "未知";
        }
    }
}
