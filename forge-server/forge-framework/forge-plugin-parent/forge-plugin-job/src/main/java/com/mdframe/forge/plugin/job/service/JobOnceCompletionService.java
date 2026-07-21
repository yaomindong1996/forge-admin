package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 一次性任务计划执行后的完成态写入。
 */
@Service
@RequiredArgsConstructor
public class JobOnceCompletionService {

    private final SysJobConfigMapper jobConfigMapper;

    public boolean markCompleted(Long jobConfigId, LocalDateTime plannedFireTime, String timezone) {
        if (jobConfigId == null || plannedFireTime == null || timezone == null) {
            return false;
        }
        return jobConfigMapper.markOnceCompleted(jobConfigId, plannedFireTime, timezone) > 0;
    }
}
