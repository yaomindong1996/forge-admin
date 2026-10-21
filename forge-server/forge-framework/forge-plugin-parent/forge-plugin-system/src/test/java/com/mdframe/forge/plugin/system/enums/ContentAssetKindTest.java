package com.mdframe.forge.plugin.system.enums;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentAssetKindTest {

    @Test
    void resolvesSupportedCodes() {
        assertThat(ContentAssetKind.require("print")).isEqualTo(ContentAssetKind.PRINT);
        assertThat(ContentAssetKind.require("FORM")).isEqualTo(ContentAssetKind.FORM);
        assertThat(ContentAssetKind.require("print-component")).isEqualTo(ContentAssetKind.PRINT_COMPONENT);
        assertThat(ContentAssetKind.PRINT.matches("Print")).isTrue();
    }

    @Test
    void rejectsUnknownKind() {
        assertThatThrownBy(() -> ContentAssetKind.require("plugin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持");
    }
}
