package com.mdframe.forge.starter.crypto.persistence;

import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.crypto.impl.AESEncryptor;
import com.mdframe.forge.starter.crypto.crypto.impl.SM4Encryptor;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionedPersistentCryptoServiceTest {

    private static final String LEGACY_KEY = key("legacy-crypto-01");
    private static final String ACTIVE_KEY = key("active-crypto-01");
    private static final String HISTORY_KEY = key("historycrypto-01");

    @Test
    void shouldWriteLegacyFormatWhenVersionedWriteDisabled() {
        CryptoProperties properties = compatibleProperties();
        VersionedPersistentCryptoService service = service(properties);

        String ciphertext = service.encrypt("secret-value", "SM4");

        assertFalse(ciphertext.startsWith("FPC1:"));
        assertEquals("secret-value", service.decrypt(ciphertext, "SM4"));
        assertEquals(PersistentCiphertext.Format.LEGACY, service.inspect(ciphertext, "SM4").format());
    }

    @Test
    void shouldWriteAndReadActiveVersionedCiphertext() {
        CryptoProperties properties = versionedProperties();
        VersionedPersistentCryptoService service = service(properties);

        String ciphertext = service.encrypt("secret-value", "SM4");

        assertTrue(ciphertext.startsWith("FPC1:SM4:v2:"));
        assertEquals("secret-value", service.decrypt(ciphertext, "SM4"));
        assertEquals(PersistentCiphertext.Format.ACTIVE, service.inspect(ciphertext, "SM4").format());
    }

    @Test
    void shouldReadHistoricalVersionedCiphertext() {
        CryptoProperties oldProperties = versionedProperties();
        oldProperties.getPersistence().setActiveKeyId("v1");
        oldProperties.getPersistence().setActiveKey(HISTORY_KEY);
        VersionedPersistentCryptoService oldService = service(oldProperties);
        String historicalCiphertext = oldService.encrypt("secret-value", "AES");

        VersionedPersistentCryptoService service = service(versionedProperties());

        assertEquals("secret-value", service.decrypt(historicalCiphertext, "SM4"));
        assertEquals(PersistentCiphertext.Format.HISTORICAL,
                service.inspect(historicalCiphertext, "SM4").format());
    }

    @Test
    void shouldReadLegacyCiphertextWhenLegacyReadEnabled() {
        VersionedPersistentCryptoService legacyService = service(compatibleProperties());
        String legacyCiphertext = legacyService.encrypt("secret-value", "SM4");

        VersionedPersistentCryptoService service = service(versionedProperties());

        assertEquals("secret-value", service.decrypt(legacyCiphertext, "SM4"));
    }

    @Test
    void shouldFailClosedForUnknownKeyId() {
        VersionedPersistentCryptoService service = service(versionedProperties());
        String ciphertext = service.encrypt("secret-value", "SM4")
                .replace("FPC1:SM4:v2:", "FPC1:SM4:missing:");

        assertThrows(IllegalStateException.class, () -> service.decrypt(ciphertext, "SM4"));
        assertEquals(PersistentCiphertext.Format.UNKNOWN_KEY, service.inspect(ciphertext, "SM4").format());
    }

    @Test
    void shouldFailClosedForMalformedHeader() {
        VersionedPersistentCryptoService service = service(versionedProperties());

        assertThrows(IllegalStateException.class, () -> service.decrypt("FPC1:SM4:v2", "SM4"));
        assertEquals(PersistentCiphertext.Format.UNKNOWN, service.inspect("FPC1:SM4:v2", "SM4").format());
    }

    @Test
    void shouldFailClosedWhenLegacyReadDisabled() {
        VersionedPersistentCryptoService legacyService = service(compatibleProperties());
        String legacyCiphertext = legacyService.encrypt("secret-value", "SM4");
        CryptoProperties properties = versionedProperties();
        properties.getPersistence().setLegacyReadEnabled(false);

        VersionedPersistentCryptoService service = service(properties);

        assertThrows(IllegalStateException.class, () -> service.decrypt(legacyCiphertext, "SM4"));
    }

    @Test
    void shouldReencryptLegacyAndKeepActiveIdempotent() {
        VersionedPersistentCryptoService legacyService = service(compatibleProperties());
        String legacyCiphertext = legacyService.encrypt("secret-value", "SM4");
        VersionedPersistentCryptoService service = service(versionedProperties());

        String activeCiphertext = service.reencrypt(legacyCiphertext, "SM4");

        assertTrue(activeCiphertext.startsWith("FPC1:SM4:v2:"));
        assertEquals("secret-value", service.decrypt(activeCiphertext, "SM4"));
        assertEquals(activeCiphertext, service.reencrypt(activeCiphertext, "SM4"));
    }

    @Test
    void shouldReturnNullAndEmptyAsIs() {
        VersionedPersistentCryptoService service = service(versionedProperties());

        assertEquals(null, service.encrypt(null, "SM4"));
        assertEquals("", service.encrypt("", "SM4"));
        assertEquals(null, service.decrypt(null, "SM4"));
        assertEquals("", service.decrypt("", "SM4"));
    }

    private static CryptoProperties compatibleProperties() {
        CryptoProperties properties = new CryptoProperties();
        properties.setSecretKey(LEGACY_KEY);
        properties.getPersistence().setEnabled(true);
        properties.getPersistence().setWriteVersioned(false);
        properties.getPersistence().setLegacyReadEnabled(true);
        properties.getPersistence().setLegacyKey(LEGACY_KEY);
        return properties;
    }

    private static CryptoProperties versionedProperties() {
        CryptoProperties properties = new CryptoProperties();
        properties.setSecretKey(LEGACY_KEY);
        properties.getPersistence().setEnabled(true);
        properties.getPersistence().setWriteVersioned(true);
        properties.getPersistence().setLegacyReadEnabled(true);
        properties.getPersistence().setLegacyKey(LEGACY_KEY);
        properties.getPersistence().setActiveKeyId("v2");
        properties.getPersistence().setActiveKey(ACTIVE_KEY);
        properties.getPersistence().getKeys().put("v1", HISTORY_KEY);
        return properties;
    }

    private static VersionedPersistentCryptoService service(CryptoProperties properties) {
        EncryptorFactory factory = new EncryptorFactory(properties);
        factory.register(new SM4Encryptor(properties));
        factory.register(new AESEncryptor(properties));
        return new VersionedPersistentCryptoService(properties, factory);
    }

    private static String key(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
