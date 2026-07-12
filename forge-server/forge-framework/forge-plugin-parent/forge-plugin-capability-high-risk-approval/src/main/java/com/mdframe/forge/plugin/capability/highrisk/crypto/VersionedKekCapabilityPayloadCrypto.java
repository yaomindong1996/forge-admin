package com.mdframe.forge.plugin.capability.highrisk.crypto;

import com.mdframe.forge.plugin.capability.highrisk.config.HighRiskApprovalProperties;
import com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class VersionedKekCapabilityPayloadCrypto implements CapabilityPayloadCrypto {

    private static final int GCM_TAG_BYTES = 16;
    private static final int GCM_IV_BYTES = 12;

    private final String activeKeyId;
    private final Map<String, SecretKey> keys;
    private final SecureRandom secureRandom = new SecureRandom();

    public VersionedKekCapabilityPayloadCrypto(HighRiskApprovalProperties properties) {
        this.activeKeyId = requireText(properties.getCrypto().getActiveKeyId());
        this.keys = decodeKeys(properties.getCrypto().getKeys());
        if (!keys.containsKey(activeKeyId)) {
            throw new IllegalStateException("高风险审批 activeKeyId 未配置有效 KEK");
        }
    }

    @Override
    public EncryptedCapabilityPayload encrypt(byte[] plaintext, byte[] aad) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256, secureRandom);
            SecretKey dek = generator.generateKey();
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher payloadCipher = Cipher.getInstance("AES/GCM/NoPadding");
            payloadCipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(128, iv));
            payloadCipher.updateAAD(aad);
            byte[] encrypted = payloadCipher.doFinal(plaintext);
            byte[] ciphertext = Arrays.copyOf(encrypted, encrypted.length - GCM_TAG_BYTES);
            byte[] tag = Arrays.copyOfRange(encrypted, encrypted.length - GCM_TAG_BYTES, encrypted.length);

            Cipher wrapper = Cipher.getInstance("AESWrap");
            wrapper.init(Cipher.WRAP_MODE, keys.get(activeKeyId));
            byte[] wrappedDek = wrapper.wrap(dek);
            return new EncryptedCapabilityPayload(activeKeyId, encode(wrappedDek), encode(iv),
                    encode(ciphertext), encode(tag));
        }
        catch (Exception exception) {
            throw new SecureActionUnavailableException("APPROVAL_CRYPTO_UNAVAILABLE", exception);
        }
    }

    @Override
    public byte[] decrypt(EncryptedCapabilityPayload payload, byte[] aad) {
        try {
            SecretKey kek = keys.get(payload.keyId());
            if (kek == null) {
                throw new IllegalStateException("审批快照 keyId 不可用");
            }
            Cipher wrapper = Cipher.getInstance("AESWrap");
            wrapper.init(Cipher.UNWRAP_MODE, kek);
            SecretKey dek = (SecretKey) wrapper.unwrap(
                    decode(payload.wrappedDek()), "AES", Cipher.SECRET_KEY);
            byte[] ciphertext = decode(payload.ciphertext());
            byte[] tag = decode(payload.authTag());
            byte[] combined = new byte[ciphertext.length + tag.length];
            System.arraycopy(ciphertext, 0, combined, 0, ciphertext.length);
            System.arraycopy(tag, 0, combined, ciphertext.length, tag.length);
            Cipher payloadCipher = Cipher.getInstance("AES/GCM/NoPadding");
            payloadCipher.init(Cipher.DECRYPT_MODE, dek,
                    new GCMParameterSpec(128, decode(payload.iv())));
            payloadCipher.updateAAD(aad);
            return payloadCipher.doFinal(combined);
        }
        catch (Exception exception) {
            throw new SecureActionUnavailableException("APPROVAL_PAYLOAD_INVALID", exception);
        }
    }

    private Map<String, SecretKey> decodeKeys(Map<String, String> values) {
        Map<String, SecretKey> result = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((keyId, encoded) -> {
                byte[] key = decode(encoded);
                if (requireText(keyId) == null || key.length != 32) {
                    throw new IllegalStateException("高风险审批 KEK 必须是 Base64 编码的 256-bit 密钥");
                }
                result.put(keyId, new SecretKeySpec(key, "AES"));
            });
        }
        return Map.copyOf(result);
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
