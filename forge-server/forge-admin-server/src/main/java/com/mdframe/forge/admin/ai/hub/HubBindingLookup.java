package com.mdframe.forge.admin.ai.hub;

import com.mdframe.forge.admin.ai.hub.mapper.HubBindingMapper;
import com.mdframe.forge.plugin.system.service.ISysConfigService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 租户默认助手绑定查询：优先读取 ai_hub_binding，未配置时回退至系统参数 ai.hub.chat-agent-code。
 */
@Component
@RequiredArgsConstructor
class HubBindingLookup {

    static final String GENERAL_AGENT_KIND = "GENERAL_AGENT";
    private static final String LEGACY_GENERAL_AGENT_CONFIG = "ai.hub.chat-agent-code";

    private final HubBindingMapper bindings;
    private final ISysConfigService configService;

    String findGeneralAgentCode(Long tenantId) {
        return TenantContextHolder.executeWithTenant(tenantId, () -> {
            String configured = bindings.selectBindingValue(tenantId, GENERAL_AGENT_KIND);
            if (StringUtils.hasText(configured)) {
                return configured.strip();
            }
            String fallback = configService.selectConfigByKey(LEGACY_GENERAL_AGENT_CONFIG);
            return StringUtils.hasText(fallback) ? fallback.strip() : null;
        });
    }
}
