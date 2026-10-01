package com.mdframe.forge.plugin.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.service.PrintParameterValidator;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrintParameterValidatorTest {

    private final PrintParameterValidator validator = new PrintParameterValidator(new ObjectMapper());

    @Test
    void shouldNormalizeOnlyDeclaredScalarParameters() {
        String schema = """
                {
                  "status":{"type":"string","required":true,"enum":["OPEN","DONE"]},
                  "minAmount":{"type":"number","min":0},
                  "limit":{"type":"integer","default":20,"max":100}
                }
                """;

        Map<String, Object> result = validator.validate(
                schema, Map.of("status", "OPEN", "minAmount", 12.5));

        assertThat(result).containsEntry("status", "OPEN")
                .containsEntry("minAmount", new BigDecimal("12.5"))
                .containsEntry("limit", 20L);
    }

    @Test
    void shouldRejectUnknownReservedAndWrongTypeParameters() {
        String schema = "{\"status\":{\"type\":\"string\"}}";

        assertInvalid(schema, Map.of("other", "OPEN"));
        assertInvalid("{\"tenantId\":{\"type\":\"integer\"}}", Map.of());
        assertInvalid(schema, Map.of("status", 1));
    }

    private void assertInvalid(String schema, Map<String, Object> params) {
        assertThatThrownBy(() -> validator.validate(schema, params))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo(400));
    }
}
