package com.mdframe.forge.starter.plugin.descriptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginDescriptorValidatorTest {

    @Test
    void should_accept_community_and_enterprise_metadata() {
        PluginDescriptorValidator.validate(descriptor("hello", List.of("community.hello"), null), "1.2.0");
        PluginDescriptor enterprise = new PluginDescriptor("enterprise", "企业示例", "1.0.0", PluginEdition.ENTERPRISE,
                ">=1.2.0 <2.0.0", List.of("ee.hello"), new PluginDescriptor.Server("forge-plugin-enterprise"), null);
        PluginDescriptorValidator.validate(enterprise, "1.2.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"h", "Hello", "../hello", "a_b", "a/hello", "hello ", "a123456789012345678901234567890123"})
    void should_reject_invalid_id_with_compatibility_context(String id) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginDescriptorValidator.validate(descriptor(id, List.of(), null), "1.2.0"))
                .withMessageContaining(id).withMessageContaining(">=1.2.0 <2.0.0").withMessageContaining("当前核心 1.2.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ee.hello", " hello", "hello ", "HELLO", "", "hello..a", "hello/a"})
    void should_reject_invalid_feature_or_edition_prefix(String feature) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                PluginDescriptorValidator.validate(descriptor("hello", List.of(feature), null), "1.2.0"));
    }

    @Test
    void should_reject_missing_duplicate_null_and_oversized_features() {
        for (List<String> features : Arrays.asList(null, Arrays.asList((String) null),
                List.of("hello", "hello"), List.of("a".repeat(65)))) {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    PluginDescriptorValidator.validate(descriptor("hello", features, null), "1.2.0"));
        }
        assertThatIllegalArgumentException().isThrownBy(() -> PluginDescriptorValidator.validate(null, "1.2.0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../ui", "/ui", "ui/../other", "ui/./other", "ui//other", "ui/", "C:/ui", "ui\\other"})
    void should_reject_unsafe_ui_directory(String path) {
        assertThatIllegalArgumentException().isThrownBy(() -> PluginDescriptorValidator.validate(
                descriptor("hello", List.of(), new PluginDescriptor.Ui(path)), "1.2.0"));
    }

    @Test
    void should_reject_bad_module_and_missing_metadata() {
        PluginDescriptor valid = descriptor("hello", List.of(), null);
        List<PluginDescriptor> invalid = List.of(
                new PluginDescriptor("hello", " ", "1.0.0", valid.edition(), valid.requiresCore(),
                        List.of(), null, null),
                new PluginDescriptor("hello", "示例", "1.0", valid.edition(), valid.requiresCore(),
                        List.of(), null, null),
                new PluginDescriptor("hello", "示例", "1.0.0", null, valid.requiresCore(), List.of(), null, null),
                new PluginDescriptor("hello", "示例", "1.0.0", valid.edition(), "", List.of(), null, null),
                new PluginDescriptor("hello", "示例", "1.0.0", valid.edition(), valid.requiresCore(), List.of(),
                        new PluginDescriptor.Server("../module"), null));
        for (PluginDescriptor descriptor : invalid) {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    PluginDescriptorValidator.validate(descriptor, "1.2.0"));
        }
    }

    @Test
    void should_reject_incompatible_core_with_clear_range() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                        PluginDescriptorValidator.validate(descriptor("hello", List.of(), null), "2.0.0"))
                .withMessageContaining("插件 hello").withMessageContaining(">=1.2.0 <2.0.0")
                .withMessageContaining("当前核心 2.0.0").withMessageContaining("核心版本不兼容");
    }

    @Test
    void should_defensively_copy_feature_list() {
        List<String> source = new ArrayList<>(List.of("hello"));
        PluginDescriptor descriptor = descriptor("hello", source, null);
        source.add("ee.hidden");
        assertThat(descriptor.features()).containsExactly("hello");
        assertThatThrownBy(() -> descriptor.features().add("other")).isInstanceOf(UnsupportedOperationException.class);
    }

    private PluginDescriptor descriptor(String id, List<String> features, PluginDescriptor.Ui ui) {
        return new PluginDescriptor(id, "示例插件", "1.0.0", PluginEdition.COMMUNITY,
                ">=1.2.0 <2.0.0", features, new PluginDescriptor.Server("forge-plugin-hello"), ui);
    }
}
