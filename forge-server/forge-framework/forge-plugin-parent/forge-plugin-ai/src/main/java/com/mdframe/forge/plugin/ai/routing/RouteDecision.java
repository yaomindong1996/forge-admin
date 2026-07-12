package com.mdframe.forge.plugin.ai.routing;

import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteReason;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteSource;
import java.util.List;

public record RouteDecision(AiProvider provider, AiModel model, AiModelRouteSource source,
                            AiModelRouteReason reason, Long policyId,
                            List<RouteCandidateSkip> skippedCandidates) { }
