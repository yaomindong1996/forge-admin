package com.mdframe.forge.plugin.generator.service.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LowcodeEncryptConfigParserTest {

    private final LowcodeEncryptConfigParser parser = new LowcodeEncryptConfigParser(new ObjectMapper());

    @Test
    void shouldParseFieldAlgorithmAndColumnName() {
        List<LowcodeEncryptConfigParser.FieldRule> rules = parser.parse("""
                {
                  "phoneNumber": {"algorithm": "SM4"},
                  "idCard": {"algorithm": "AES", "columnName": "id_card_cipher"}
                }
                """);

        assertEquals(2, rules.size());
        assertEquals("phoneNumber", rules.get(0).fieldName());
        assertEquals("phone_number", rules.get(0).columnName());
        assertEquals("SM4", rules.get(0).algorithm());
        assertEquals("id_card_cipher", rules.get(1).columnName());
        assertEquals("AES", rules.get(1).algorithm());
    }

    @Test
    void shouldRejectInvalidConfig() {
        assertThrows(BusinessException.class, () -> parser.parse("[]"));
        assertThrows(BusinessException.class, () -> parser.parse("""
                {"phone": {"algorithm": "DES"}}
                """));
        assertThrows(BusinessException.class, () -> parser.parse("""
                {"phone;drop": {"algorithm": "SM4"}}
                """));
    }
}
