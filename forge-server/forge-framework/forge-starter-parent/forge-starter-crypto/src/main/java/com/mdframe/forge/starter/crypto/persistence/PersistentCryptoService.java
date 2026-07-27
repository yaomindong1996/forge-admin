package com.mdframe.forge.starter.crypto.persistence;

/**
 * 数据库持久化密文服务。
 */
public interface PersistentCryptoService {

    String encrypt(String plaintext, String algorithm);

    String decrypt(String ciphertext, String legacyAlgorithm);

    PersistentCiphertext inspect(String ciphertext, String legacyAlgorithm);

    String reencrypt(String ciphertext, String legacyAlgorithm);
}
