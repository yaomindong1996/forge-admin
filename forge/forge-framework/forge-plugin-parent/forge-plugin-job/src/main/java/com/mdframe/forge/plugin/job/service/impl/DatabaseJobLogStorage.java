package com.mdframe.forge.plugin.job.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.model.JobLog;
import com.mdframe.forge.plugin.job.spi.IJobLogStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 数据库日志存储实现
 */
@Slf4j
@Component
public class DatabaseJobLogStorage implements IJobLogStorage {
    
    @Autowired
    private SysJobLogMapper jobLogMapper;
    
    @Override
    public void saveLog(JobLog jobLog) {
        SysJobLog entity = new SysJobLog();
        BeanUtil.copyProperties(jobLog, entity);
        jobLogMapper.insert(entity);
        log.debug("保存任务日志到数据库: {}.{}", jobLog.getJobGroup(), jobLog.getJobName());
    }
    
    @Override
    public JobLog getLog(Long logId) {
        SysJobLog entity = jobLogMapper.selectById(logId);
        if (entity == null) {
            return null;
        }
        JobLog log = new JobLog();
        BeanUtil.copyProperties(entity, log);
        return log;
    }
}
