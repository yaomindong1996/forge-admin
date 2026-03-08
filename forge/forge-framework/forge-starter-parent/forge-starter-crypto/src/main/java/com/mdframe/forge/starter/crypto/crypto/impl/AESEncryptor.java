package com.mdframe.forge.starter.crypto.crypto.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.crypto.crypto.CryptoAlgorithm;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * AES加密器实现
 */
@Slf4j
public class AESEncryptor implements Encryptor {

    private final AES defaultAes;
    private final CryptoProperties properties;

    public AESEncryptor(CryptoProperties properties) {
        this.properties = properties;
        // 如果配置了默认密钥，则初始化默认加密器
        if (properties.getSecretKey() != null && !properties.getSecretKey().isEmpty()) {
            byte[] keyBytes = Base64.decode(properties.getSecretKey());
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new IllegalArgumentException("AES密钥长度必须为16/24/32字节");
            }
            this.defaultAes = SecureUtil.aes(keyBytes);
        } else {
            this.defaultAes = null;
        }
        log.info("AES加密器初始化完成");
    }

    @Override
    public String encrypt(String plainText) {
        if (defaultAes == null) {
            throw new IllegalStateException("默认密钥未配置");
        }
        return doEncrypt(plainText, defaultAes);
    }

    @Override
    public String encrypt(String plainText, String key) {
        if (key == null || key.isEmpty()) {
            return encrypt(plainText);
        }
        AES aes = createAes(key);
        return doEncrypt(plainText, aes);
    }

    @Override
    public String decrypt(String cipherText) {
        if (defaultAes == null) {
            throw new IllegalStateException("默认密钥未配置");
        }
        return doDecrypt(cipherText, defaultAes);
    }

    @Override
    public String decrypt(String cipherText, String key) {
        if (key == null || key.isEmpty()) {
            return decrypt(cipherText);
        }
        AES aes = createAes(key);
        return doDecrypt(cipherText, aes);
    }

    @Override
    public CryptoAlgorithm algorithm() {
        return CryptoAlgorithm.AES;
    }

    private AES createAes(String base64Key) {
        byte[] keyBytes = Base64.decode(base64Key);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("AES密钥长度必须为16/24/32字节");
        }
        return SecureUtil.aes(keyBytes);
    }

    private String doEncrypt(String plainText, AES aes) {
        if (plainText == null) {
            return null;
        }
        try {
            return aes.encryptBase64(plainText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("AES加密失败", e);
        }
    }

    private String doDecrypt(String cipherText, AES aes) {
        if (cipherText == null) {
            return null;
        }
        try {
            return aes.decryptStr(cipherText);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new RuntimeException("AES解密失败", e);
        }
    }
}
