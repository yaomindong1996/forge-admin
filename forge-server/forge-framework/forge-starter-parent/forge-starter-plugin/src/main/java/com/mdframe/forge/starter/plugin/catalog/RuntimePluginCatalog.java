package com.mdframe.forge.starter.plugin.catalog;

import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptor;
import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** 启动期完整校验后发布只读快照；刷新清单不会动态加载/卸载代码。 */
public final class RuntimePluginCatalog {

    private final Map<String, RuntimePlugin> byId;
    private final List<RuntimePlugin> plugins;

    public RuntimePluginCatalog(ResourcePatternResolver resolver, PluginRegistry registry) {
        Map<String, RuntimePlugin> loaded = new TreeMap<>();
        for (RuntimePlugin module : BuiltinModuleLoader.load(resolver)) {
            add(loaded, module);
        }
        for (PluginDescriptor descriptor : registry.getPlugins()) {
            add(loaded, external(descriptor));
        }
        byId = Map.copyOf(loaded);
        plugins = List.copyOf(loaded.values());
    }

    public List<RuntimePlugin> getPlugins() {
        return plugins;
    }

    public Optional<RuntimePlugin> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    private static RuntimePlugin external(PluginDescriptor descriptor) {
        String module = descriptor.server() == null ? null : descriptor.server().module();
        return new RuntimePlugin(descriptor.id(), descriptor.name(), descriptor.version(), PluginOrigin.EXTERNAL,
                descriptor.edition(), descriptor.requiresCore(), module, descriptor.ui() != null,
                descriptor.features());
    }

    private static void add(Map<String, RuntimePlugin> loaded, RuntimePlugin plugin) {
        if (loaded.putIfAbsent(plugin.id(), plugin) != null) {
            throw new IllegalArgumentException("运行插件 ID 重复：" + plugin.id());
        }
    }
}
