package com.mdframe.forge.starter.crypto.crypto.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.crypto.crypto.CryptoAlgorithm;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 国密SM4加密器实现
 */
@Slf4j
public class SM4Encryptor implements Encryptor {

    private final SM4 defaultSm4;
    private final CryptoProperties properties;

    public SM4Encryptor(CryptoProperties properties) {
        this.properties = properties;
        // 如果配置了默认密钥，则初始化默认加密器
        if (properties.getSecretKey() != null && !properties.getSecretKey().isEmpty()) {
            byte[] keyBytes = Base64.decode(properties.getSecretKey());
            if (keyBytes.length != 16) {
                throw new IllegalArgumentException("SM4密钥长度必须为16字节");
            }
            this.defaultSm4 = SmUtil.sm4(keyBytes);
        } else {
            this.defaultSm4 = null;
        }
        log.info("SM4加密器初始化完成");
    }

    @Override
    public String encrypt(String plainText) {
        if (defaultSm4 == null) {
            throw new IllegalStateException("默认密钥未配置");
        }
        return doEncrypt(plainText, defaultSm4);
    }

    @Override
    public String encrypt(String plainText, String key) {
        if (key == null || key.isEmpty()) {
            return encrypt(plainText);
        }
        SM4 sm4 = createSm4(key);
        return doEncrypt(plainText, sm4);
    }

    @Override
    public String decrypt(String cipherText) {
        if (defaultSm4 == null) {
            throw new IllegalStateException("默认密钥未配置");
        }
        return doDecrypt(cipherText, defaultSm4);
    }

    @Override
    public String decrypt(String cipherText, String key) {
        if (key == null || key.isEmpty()) {
            return decrypt(cipherText);
        }
        SM4 sm4 = createSm4(key);
        return doDecrypt(cipherText, sm4);
    }

    @Override
    public CryptoAlgorithm algorithm() {
        return CryptoAlgorithm.SM4;
    }

    private SM4 createSm4(String base64Key) {
        byte[] keyBytes = Base64.decode(base64Key);
        if (keyBytes.length != 16) {
            throw new IllegalArgumentException("SM4密钥长度必须为16字节");
        }
        return SmUtil.sm4(keyBytes);
    }

    private String doEncrypt(String plainText, SM4 sm4) {
        if (plainText == null) {
            return null;
        }
        try {
            return sm4.encryptBase64(plainText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("SM4加密失败", e);
            throw new RuntimeException("SM4加密失败", e);
        }
    }

    private String doDecrypt(String cipherText, SM4 sm4) {
        if (cipherText == null) {
            return null;
        }
        try {
            return sm4.decryptStr(cipherText);
        } catch (Exception e) {
            log.error("SM4解密失败", e);
            throw new RuntimeException("SM4解密失败", e);
        }
    }
}
