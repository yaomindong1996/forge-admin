package com.mdframe.forge.plugin.system.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 新租户可选基础数据包。创建时由管理员勾选；{@code null} 表示使用推荐默认集。
 */
public final class TenantBootstrapPackages {

    public static final String DICT_AND_CONFIG = "dictAndConfig";
    public static final String ROOT_ORG = "rootOrg";
    public static final String TENANT_ADMIN_ROLE = "tenantAdminRole";
    public static final String MESSAGE_TEMPLATES = "messageTemplates";
    public static final String FLOW_ASSIST = "flowAssist";
    public static final String DATA_SCOPE_PRESETS = "dataScopePresets";

    public static final List<String> RECOMMENDED = List.of(
            DICT_AND_CONFIG,
            ROOT_ORG,
            TENANT_ADMIN_ROLE,
            MESSAGE_TEMPLATES,
            FLOW_ASSIST,
            DATA_SCOPE_PRESETS
    );

    private TenantBootstrapPackages() {
    }

    /**
     * {@code null} → 推荐默认；空列表 → 不引导；其它 → 仅保留已知编码并去重保序。
     */
    public static Set<String> normalize(List<String> selected) {
        if (selected == null) {
            return new LinkedHashSet<>(RECOMMENDED);
        }
        Set<String> known = Set.copyOf(RECOMMENDED);
        Set<String> out = new LinkedHashSet<>();
        for (String raw : selected) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String code = raw.trim();
            if (known.contains(code)) {
                out.add(code);
            }
        }
        return out;
    }
}
