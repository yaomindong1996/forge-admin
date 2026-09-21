package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PrintTemplateDtoTest {

    @Test
    void validatesRequiredFieldsRevisionsAndSourceIdentity() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new PrintTemplateCreateDTO(1L, "purchase", "采购单", PrintSourceType.LOWCODE, "2", null, "purchase", "{}"))).isEmpty();
            assertThat(validator.validate(new PrintTemplateCreateDTO(1L, "purchase", "采购单", PrintSourceType.CODE, null, "purchaseForm", "purchase", "{}"))).isEmpty();
            assertThat(validator.validate(new PrintTemplateCreateDTO(1L, "purchase", "采购单", PrintSourceType.CODE, "2", "purchaseForm", "purchase", "{}"))).anySatisfy(error -> assertThat(error.getPropertyPath().toString()).isEqualTo("sourceValid"));
            assertThat(validator.validate(new PrintTemplateCreateDTO(null, "../invalid", " ", null, "bad/page", null, "", ""))).hasSizeGreaterThanOrEqualTo(6);
            assertThat(validator.validate(new PrintTemplateUpdateDTO(0L, "模板", "{}"))).isNotEmpty();
            assertThat(validator.validate(new PrintTemplatePublishDTO(Long.MAX_VALUE))).isNotEmpty();
            assertThat(validator.validate(new PrintTemplateStatusDTO(1L, 2))).isNotEmpty();
            assertThat(validator.validate(new PrintTemplateCopyDTO(1L, "copy", "模板"))).isEmpty();
        }
    }
}
