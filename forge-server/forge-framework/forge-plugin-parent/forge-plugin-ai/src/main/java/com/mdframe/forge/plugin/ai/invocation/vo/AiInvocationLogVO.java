package com.mdframe.forge.plugin.ai.invocation.vo;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class AiInvocationLogVO {
    private Long id; private String requestId; private Long userId; private String agentCode; private String sessionId;
    private String routeSource; private String routeReason; private Long routePolicyId; private Long providerId; private Long modelId; private String providerModelId; private String adapterCode;
    private String outcome; private String errorCategory; private Integer httpStatus; private String errorCode; private Long latencyMs;
    private Long promptTokens; private Long completionTokens; private Long totalTokens; private Boolean usageAvailable; private Boolean costAvailable; private LocalDateTime createTime;
}
