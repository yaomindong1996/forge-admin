package com.mdframe.forge.plugin.ai.routing;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
public record RouteRequest(Long tenantId, AiAgent agent, Long providerId, String modelName) { }
