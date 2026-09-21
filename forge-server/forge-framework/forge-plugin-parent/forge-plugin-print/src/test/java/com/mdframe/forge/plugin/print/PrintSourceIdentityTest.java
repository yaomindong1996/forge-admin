package com.mdframe.forge.plugin.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.dto.PrintTemplateCreateDTO;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.assertj.core.api.Assertions.*;

class PrintSourceIdentityTest {
    @Test
    void acceptsWorkspaceIdentifiersAndRejectsInvalidOrAmbiguousSource() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String page : new String[]{"page_purchase", "form-09", "9007199254740993", "a".repeat(128)}) {
                assertThat(validator.validate(source(page))).isEmpty();
                assertThat(validator.validate(new PrintTemplateCreateDTO(2L, "order", "合成模板",
                        PrintSourceType.LOWCODE, page, null, "purchase", "{}"))).isEmpty();
            }
            for (String page : new String[]{"", " ", "x/y", "x\nother", "a".repeat(129)}) {
                assertThat(validator.validate(source(page))).isNotEmpty();
            }
            assertThat(validator.validate(source(null))).isNotEmpty();
            assertThat(validator.validate(new PrintSourceRequest(2L, PrintSourceType.CODE,
                    "page_purchase", "form", "purchase"))).isNotEmpty();
        }
    }

    @Test
    void preservesLegacyNumericWireAndHashWithoutFloatingPointConversion() throws Exception {
        var mapper = new ObjectMapper();
        var source = mapper.readValue("""
                {"applicationId":2,"sourceType":"LOWCODE","pageId":9007199254740993,
                 "formKey":null,"objectCode":"purchase"}
                """, PrintSourceRequest.class);
        assertThat(source.pageId()).isEqualTo("9007199254740993");
        String legacy = "LOWCODE\n9007199254740993\n\npurchase";
        assertThat(source.key()).isEqualTo("LOWCODE:" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(legacy.getBytes(StandardCharsets.UTF_8))));
        assertThat(mapper.readValue(mapper.writeValueAsString(source), PrintSourceRequest.class)).isEqualTo(source);
        assertThat(source("page_purchase").key()).isNotEqualTo(source("page_Purchase").key());
    }

    private PrintSourceRequest source(String page) {
        return new PrintSourceRequest(2L, PrintSourceType.LOWCODE, page, null, "purchase");
    }
}
