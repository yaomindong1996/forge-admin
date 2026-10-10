package com.mdframe.forge.starter.plugin;

import com.mdframe.forge.starter.plugin.version.SemanticVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ForgeVersionTest {

    @Test
    void should_read_filtered_resource_matching_current_core_version() {
        assertThat(ForgeVersion.CURRENT).isEqualTo("1.2.0");
        assertThat(ForgeVersion.satisfies(">=1.2.0 <2.0.0")).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "1.2.0, '>=1.2.0 <2.0.0', true",
            "1.9.9, '>=1.2.0 <2.0.0', true",
            "1.1.3, '>=1.2.0 <2.0.0', false",
            "2.0.0, '>=1.2.0 <2.0.0', false",
            "1.2.0, '>1.2.0', false",
            "1.2.1, '>1.2.0 <=1.2.1', true",
            "1.2.0, '=1.2.0', true",
            "1.2.0, '1.2.0', true",
            "1.2.0-rc.1, '<1.2.0', true",
            "1.2.0-rc.10, '>1.2.0-rc.2', true",
            "1.2.0+build.7, '=1.2.0+other', true",
            "999999999999999999999.0.0, '>1.2.0', true"
    })
    void should_match_each_supported_comparison(String version, String range, boolean expected) {
        assertThat(ForgeVersion.satisfies(version, range)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "^1.2.0", "~1.2.0", "1.2", "*", ">=", "!=1.2.0", ">=1.2.0 || <2.0.0",
            ">=1.2.0 <2.x", "<0.0.1 illegal", "01.2.0", "1.2.0-01", "1.2.0+", "1.2.0-"})
    void should_reject_invalid_expression_even_after_non_matching_condition(String range) {
        assertThatIllegalArgumentException().isThrownBy(() -> ForgeVersion.satisfies("1.2.0", range));
    }

    @Test
    void should_sort_prereleases_before_release_in_semver_order() {
        List<String> ordered = List.of("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
                "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0");
        for (int index = 1; index < ordered.size(); index++) {
            assertThat(SemanticVersion.parse(ordered.get(index - 1)))
                    .isLessThan(SemanticVersion.parse(ordered.get(index)));
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"v1.2.0", "1.2", "01.2.0", "1.2.0-01", "1.2.0+", " 1.2.0 "})
    void should_reject_invalid_current_version(String version) {
        assertThatIllegalArgumentException().isThrownBy(() -> ForgeVersion.satisfies(version, ">=1.2.0"));
    }
}
