package com.mdframe.forge.plugin.capability.naming;

import com.mdframe.forge.plugin.capability.exception.CapabilityDefinitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityToolNameMapperTest {

    private final CapabilityToolNameMapper mapper = new CapabilityToolNameMapper();

    @Test
    void shouldKeepValidCapabilityCodeStable() {
        assertThat(mapper.toProtocolToolName("capability.ping")).isEqualTo("capability.ping");
        assertThat(mapper.toProtocolToolName("capability.ping")).isEqualTo("capability.ping");
    }

    @Test
    void shouldRejectInvalidOrOverlongCapabilityCode() {
        assertInvalid("");
        assertInvalid("Capability.ping");
        assertInvalid("capability..ping");
        assertInvalid("1capability.ping");
        assertInvalid("capability.ping-value");
        assertInvalid("a".repeat(129));
    }

    private void assertInvalid(String code) {
        assertThatThrownBy(() -> mapper.toProtocolToolName(code))
                .isInstanceOf(CapabilityDefinitionException.class)
                .hasMessageContaining("能力编码");
    }
}
