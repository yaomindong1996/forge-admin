package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.plugin.system.dto.SysPluginQuery;
import com.mdframe.forge.plugin.system.vo.SysPluginVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.catalog.PluginOrigin;
import com.mdframe.forge.starter.plugin.catalog.RuntimePlugin;
import com.mdframe.forge.starter.plugin.catalog.RuntimePluginCatalog;
import com.mdframe.forge.starter.plugin.descriptor.PluginEdition;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysPluginServiceTest {
    private final RuntimePluginCatalog catalog = mock(RuntimePluginCatalog.class);
    private final FeatureGate gate = mock(FeatureGate.class);
    private final SysPluginService service = new SysPluginService(catalog, gate);

    @BeforeEach
    void setup() {
        when(catalog.getPlugins()).thenReturn(List.of(plugin("one", PluginOrigin.BUILTIN),
                plugin("two", PluginOrigin.EXTERNAL)));
        when(gate.edition()).thenReturn("community");
    }

    @Test
    void should_page_filtered_total_and_keep_core_version_separate() {
        SysPluginQuery query = new SysPluginQuery();
        query.setPageSize(1);
        query.setPageNum(2);
        var page = service.page(query);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.records()).extracting(SysPluginVO::id).containsExactly("two");
        assertThat(page.coreVersion()).isEqualTo("1.2.0");
        query.setOrigin("builtin");
        query.setKeyword(" ONE ");
        query.setPageNum(1);
        assertThat(service.page(query).total()).isEqualTo(1);
        query.setKeyword("不存在");
        assertThat(service.page(query).records()).isEmpty();
    }

    @Test
    void should_handle_max_page_number_without_overflow() {
        SysPluginQuery query = new SysPluginQuery();
        query.setPageNum(Integer.MAX_VALUE);
        assertThat(service.page(query).records()).isEmpty();
        assertThat(service.page(query).total()).isEqualTo(2);
    }

    @Test
    void should_return_gate_result_without_claiming_health_or_ui_deployment() {
        RuntimePlugin plugin = plugin("one", PluginOrigin.EXTERNAL);
        when(catalog.findById("one")).thenReturn(Optional.of(plugin));
        when(gate.isEnabled("community.one")).thenReturn(true);
        var detail = service.detail("one");
        assertThat(detail.loadState()).isEqualTo("backend_loaded");
        assertThat(detail.features()).containsExactly(new SysPluginVO.Feature("community.one", true));
        when(gate.isEnabled("community.one")).thenReturn(false);
        assertThat(service.detail("one").features().get(0).enabled()).isFalse();
        RuntimePlugin metadata = new RuntimePlugin("ui", "界面", "1.0.0", PluginOrigin.EXTERNAL,
                PluginEdition.COMMUNITY, ">=1.2.0", null, true, List.of());
        when(catalog.findById("ui")).thenReturn(Optional.of(metadata));
        assertThat(service.detail("ui").loadState()).isEqualTo("metadata_only");
    }

    @Test
    void should_explain_unknown_id_and_reject_invalid_query() {
        when(catalog.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail("missing")).isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前服务未加载");
        SysPluginQuery query = new SysPluginQuery();
        query.setPageNum(0);
        assertThatThrownBy(() -> service.page(query)).hasMessageContaining("分页参数");
        query.setPageNum(1);
        query.setPageSize(101);
        assertThatThrownBy(() -> service.page(query)).hasMessageContaining("分页参数");
        query.setPageSize(15);
        query.setOrigin("other");
        assertThatThrownBy(() -> service.page(query)).hasMessageContaining("来源");
        query.setOrigin(null);
        query.setKeyword("a".repeat(101));
        assertThatThrownBy(() -> service.page(query)).hasMessageContaining("最多 100");
    }

    private RuntimePlugin plugin(String id, PluginOrigin origin) {
        return new RuntimePlugin(id, "测试 " + id, "1.0.0", origin, PluginEdition.COMMUNITY,
                ">=1.2.0", "forge-plugin-" + id, true, List.of("community.one"));
    }
}
