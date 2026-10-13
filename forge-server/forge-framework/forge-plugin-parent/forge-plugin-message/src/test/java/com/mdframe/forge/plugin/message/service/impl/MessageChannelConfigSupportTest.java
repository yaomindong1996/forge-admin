package com.mdframe.forge.plugin.message.service.impl;

import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageChannelConfigSupportTest {

    @Test
    void maskSecretShouldUseSensitiveMask() {
        String masked = MessageChannelConfigSupport.maskSecret("abcdefghijklmnop");
        assertThat(masked).isEqualTo(SensitiveDataUtil.MASK);
        assertThat(SensitiveDataUtil.isMaskedValue(masked)).isTrue();
    }

    @Test
    void lastEnabledChannelCannotDisable() {
        assertThatThrownBy(() -> MessageChannelConfigSupport.assertCanDisable(true, 0, "短信"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少保留一条启用");
        assertThatCode(() -> MessageChannelConfigSupport.assertCanDisable(true, 1, "短信"))
                .doesNotThrowAnyException();
    }

    @Test
    void isDisablingDetectsEnabledToDisabled() {
        assertThat(MessageChannelConfigSupport.isDisabling(
                EnableStatus.ENABLED.getCode(), EnableStatus.DISABLED.getCode())).isTrue();
        assertThat(MessageChannelConfigSupport.isDisabling(
                EnableStatus.ENABLED.getCode(), EnableStatus.ENABLED.getCode())).isFalse();
    }
}
