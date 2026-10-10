package com.mdframe.forge.starter.plugin.feature;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityFeatureGateTest {

    private final FeatureGate gate = new CommunityFeatureGate();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "community.hello", "custom.feature", "system:user", "ee"})
    void should_enable_ordinary_features(String code) {
        assertThat(gate.isEnabled(code)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ee.print", "ee.test", " ee.test "})
    void should_deny_enterprise_features_without_license(String code) {
        assertThat(gate.isEnabled(code)).isFalse();
    }

    @Test
    void should_identify_community_edition() {
        assertThat(gate.edition()).isEqualTo("community");
    }
}
