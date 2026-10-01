package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.dto.PrintBindingSaveDTO;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrintBindingDtoTest {

    @Test
    void shouldRequireFixedVersionOnlyForStandaloneSource() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var standalone = new PrintSourceRequest(
                    7L, "purchase_order", null, PrintSourceType.DATASET,
                    null, null, "purchase_order");
            var application = new PrintSourceRequest(
                    2L, PrintSourceType.LOWCODE, "page_purchase", null, "purchase_order");

            assertThat(validator.validate(binding(standalone, 31L))).isEmpty();
            assertThat(validator.validate(binding(standalone, null))).isNotEmpty();
            assertThat(validator.validate(binding(application, null))).isEmpty();
            assertThat(validator.validate(binding(application, 31L))).isNotEmpty();
        }
    }

    private PrintBindingSaveDTO binding(PrintSourceRequest source, Long versionId) {
        return new PrintBindingSaveDTO(
                null, null, source, 21L, versionId, PrintScene.DETAIL, true, 0, 1);
    }
}
