package com.mdframe.forge.plugin.job.spi;

/**
 * 告警通知SPI
 * 支持扩展到钉钉/企微/邮件等
 */
public interface IJobAlarmNotifier {
    
    /**
     * 发送告警
     * @param jobName 任务名称
     * @param errorMsg 错误信息
     */
    void sendAlarm(String jobName, String errorMsg);
}
