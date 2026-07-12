package com.mdframe.forge.plugin.capability.highrisk.crypto;

public record EncryptedCapabilityPayload(
        String keyId,
        String wrappedDek,
        String iv,
        String ciphertext,
        String authTag) {
}
