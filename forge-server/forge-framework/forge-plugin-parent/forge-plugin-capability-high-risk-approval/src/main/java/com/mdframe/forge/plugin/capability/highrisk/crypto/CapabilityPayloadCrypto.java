package com.mdframe.forge.plugin.capability.highrisk.crypto;

public interface CapabilityPayloadCrypto {

    EncryptedCapabilityPayload encrypt(byte[] plaintext, byte[] aad);

    byte[] decrypt(EncryptedCapabilityPayload payload, byte[] aad);
}
