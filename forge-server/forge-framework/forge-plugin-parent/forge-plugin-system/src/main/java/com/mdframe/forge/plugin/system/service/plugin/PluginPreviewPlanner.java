package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.enums.PluginTaskOperation;
import com.mdframe.forge.plugin.system.vo.SysPluginPreviewVO;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.catalog.PluginOrigin;
import com.mdframe.forge.starter.plugin.catalog.RuntimePlugin;
import com.mdframe.forge.starter.plugin.catalog.RuntimePluginCatalog;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import com.mdframe.forge.starter.plugin.delivery.SourcePluginPackage;
import com.mdframe.forge.starter.plugin.descriptor.PluginEdition;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.version.SemanticVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PluginPreviewPlanner {
    private final RuntimePluginCatalog catalog;
    private final FeatureGate gate;

    public SysPluginPreviewVO preview(SourcePluginPackage source) {
        var descriptor = source.descriptor();
        RuntimePlugin current = catalog.findById(descriptor.id()).orElse(null);
        List<String> blockers = new ArrayList<>();
        if (descriptor.edition() == PluginEdition.ENTERPRISE) {
            blockers.add("商业插件须通过独立 Pro 工程交付，本工作台暂不受理");
        }
        validateCurrent(source, current, blockers);
        if (descriptor.server() != null && catalog.getPlugins().stream().anyMatch(plugin ->
                !plugin.id().equals(descriptor.id()) && descriptor.server().module().equals(plugin.serverModule()))) {
            blockers.add("后端模块名与当前实例其它插件冲突");
        }
        PluginTaskOperation operation = current == null ? PluginTaskOperation.INSTALL : PluginTaskOperation.REPLACE;
        List<String> warnings = List.of("这里只比较上传包与当前后端构建声明，尚未连接源码工作区。",
                "待执行器检查源码登记、目标目录、客户定制和构建依赖；不会在 Web 服务中执行。",
                "确认仅排队，执行器须单独启用并审查。SQL/权限变更仍需审查，源码回滚不回滚数据库。",
                "ZIP 格式和摘要校验不证明交付方可信，也不代表恶意代码或密钥内容检测通过。");
        return new SysPluginPreviewVO(source, ForgeVersion.CURRENT, current == null ? null : current.version(),
                operation.getCode(), snapshot(), List.copyOf(blockers), warnings);
    }

    public String snapshot() {
        String plugins = catalog.getPlugins().stream()
                .map(plugin -> plugin.id() + ":" + plugin.version() + ":" + plugin.origin() + ":"
                        + plugin.edition() + ":" + plugin.serverModule())
                .collect(Collectors.joining("\n"));
        return PackageDigests.sha256(ForgeVersion.CURRENT + "\n" + gate.edition() + "\n" + plugins);
    }

    private void validateCurrent(SourcePluginPackage source, RuntimePlugin current, List<String> blockers) {
        if (current == null) {
            return;
        }
        if (current.origin() == PluginOrigin.BUILTIN) {
            blockers.add("不能用外部插件替换核心内置模块");
        }
        var nextVersion = SemanticVersion.parse(source.descriptor().version());
        if (nextVersion.compareTo(SemanticVersion.parse(current.version())) < 0) {
            blockers.add("禁止降级当前实例的插件版本，需另行审查恢复方案");
        }
    }
}
