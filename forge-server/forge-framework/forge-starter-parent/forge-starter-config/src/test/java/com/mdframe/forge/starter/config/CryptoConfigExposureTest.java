package com.mdframe.forge.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.config.config.CryptoManageConfig;
import com.mdframe.forge.starter.config.converter.ConfigConverter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoConfigExposureTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void cryptoManageConfigShouldNotSerializeDeploymentSecretFields() throws Exception {
        CryptoManageConfig config = new CryptoManageConfig();

        String json = objectMapper.writeValueAsString(config);

        assertFalse(json.contains("secretKey"));
        assertFalse(json.contains("rsaPrivateKey"));
        assertFalse(json.contains("activeKey"));
        assertFalse(json.contains("legacyKey"));
        assertFalse(json.contains("keys"));
    }

    @Test
    void cryptoManageConfigShouldDetectSubmittedDeploymentSecretFields() throws Exception {
        String json = """
                {
                  "enabled": true,
                  "secretKey": "redacted",
                  "persistence": {"activeKey": "redacted"}
                }
                """;

        CryptoManageConfig config = objectMapper.readValue(json, CryptoManageConfig.class);

        assertTrue(config.hasDeploymentSecretFields());
    }

    @Test
    void configConverterShouldNotFlattenDeploymentSecretKeys() throws Exception {
        String json = """
                {
                  "enabled": true,
                  "algorithm": "SM4",
                  "secretKey": "redacted",
                  "rsaPublicKey": "redacted",
                  "rsaPrivateKey": "redacted",
                  "persistence": {"activeKey": "redacted", "keys": {"old": "redacted"}},
                  "enableApiCrypto": true
                }
                """;

        Map<String, String> flattened = new ConfigConverter().convertCryptoConfig(json);

        assertTrue(flattened.containsKey("forge.crypto.enabled"));
        assertTrue(flattened.containsKey("forge.crypto.enable-api-crypto"));
        assertFalse(flattened.containsKey("forge.crypto.secret-key"));
        assertFalse(flattened.containsKey("forge.crypto.rsa-public-key"));
        assertFalse(flattened.containsKey("forge.crypto.rsa-private-key"));
        assertFalse(flattened.keySet().stream().anyMatch(key -> key.startsWith("forge.crypto.persistence")));
    }
}
