package com.mdframe.forge.starter.crypto.config;

import com.mdframe.forge.starter.core.context.CryptoProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoConfigurationValidatorTest {

    private static final String VALID_16_BYTE_KEY = Base64.getEncoder()
            .encodeToString("1234567890abcdef".getBytes(StandardCharsets.UTF_8));

    private final CryptoConfigurationValidator validator = new CryptoConfigurationValidator();

    @Test
    void shouldRejectEnabledApiCryptoWithoutRootKey() {
        CryptoProperties properties = new CryptoProperties();
        properties.setSecretKey(null);
        properties.getPersistence().setEnabled(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validator.validate(properties));

        assertTrue(exception.getMessage().contains("FORGE_CRYPTO_SECRET_KEY"));
    }

    @Test
    void shouldRejectInvalidRootKeyWithoutLeakingValue() {
        CryptoProperties properties = new CryptoProperties();
        properties.setSecretKey("not-a-base64-secret");
        properties.getPersistence().setEnabled(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validator.validate(properties));

        assertTrue(exception.getMessage().contains("FORGE_CRYPTO_SECRET_KEY"));
        assertTrue(!exception.getMessage().contains("not-a-base64-secret"));
    }

    @Test
    void shouldNotRequireKeysWhenGlobalCryptoDisabled() {
        CryptoProperties properties = new CryptoProperties();
        properties.setEnabled(false);
        properties.setSecretKey("disabled-invalid-placeholder");
        properties.getPersistence().setWriteVersioned(true);
        properties.getPersistence().setActiveKeyId("bad key id");

        assertDoesNotThrow(() -> validator.validate(properties));
    }

    @Test
    void shouldRequireLegacyKeyWhenWritingOldPersistenceFormat() {
        CryptoProperties properties = new CryptoProperties();
        properties.setEnableApiCrypto(false);
        properties.setEnableFieldCrypto(false);
        properties.setSecretKey(null);
        properties.getPersistence().setEnabled(true);
        properties.getPersistence().setWriteVersioned(false);
        properties.getPersistence().setLegacyReadEnabled(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validator.validate(properties));

        assertTrue(exception.getMessage().contains("FORGE_CRYPTO_PERSISTENCE_LEGACY_KEY"));
    }

    @Test
    void shouldRequireSafeActiveKeyIdWhenWritingVersionedPersistenceFormat() {
        CryptoProperties properties = new CryptoProperties();
        properties.setEnableApiCrypto(false);
        properties.setEnableFieldCrypto(false);
        properties.getPersistence().setEnabled(true);
        properties.getPersistence().setWriteVersioned(true);
        properties.getPersistence().setLegacyReadEnabled(false);
        properties.getPersistence().setActiveKeyId("bad key id");
        properties.getPersistence().setActiveKey(VALID_16_BYTE_KEY);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validator.validate(properties));

        assertTrue(exception.getMessage().contains("active-key-id"));
    }

    @Test
    void shouldAcceptValidCompatibilityConfig() {
        CryptoProperties properties = new CryptoProperties();
        properties.setSecretKey(VALID_16_BYTE_KEY);
        properties.getPersistence().setEnabled(true);
        properties.getPersistence().setWriteVersioned(false);
        properties.getPersistence().setLegacyReadEnabled(true);

        assertDoesNotThrow(() -> validator.validate(properties));
    }

    @Test
    void shouldAcceptValidVersionedPersistenceConfig() {
        CryptoProperties properties = new CryptoProperties();
        properties.setEnableApiCrypto(false);
        properties.setEnableFieldCrypto(false);
        properties.getPersistence().setEnabled(true);
        properties.getPersistence().setWriteVersioned(true);
        properties.getPersistence().setLegacyReadEnabled(false);
        properties.getPersistence().setActiveKeyId("v2");
        properties.getPersistence().setActiveKey(VALID_16_BYTE_KEY);
        properties.getPersistence().getKeys().put("v1", VALID_16_BYTE_KEY);

        assertDoesNotThrow(() -> validator.validate(properties));
    }

    @Test
    void shouldRejectBlankHistoricalKey() {
        CryptoProperties properties = new CryptoProperties();
        properties.setEnableApiCrypto(false);
        properties.setEnableFieldCrypto(false);
        properties.getPersistence().setWriteVersioned(true);
        properties.getPersistence().setLegacyReadEnabled(false);
        properties.getPersistence().setActiveKeyId("v2");
        properties.getPersistence().setActiveKey(VALID_16_BYTE_KEY);
        properties.getPersistence().getKeys().put("v1", " ");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validator.validate(properties));

        assertTrue(exception.getMessage().contains("forge.crypto.persistence.keys.v1"));
    }
}
