package com.mdframe.forge.starter.plugin.delivery;

import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptor;
import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptorReader;
import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptorValidator;

import java.util.List;
import java.util.Map;

import static com.mdframe.forge.starter.plugin.delivery.PackagePathRules.require;

/** 只做格式/内容摘要；不把这次预检冒充安装器对宿主工作区的完整检查。 */
public final class SourcePluginPackageReader {
    private SourcePluginPackageReader() {
    }

    public static SourcePluginPackage read(byte[] archive, String coreVersion) {
        Map<String, byte[]> files = PluginZipReader.read(archive);
        byte[] root = files.get("forge-plugin.json");
        PluginDescriptor descriptor = PluginDescriptorReader.read(root);
        PluginDescriptorValidator.validate(descriptor, coreVersion);
        require(descriptor.name().length() <= 128 && descriptor.features().size() <= 128
                && descriptor.version().length() <= 256, "插件名称、版本或功能数量超过限制");
        require(descriptor.server() != null || descriptor.ui() != null, "插件至少需要后端或 UI");
        validateComponents(files, descriptor, root);
        long expanded = files.values().stream().mapToLong(value -> value.length).sum();
        List<SourcePluginPackage.FileSummary> summaries = files.entrySet().stream().limit(200)
                .map(entry -> new SourcePluginPackage.FileSummary(entry.getKey(), entry.getValue().length,
                        PackageDigests.sha256(entry.getValue()))).toList();
        return new SourcePluginPackage(descriptor, PackageDigests.sha256(archive), archive.length,
                expanded, files.size(), summaries);
    }

    private static void validateComponents(Map<String, byte[]> files, PluginDescriptor descriptor, byte[] root) {
        String server = descriptor.server() == null ? null : "server/" + descriptor.server().module();
        if (server != null) {
            byte[] runtime = files.get(server + "/src/main/resources/META-INF/forge-plugin.json");
            require(PluginDescriptorReader.identical(root, runtime), "根描述与运行时描述不一致");
            SourcePomValidator.validate(files.get(server + "/pom.xml"), descriptor.server().module());
        }
        if (descriptor.ui() != null) {
            String ui = descriptor.ui().dir();
            require(files.keySet().stream().anyMatch(path -> path.startsWith(ui + "/")), "插件 UI 目录没有文件");
            require(server == null || !(ui.equals(server) || ui.startsWith(server + "/")
                    || server.startsWith(ui + "/")), "前后端组件目录不能重叠");
        }
    }
}
