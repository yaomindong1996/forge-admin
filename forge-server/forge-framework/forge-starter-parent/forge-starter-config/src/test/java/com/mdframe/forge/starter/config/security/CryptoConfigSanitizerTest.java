package com.mdframe.forge.starter.config.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoConfigSanitizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CryptoConfigSanitizer sanitizer = new CryptoConfigSanitizer(objectMapper);

    @Test
    void shouldRemoveDeploymentSecretFieldsFromCryptoJson() throws Exception {
        String json = """
                {
                  "enabled": true,
                  "secretKey": "redacted",
                  "rsaPrivateKey": "redacted",
                  "enableApiCrypto": true,
                  "persistence": {"activeKey": "redacted"}
                }
                """;

        JsonNode sanitized = objectMapper.readTree(sanitizer.sanitizeJson(json));

        assertTrue(sanitized.get("enabled").asBoolean());
        assertFalse(sanitized.has("secretKey"));
        assertFalse(sanitized.has("rsaPrivateKey"));
        assertFalse(sanitized.has("persistence"));
    }

    @Test
    void shouldRejectCryptoGroupWritesContainingDeploymentSecrets() {
        assertTrue(sanitizer.containsDeploymentSecretValue("crypto",
                "{\"enabled\":true,\"secretKey\":\"redacted\"}"));
        assertFalse(sanitizer.containsDeploymentSecretValue("login",
                "{\"secretKey\":\"not-crypto-group\"}"));
        assertFalse(sanitizer.containsDeploymentSecretValue("crypto",
                "{\"enabled\":true,\"enableApiCrypto\":false}"));
    }

    @Test
    void malformedCryptoJsonShouldFailClosed() {
        String malformed = "{\"secretKey\":\"redacted\"";

        assertTrue(sanitizer.containsDeploymentSecretValue("crypto", malformed));
        assertEquals("{}", sanitizer.sanitizeJson(malformed));
    }
}
