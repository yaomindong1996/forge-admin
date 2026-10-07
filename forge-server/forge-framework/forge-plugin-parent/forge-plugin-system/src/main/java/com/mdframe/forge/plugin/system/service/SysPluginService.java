package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.plugin.system.dto.SysPluginQuery;
import com.mdframe.forge.plugin.system.enums.PluginLoadState;
import com.mdframe.forge.plugin.system.vo.SysPluginPageVO;
import com.mdframe.forge.plugin.system.vo.SysPluginVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.catalog.RuntimePlugin;
import com.mdframe.forge.starter.plugin.catalog.RuntimePluginCatalog;
import com.mdframe.forge.starter.plugin.catalog.PluginOrigin;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Arrays;

/** 当前后端实例只读目录，不从生产服务执行 CLI 或读源码工作区。 */
@Service
@RequiredArgsConstructor
public class SysPluginService {
    private final RuntimePluginCatalog catalog;
    private final FeatureGate gate;

    public SysPluginPageVO page(SysPluginQuery query) {
        validate(query);
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().strip().toLowerCase(Locale.ROOT);
        List<RuntimePlugin> filtered = catalog.getPlugins().stream()
                .filter(plugin -> matches(plugin, query.getOrigin(), keyword)).toList();
        long offset = ((long) query.getPageNum() - 1) * query.getPageSize();
        List<SysPluginVO> records = filtered.stream().skip(offset).limit(query.getPageSize())
                .map(this::view).toList();
        return new SysPluginPageVO(records, filtered.size(), query.getPageNum(), query.getPageSize(),
                ForgeVersion.CURRENT, gate.edition());
    }

    public SysPluginVO detail(String id) {
        return catalog.findById(id).map(this::view)
                .orElseThrow(() -> new BusinessException(404, "当前服务未加载此插件，请确认部署实例"));
    }

    private boolean matches(RuntimePlugin plugin, String origin, String keyword) {
        boolean sameOrigin = origin == null || plugin.origin().getCode().equals(origin);
        return sameOrigin && (plugin.id().toLowerCase(Locale.ROOT).contains(keyword)
                || plugin.name().toLowerCase(Locale.ROOT).contains(keyword));
    }

    private SysPluginVO view(RuntimePlugin plugin) {
        List<SysPluginVO.Feature> features = plugin.features().stream()
                .map(code -> new SysPluginVO.Feature(code, gate.isEnabled(code))).toList();
        PluginLoadState state = plugin.serverModule() == null
                ? PluginLoadState.METADATA_ONLY : PluginLoadState.BACKEND_LOADED;
        return new SysPluginVO(plugin.id(), plugin.name(), plugin.version(), plugin.origin().getCode(),
                plugin.edition().getCode(), state.getCode(), plugin.requiresCore(), plugin.serverModule(),
                plugin.hasUi(), features);
    }

    private void validate(SysPluginQuery query) {
        if (query.getPageNum() < 1 || query.getPageSize() < 1 || query.getPageSize() > 100) {
            throw new BusinessException(400, "分页参数不合法，每页最多 100 条");
        }
        if (query.getKeyword() != null && query.getKeyword().length() > 100) {
            throw new BusinessException(400, "搜索文字最多 100 个字符");
        }
        if (query.getOrigin() != null && Arrays.stream(PluginOrigin.values())
                .noneMatch(origin -> origin.getCode().equals(query.getOrigin()))) {
            throw new BusinessException(400, "插件来源不合法");
        }
    }
}
