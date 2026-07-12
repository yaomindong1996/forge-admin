package com.mdframe.forge.plugin.capability.flowaction.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public class FlowActionSourceService {

    private final FlowActionSourceMapper sourceMapper;
    private final ObjectMapper objectMapper;

    public ResolvedFlowActionSource requirePublished(
            Long tenantId,
            String suiteCode,
            String objectCode) {
        FlowActionSourceRow row;
        try {
            row = sourceMapper.selectPublishedFlowSource(
                    requireTenant(tenantId), suiteCode, objectCode);
        }
        catch (RuntimeException exception) {
            throw new SecureActionUnavailableException("FLOW_CATALOG_UNAVAILABLE", exception);
        }
        if (row == null) {
            throw new BusinessException("业务对象未发布或未配置启用的主流程");
        }
        String flowModelKey = resolveFlowModelKey(row);
        if (StringUtils.isBlank(flowModelKey)) {
            throw new BusinessException("业务对象主流程绑定缺少 flowModelKey");
        }
        return new ResolvedFlowActionSource(row, flowModelKey);
    }

    public ResolvedFlowActionSource requireMatching(
            Long tenantId,
            String suiteCode,
            String objectCode,
            Integer publishedObjectVersion,
            Long bindingId,
            String flowModelKey) {
        ResolvedFlowActionSource source = requirePublished(tenantId, suiteCode, objectCode);
        if (!source.row().getPublishedObjectVersion().equals(publishedObjectVersion)
                || !source.row().getBindingId().equals(bindingId)
                || !source.flowModelKey().equals(flowModelKey)) {
            throw new BusinessException(409, "FLOW_BINDING_MISMATCH");
        }
        return source;
    }

    private String resolveFlowModelKey(FlowActionSourceRow row) {
        if (StringUtils.isNotBlank(row.getBindingConfig())) {
            try {
                JsonNode config = objectMapper.readTree(row.getBindingConfig());
                String configured = StringUtils.trimToNull(config.path("flowModelKey").asText());
                if (configured != null) {
                    return configured;
                }
            }
            catch (Exception exception) {
                throw new SecureActionUnavailableException("FLOW_CATALOG_UNAVAILABLE", exception);
            }
        }
        return StringUtils.trimToNull(row.getBindingKey());
    }

    private Long requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }

    public record ResolvedFlowActionSource(
            FlowActionSourceRow row,
            String flowModelKey) {
    }
}
