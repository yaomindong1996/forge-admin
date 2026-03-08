package com.mdframe.forge.starter.crypto.crypto;

/**
 * 加密器接口
 */
public interface Encryptor {

    /**
     * 使用默认密钥加密
     *
     * @param plainText 明文
     * @return 密文(Base64编码)
     */
    String encrypt(String plainText);

    /**
     * 使用指定密钥加密
     *
     * @param plainText 明文
     * @param key       密钥(Base64编码)
     * @return 密文(Base64编码)
     */
    String encrypt(String plainText, String key);

    /**
     * 使用默认密钥解密
     *
     * @param cipherText 密文(Base64编码)
     * @return 明文
     */
    String decrypt(String cipherText);

    /**
     * 使用指定密钥解密
     *
     * @param cipherText 密文(Base64编码)
     * @param key        密钥(Base64编码)
     * @return 明文
     */
    String decrypt(String cipherText, String key);

    /**
     * 支持的算法类型
     */
    CryptoAlgorithm algorithm();
}
