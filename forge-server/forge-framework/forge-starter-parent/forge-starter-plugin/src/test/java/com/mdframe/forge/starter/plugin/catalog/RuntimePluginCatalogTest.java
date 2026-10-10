package com.mdframe.forge.starter.plugin.catalog;

import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuntimePluginCatalogTest {
    @Test
    void should_merge_real_declarations_in_stable_order_and_freeze_snapshot() throws IOException {
        RuntimePluginCatalog catalog = catalog(new String[]{builtin("zeta")}, external("alpha"));
        assertThat(catalog.getPlugins()).extracting(RuntimePlugin::id).containsExactly("alpha", "zeta");
        RuntimePlugin builtin = catalog.findById("zeta").orElseThrow();
        assertThat(builtin.origin()).isEqualTo(PluginOrigin.BUILTIN);
        assertThat(builtin.version()).isEqualTo("1.1.9");
        assertThat(catalog.findById("alpha").orElseThrow().hasUi()).isTrue();
        assertThat(catalog.findById("missing")).isEmpty();
        assertThatThrownBy(() -> catalog.getPlugins().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> builtin.features().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_allow_empty_host_without_guessing_from_source_catalog() throws IOException {
        assertThat(catalog(new String[]{}).getPlugins()).isEmpty();
    }

    @Test
    void should_reject_duplicate_builtin_and_external_collision() {
        assertThatThrownBy(() -> catalog(new String[]{builtin("same"), builtin("same")}))
                .hasMessageContaining("ID 重复");
        assertThatThrownBy(() -> catalog(new String[]{builtin("same")}, external("same")))
                .hasMessageContaining("ID 重复");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{", "{}", "[]", "{\"id\":\"one\",\"id\":\"two\"}",
            "{\"unexpected\":true}", "{}{}"})
    void should_reject_malformed_or_ambiguous_declaration(String json) {
        assertThatThrownBy(() -> catalog(new String[]{json})).isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "42", "1.5"})
    void should_not_coerce_non_string_identity(String scalar) {
        String json = builtin("hello").replace("\"hello\"", scalar);
        assertThatThrownBy(() -> catalog(new String[]{json})).hasMessageContaining("无法解析");
    }

    @Test
    void should_reject_oversize_unsafe_id_missing_module_and_unfiltered_version() {
        assertThatThrownBy(() -> catalog(new String[]{" ".repeat(16 * 1024 + 1)}))
                .hasMessageContaining("超过 16 KiB");
        assertThatThrownBy(() -> catalog(new String[]{builtin("../bad")})).hasMessageContaining("ID 不合法");
        assertThatThrownBy(() -> catalog(new String[]{builtin("good").replace("forge-plugin-one", "../one")}))
                .hasMessageContaining("Maven 标识不合法");
        assertThatThrownBy(() -> catalog(new String[]{builtin("good").replace("1.1.9", "$" + "{project.version}")}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_fail_when_classpath_scan_fails() throws IOException {
        ResourcePatternResolver resolver = resolver(new String[]{}, new String[]{});
        when(resolver.getResources(BuiltinModuleLoader.PATTERN)).thenThrow(new IOException("fixture"));
        assertThatThrownBy(() -> new RuntimePluginCatalog(resolver, new PluginRegistry(resolver)))
                .hasMessageContaining("扫描内置模块声明失败");
    }

    private RuntimePluginCatalog catalog(String[] builtin, String... external) throws IOException {
        ResourcePatternResolver resolver = resolver(builtin, external);
        return new RuntimePluginCatalog(resolver, new PluginRegistry(resolver));
    }

    private ResourcePatternResolver resolver(String[] builtin, String[] external) throws IOException {
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources(BuiltinModuleLoader.PATTERN)).thenReturn(resources(builtin));
        when(resolver.getResources(PluginRegistry.DESCRIPTOR_PATTERN)).thenReturn(resources(external));
        return resolver;
    }

    private Resource[] resources(String[] json) {
        return Arrays.stream(json).map(value -> new ByteArrayResource(value.getBytes(StandardCharsets.UTF_8)))
                .toArray(Resource[]::new);
    }

    private String builtin(String id) {
        return """
                {"id":"%s","name":"内置测试模块","version":"1.1.9","module":"forge-plugin-one"}
                """.formatted(id);
    }

    private String external(String id) {
        return """
                {"id":"%s","name":"外部测试插件","version":"1.0.0","edition":"community",
                 "requiresCore":">=1.2.0","features":["community.one"],
                 "server":{"module":"forge-plugin-one"},"ui":{"dir":"ui/one"}}
                """.formatted(id);
    }
}
