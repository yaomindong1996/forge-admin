package com.mdframe.forge.plugin.ai.invocation.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("ai_model_invocation_log")
public class AiModelInvocationLog extends TenantEntity {
    @TableId private Long id;
    private String requestId; private Long userId; private String agentCode; private String sessionId;
    private String phase; private Boolean dispatched; private String routeSource; private String routeReason; private Long routePolicyId;
    private Long providerId; private Long modelId; private String providerModelId; private String adapterCode;
    private String outcome; private String errorCategory; private Integer httpStatus; private String errorCode; private Long latencyMs;
    private Long promptTokens; private Long completionTokens; private Long totalTokens;
    private Boolean usageAvailable; private Boolean costAvailable;
    private Long inputPricePerMillionCent; private Long outputPricePerMillionCent;
}
