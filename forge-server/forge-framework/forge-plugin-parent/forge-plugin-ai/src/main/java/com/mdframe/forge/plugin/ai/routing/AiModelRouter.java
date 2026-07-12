package com.mdframe.forge.plugin.ai.routing;
public interface AiModelRouter {
    RoutedInvocation route(RouteRequest request);
    RouteDecision preview(RouteRequest request);
}
