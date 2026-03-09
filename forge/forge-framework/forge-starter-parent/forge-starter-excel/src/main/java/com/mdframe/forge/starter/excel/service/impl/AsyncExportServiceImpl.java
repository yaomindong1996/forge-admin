package com.mdframe.forge.starter.excel.service.impl;

import com.mdframe.forge.starter.excel.core.DynamicExportEngine;
import com.mdframe.forge.starter.excel.model.AsyncExportTask;
import com.mdframe.forge.starter.excel.service.AsyncExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步导出服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncExportServiceImpl implements AsyncExportService {

    private final DynamicExportEngine dynamicExportEngine;
    
    /**
     * 任务存储（生产环境建议用 Redis）
     */
    private final Map<String, AsyncExportTask> taskStore = new ConcurrentHashMap<>();
    
    /**
     * 临时文件目录
     */
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/forge-excel-export/";
    
    static {
        // 创建临时目录
        try {
            Files.createDirectories(Paths.get(TEMP_DIR));
        } catch (IOException e) {
            log.warn("创建临时目录失败", e);
        }
    }

    @Override
    public String submitExportTask(String configKey, Map<String, Object> queryParams, String fileName) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        
        AsyncExportTask task = new AsyncExportTask();
        task.setTaskId(taskId);
        task.setConfigKey(configKey);
        task.setFileName(fileName != null ? fileName : configKey + "_" + System.currentTimeMillis() + ".xlsx");
        task.setStatus(0); // 处理中
        task.setCreateTime(LocalDateTime.now());
        task.setExpireTime(LocalDateTime.now().plusHours(24)); // 24 小时后过期
        
        taskStore.put(taskId, task);
        
        // 异步执行导出
        executeExportAsync(taskId, configKey, queryParams);
        
        log.info("提交异步导出任务：taskId={}, configKey={}", taskId, configKey);
        return taskId;
    }

    @Async
    protected void executeExportAsync(String taskId, String configKey, Map<String, Object> queryParams) {
        AsyncExportTask task = taskStore.get(taskId);
        if (task == null) {
            return;
        }
        
        try {
            // 使用 ByteArrayOutputStream 捕获输出
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // 创建包装的 HttpServletResponse 来捕获输出
            MockHttpServletResponse mockResponse = new MockHttpServletResponse();
            mockResponse.setOutputStream(outputStream);
            
            // 执行导出
            dynamicExportEngine.export(mockResponse, configKey, queryParams);
            
            // 写入文件
            String filePath = TEMP_DIR + taskId + ".xlsx";
            File file = new File(filePath);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                outputStream.writeTo(fos);
            }
            
            // 更新任务状态
            task.setStatus(1); // 完成
            task.setFilePath(filePath);
            task.setFileSize(file.length());
            task.setFinishTime(LocalDateTime.now());
            
            log.info("异步导出完成：taskId={}, filePath={}, size={}", taskId, filePath, file.length());
            
        } catch (Exception e) {
            log.error("异步导出失败：taskId={}", taskId, e);
            task.setStatus(2); // 失败
            task.setErrorMessage(e.getMessage());
            task.setFinishTime(LocalDateTime.now());
        }
    }

    @Override
    public AsyncExportTask getTaskStatus(String taskId) {
        return taskStore.get(taskId);
    }

    @Override
    public byte[] downloadFile(String taskId) {
        AsyncExportTask task = taskStore.get(taskId);
        if (task == null || task.getStatus() != 1 || task.getFilePath() == null) {
            throw new RuntimeException("任务不存在或文件未生成");
        }
        
        try {
            Path path = Paths.get(task.getFilePath());
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败", e);
        }
    }

    @Override
    public void cleanupExpiredTasks() {
        LocalDateTime now = LocalDateTime.now();
        taskStore.entrySet().removeIf(entry -> {
            AsyncExportTask task = entry.getValue();
            if (task.getExpireTime() != null && task.getExpireTime().isBefore(now)) {
                // 删除文件
                if (task.getFilePath() != null) {
                    try {
                        Files.deleteIfExists(Paths.get(task.getFilePath()));
                    } catch (IOException e) {
                        log.warn("删除过期文件失败：{}", task.getFilePath(), e);
                    }
                }
                log.info("清理过期任务：taskId={}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 模拟 HttpServletResponse 用于捕获输出
     */
    private static class MockHttpServletResponse {
        private ByteArrayOutputStream outputStream;
        
        public void setOutputStream(ByteArrayOutputStream outputStream) {
            this.outputStream = outputStream;
        }
        
        public ByteArrayOutputStream getOutputStream() {
            return outputStream;
        }
        
        public void setContentType(String contentType) {
            // NOOP
        }
        
        public void setCharacterEncoding(String encoding) {
            // NOOP
        }
        
        public void setHeader(String name, String value) {
            // NOOP
        }
    }
}
