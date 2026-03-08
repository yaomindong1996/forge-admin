package com.mdframe.forge.plugin.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.service.ISysJobLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务日志Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysJobLogServiceImpl extends ServiceImpl<SysJobLogMapper, SysJobLog> implements ISysJobLogService {
    
    @Override
    public Page<SysJobLog> selectLogPage(Page<SysJobLog> page, SysJobLog query) {
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(query.getJobName()), SysJobLog::getJobName, query.getJobName())
                .eq(StringUtils.isNotBlank(query.getJobGroup()), SysJobLog::getJobGroup, query.getJobGroup())
                .eq(query.getStatus() != null, SysJobLog::getStatus, query.getStatus())
                .orderByDesc(SysJobLog::getTriggerTime);
        return this.page(page, wrapper);
    }
    
    @Override
    public int cleanLog(int days) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysJobLog::getTriggerTime, beforeDate);
        return this.baseMapper.delete(wrapper);
    }
}
