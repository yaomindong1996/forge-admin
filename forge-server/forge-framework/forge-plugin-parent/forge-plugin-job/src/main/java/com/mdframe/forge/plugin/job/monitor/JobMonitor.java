package com.mdframe.forge.plugin.job.monitor;

import com.mdframe.forge.plugin.job.model.JobLog;
import com.mdframe.forge.plugin.job.spi.IJobLogStorage;
import com.mdframe.forge.plugin.job.support.JobLogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 任务监控器
 * 负责日志记录和告警通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobMonitor {

    private final IJobLogStorage logStorage;

    private final JobLogSanitizer logSanitizer;
    
    /**
     * 记录执行日志
     */
    public void recordLog(Long jobConfigId, String triggerType, String jobName, String jobGroup,
                          String executorHandler, String jobParam, Date triggerTime,
                          LocalDateTime startTime, LocalDateTime endTime, Integer status,
                          String result, Exception exception) {
        
        JobLog jobLog = new JobLog();
        jobLog.setJobConfigId(jobConfigId);
        jobLog.setTriggerType(triggerType);
        jobLog.setJobName(jobName);
        jobLog.setJobGroup(jobGroup);
        jobLog.setExecutorHandler(executorHandler);
        jobLog.setJobParam(logSanitizer.sanitizeJobParam(jobParam));
        jobLog.setTriggerTime(convertToLocalDateTime(triggerTime));
        jobLog.setStartTime(startTime);
        jobLog.setEndTime(endTime);
        jobLog.setDuration(Duration.between(startTime, endTime).toMillis());
        jobLog.setStatus(status);
        jobLog.setResult(logSanitizer.sanitizeResult(result));
        
        if (exception != null) {
            jobLog.setExceptionMsg(logSanitizer.sanitizeException(getStackTrace(exception)));
        }
        
        // 保存日志
        try {
            logStorage.saveLog(jobLog);
        } catch (Exception e) {
            log.error("保存任务日志失败", e);
        }
    }
    
    /**
     * 获取异常堆栈
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    /**
     * Date转LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }
}
