package com.mdframe.forge.starter.excel.service;

import com.mdframe.forge.starter.excel.model.AsyncExportTask;
import java.util.Map;

/**
 * 异步导出服务接口
 */
public interface AsyncExportService {
    
    /**
     * 提交异步导出任务
     *
     * @param configKey   配置键
     * @param queryParams 查询参数
     * @param fileName    文件名
     * @return 任务 ID
     */
    String submitExportTask(String configKey, Map<String, Object> queryParams, String fileName);
    
    /**
     * 查询任务状态
     *
     * @param taskId 任务 ID
     * @return 任务信息
     */
    AsyncExportTask getTaskStatus(String taskId);
    
    /**
     * 下载导出文件
     *
     * @param taskId 任务 ID
     * @return 文件字节数组
     */
    byte[] downloadFile(String taskId);
    
    /**
     * 清理过期任务
     */
    void cleanupExpiredTasks();
}
