package com.mdframe.forge.plugin.ai.routing.vo;
import com.mdframe.forge.plugin.ai.routing.RouteCandidateSkip;
import lombok.Data;
import java.util.List;
@Data public class AiModelRoutePreviewVO { private Long providerId; private String providerName; private Long modelId; private String providerModelId; private String modelName; private String source; private String reason; private Long policyId; private List<RouteCandidateSkip> skippedCandidates; }
