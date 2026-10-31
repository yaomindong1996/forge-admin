package com.mdframe.forge.plugin.message.job;

import com.mdframe.forge.plugin.message.mapper.SysSmsRoutePolicyMapper;
import com.mdframe.forge.plugin.message.service.SmsConfigService;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时扫描开启 Ping 的租户并逐通道探活。
 * <p>
 * Cron 以注解默认为准；租户策略里的 pingCron 供运维对照，后续可做成动态调度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsPingScheduler {

    private final SysSmsRoutePolicyMapper routePolicyMapper;
    private final SmsConfigService smsConfigService;

    @IgnoreTenant
    @ScheduledJob(
            name = "smsChannelPing",
            group = "MESSAGE",
            cron = "0 0 */6 * * ?",
            description = "短信通道定时 Ping 探活",
            enabled = true
    )
    public void pingEnabledTenants() {
        List<Long> tenantIds = routePolicyMapper.selectPingEnabledTenantIds();
        if (tenantIds == null || tenantIds.isEmpty()) {
            return;
        }
        for (Long tenantId : tenantIds) {
            try {
                // 发送链路按当前租户取通道；不切换上下文会落到默认租户，探活打到别人的账号上
                int success = TenantContextHolder.executeWithTenant(tenantId,
                        () -> smsConfigService.runPingNow(tenantId));
                log.info("短信 Ping 完成: tenantId={}, successCount={}", tenantId, success);
            } catch (Exception ex) {
                log.warn("短信 Ping 失败: tenantId={}, err={}", tenantId, ex.getMessage());
            }
        }
    }
}
