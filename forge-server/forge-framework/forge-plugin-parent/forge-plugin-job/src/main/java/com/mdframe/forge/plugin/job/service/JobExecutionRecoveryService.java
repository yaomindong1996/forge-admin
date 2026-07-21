package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 启动时终结已经失去心跳或从未开始的执行记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionRecoveryService {

    public static final String RECOVERY_REASON = "任务执行节点中断，启动恢复已终结该记录";

    private final SysJobLogMapper jobLogMapper;
    private final JobProperties jobProperties;

    public int recoverStaleExecutions() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minus(jobProperties.validatedExecutionRecoveryTimeout());
        int recovered = jobLogMapper.failStaleExecutions(cutoff, RECOVERY_REASON);
        if (recovered > 0) {
            log.warn("启动恢复已终结失联任务执行记录: count={}", recovered);
        }
        return recovered;
    }
}

