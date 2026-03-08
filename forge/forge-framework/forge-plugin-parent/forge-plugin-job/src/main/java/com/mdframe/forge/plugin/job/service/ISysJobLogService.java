package com.mdframe.forge.plugin.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.job.entity.SysJobLog;

/**
 * 任务日志Service
 */
public interface ISysJobLogService extends IService<SysJobLog> {
    
    /**
     * 分页查询日志
     */
    Page<SysJobLog> selectLogPage(Page<SysJobLog> page, SysJobLog query);
    
    /**
     * 清理日志
     * @param days 保留最近N天的日志
     */
    int cleanLog(int days);
}
