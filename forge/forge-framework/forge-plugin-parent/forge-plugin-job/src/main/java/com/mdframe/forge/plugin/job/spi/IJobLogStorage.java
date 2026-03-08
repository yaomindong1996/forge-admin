package com.mdframe.forge.plugin.job.spi;

import com.mdframe.forge.plugin.job.model.JobLog;

/**
 * 日志存储SPI
 * 支持扩展到ES/MongoDB等
 */
public interface IJobLogStorage {
    
    /**
     * 保存日志
     */
    void saveLog(JobLog log);
    
    /**
     * 查询日志
     */
    JobLog getLog(Long logId);
}
