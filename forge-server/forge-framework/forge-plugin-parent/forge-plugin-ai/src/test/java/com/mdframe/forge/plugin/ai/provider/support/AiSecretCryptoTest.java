package com.mdframe.forge.plugin.ai.provider.support;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.persistence.PersistentCiphertext;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSecretCryptoTest {

    @Mock
    private PersistentCryptoService cryptoService;

    @InjectMocks
    private AiSecretCrypto aiSecretCrypto;

    @Test
    void looksLikeVendorApiKeyDetectsCommonPrefixes() {
        assertTrue(AiSecretCrypto.looksLikeVendorApiKey("sk-live-demo-key-123456"));
        assertTrue(AiSecretCrypto.looksLikeVendorApiKey("ak-demo-key-abcdefgh"));
        assertFalse(AiSecretCrypto.looksLikeVendorApiKey("FPC1:SM4:v1:payload"));
        assertFalse(AiSecretCrypto.looksLikeVendorApiKey(""));
    }

    @Test
    void resolvePlainSecretPassesThroughPlaintextWhenLegacyReadDisabled() {
        String plain = "sk-deepseek-demo-key-123456";
        when(cryptoService.inspect(eq(plain), isNull())).thenReturn(
                new PersistentCiphertext(PersistentCiphertext.Format.LEGACY, "SM4", null, plain, null));
        when(cryptoService.decrypt(eq(plain), isNull()))
                .thenThrow(new BusinessException("旧无版本密文读取已关闭"));

        assertEquals(plain, aiSecretCrypto.resolvePlainSecret(plain));
        verify(cryptoService).decrypt(eq(plain), isNull());
    }

    @Test
    void resolvePlainSecretDecryptsActiveCiphertext() {
        String cipher = "FPC1:SM4:v1:abc";
        when(cryptoService.inspect(eq(cipher), isNull())).thenReturn(
                new PersistentCiphertext(PersistentCiphertext.Format.ACTIVE, "SM4", "v1", "abc", null));
        when(cryptoService.decrypt(eq(cipher), isNull())).thenReturn("sk-real");

        assertEquals("sk-real", aiSecretCrypto.resolvePlainSecret(cipher));
    }

    @Test
    void resolvePlainSecretRejectsUnknownKeyIdWithClearMessage() {
        String cipher = "FPC1:SM4:other:abc";
        when(cryptoService.inspect(eq(cipher), isNull())).thenReturn(
                new PersistentCiphertext(PersistentCiphertext.Format.UNKNOWN_KEY, "SM4", "other", "abc", "未知 keyId"));

        BusinessException ex = assertThrows(BusinessException.class, () -> aiSecretCrypto.resolvePlainSecret(cipher));
        assertTrue(ex.getMessage().contains("重新填写"));
        verify(cryptoService, never()).decrypt(any(), any());
    }

    @Test
    void isEncryptedStillTreatsNonEmptyAsCandidate() {
        assertTrue(AiSecretCrypto.isEncrypted("sk-raw-key"));
        assertFalse(AiSecretCrypto.isEncrypted(null));
        assertFalse(AiSecretCrypto.isEncrypted(""));
    }
}
