package com.mdframe.forge.plugin.ai.invocation.job;

import com.mdframe.forge.plugin.ai.invocation.mapper.AiModelInvocationLogMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import com.mdframe.forge.starter.tenant.util.TenantUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component @RequiredArgsConstructor
public class AiInvocationLogRetentionJob {
    private final AiModelInvocationLogMapper mapper;
    public String cleanExpiredInvocationLogs(String retentionDays) {
        int days=90; if(retentionDays!=null&&!retentionDays.isBlank()){try{days=Integer.parseInt(retentionDays.trim());}catch(NumberFormatException e){throw new BusinessException("保留天数必须是正整数");}}
        if(days<=0||days>3650)throw new BusinessException("保留天数必须在1到3650之间");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int deleted = TenantUtil.ignore(() -> mapper.deleteBefore(cutoff));
        return "deleted=" + deleted;
    }
    @ScheduledJob(name="aiInvocationLogRetention", group="AI", cron="0 20 2 * * ?", description="清理超期AI模型调用治理日志")
    public String cleanExpiredInvocationLogs(){return cleanExpiredInvocationLogs(null);}
}
