package com.mdframe.forge.plugin.system.vo;
import java.util.List;
public record PluginRuntimeProbeVO(String nonce, String coreVersion, String jarSha256, List<Plugin> plugins) {
    public record Plugin(String id, String version, boolean backendLoaded) {}
}
