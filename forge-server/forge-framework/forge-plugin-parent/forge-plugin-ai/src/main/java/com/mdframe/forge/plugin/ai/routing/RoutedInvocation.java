package com.mdframe.forge.plugin.ai.routing;
import com.mdframe.forge.plugin.ai.health.AiModelHealthLease;
public record RoutedInvocation(RouteDecision decision, AiModelHealthLease healthLease) implements AutoCloseable {
    @Override public void close() { healthLease.close(); }
}
