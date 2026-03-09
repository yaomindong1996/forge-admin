package com.mdframe.forge.starter.excel.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 异步导出任务
 */
@Data
public class AsyncExportTask {
    
    /**
     * 任务 ID
     */
    private String taskId;
    
    /**
     * 配置键
     */
    private String configKey;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 任务状态：0-处理中，1-完成，2-失败
     */
    private Integer status;
    
    /**
     * 文件路径（完成后填充）
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 数据条数
     */
    private Integer dataCount;
    
    /**
     * 错误信息（失败时填充）
     */
    private String errorMessage;
    
    /**
     * 创建人 ID
     */
    private Long createBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime finishTime;
    
    /**
     * 过期时间（文件保留期限）
     */
    private LocalDateTime expireTime;
}
