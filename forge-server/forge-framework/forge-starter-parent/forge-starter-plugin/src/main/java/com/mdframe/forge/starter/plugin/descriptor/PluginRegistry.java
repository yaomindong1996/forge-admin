package com.mdframe.forge.starter.plugin.descriptor;

import com.mdframe.forge.starter.plugin.ForgeVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** 启动时一次性装载并校验，只有完整成功后才暴露不可变注册表。 */
@Slf4j
public final class PluginRegistry {

    public static final String DESCRIPTOR_PATTERN = "classpath*:META-INF/forge-plugin.json";

    private static final int MAX_DESCRIPTOR_BYTES = 64 * 1024;

    private final Map<String, PluginDescriptor> byId;
    private final List<PluginDescriptor> plugins;

    public PluginRegistry(ResourcePatternResolver resolver) {
        Map<String, PluginDescriptor> loaded = load(resolver);
        byId = Map.copyOf(loaded);
        plugins = List.copyOf(loaded.values());
    }

    public List<PluginDescriptor> getPlugins() {
        return plugins;
    }

    public Optional<PluginDescriptor> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    private Map<String, PluginDescriptor> load(ResourcePatternResolver resolver) {
        Map<String, PluginDescriptor> loaded = new TreeMap<>();
        try {
            for (Resource resource : resolver.getResources(DESCRIPTOR_PATTERN)) {
                PluginDescriptor descriptor = read(resource);
                PluginDescriptorValidator.validate(descriptor, ForgeVersion.CURRENT);
                if (loaded.putIfAbsent(descriptor.id(), descriptor) != null) {
                    String context = PluginDescriptorValidator.context(descriptor, ForgeVersion.CURRENT);
                    throw new IllegalArgumentException(context + "：id 重复，来源 " + resource.getDescription());
                }
            }
            return loaded;
        } catch (IOException exception) {
            log.error("扫描插件描述失败", exception);
            throw new IllegalStateException("扫描插件描述失败，当前核心 " + ForgeVersion.CURRENT, exception);
        }
    }

    private PluginDescriptor read(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            byte[] bytes = input.readNBytes(MAX_DESCRIPTOR_BYTES + 1);
            if (bytes.length > MAX_DESCRIPTOR_BYTES) {
                throw new IllegalArgumentException("插件描述超过 64 KiB：" + resource.getDescription());
            }
            try {
                return PluginDescriptorReader.read(bytes);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("插件描述无法读取/解析，来源 " + resource.getDescription()
                        + "，当前核心 " + ForgeVersion.CURRENT, exception);
            }
        } catch (IOException exception) {
            log.error("读取插件描述失败，来源={}", resource.getDescription(), exception);
            throw new IllegalStateException("插件描述无法读取/解析，来源 " + resource.getDescription()
                    + "，当前核心 " + ForgeVersion.CURRENT, exception);
        }
    }
}
