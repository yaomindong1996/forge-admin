package com.mdframe.forge.plugin.job.monitor;

import com.mdframe.forge.plugin.job.model.JobLog;
import com.mdframe.forge.plugin.job.spi.IJobAlarmNotifier;
import com.mdframe.forge.plugin.job.spi.IJobLogStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 任务监控器
 * 负责日志记录和告警通知
 */
@Slf4j
@Component
public class JobMonitor {
    
    @Autowired
    private IJobLogStorage logStorage;
    
    @Autowired(required = false)
    private List<IJobAlarmNotifier> alarmNotifiers;
    
    /**
     * 记录执行日志
     */
    public void recordLog(String jobName, String jobGroup, String executorHandler,
                         String jobParam, Date triggerTime, LocalDateTime startTime,
                         LocalDateTime endTime, Integer status, String result, Exception exception) {
        
        JobLog jobLog = new JobLog();
        jobLog.setJobName(jobName);
        jobLog.setJobGroup(jobGroup);
        jobLog.setExecutorHandler(executorHandler);
        jobLog.setJobParam(jobParam);
        jobLog.setTriggerTime(convertToLocalDateTime(triggerTime));
        jobLog.setStartTime(startTime);
        jobLog.setEndTime(endTime);
        jobLog.setDuration(Duration.between(startTime, endTime).toMillis());
        jobLog.setStatus(status);
        jobLog.setResult(truncate(result, 2000));
        
        if (exception != null) {
            jobLog.setExceptionMsg(truncate(getStackTrace(exception), 4000));
        }
        
        // 保存日志
        try {
            logStorage.saveLog(jobLog);
        } catch (Exception e) {
            log.error("保存任务日志失败", e);
        }
        
        // 失败告警
        if (status == 0 && alarmNotifiers != null && !alarmNotifiers.isEmpty()) {
            String errorMsg = String.format("任务执行失败\n任务: %s.%s\n参数: %s\n异常: %s",
                    jobGroup, jobName, jobParam, exception != null ? exception.getMessage() : "未知");
            
            for (IJobAlarmNotifier notifier : alarmNotifiers) {
                try {
                    notifier.sendAlarm(jobName, errorMsg);
                } catch (Exception e) {
                    log.error("发送告警失败", e);
                }
            }
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
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
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
