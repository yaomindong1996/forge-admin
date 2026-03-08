package com.mdframe.forge.starter.crypto.keyexchange;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * RSA 密钥对持有者
 * 用于管理服务端的 RSA 公钥/私钥对
 */
@Slf4j
public class RsaKeyPairHolder {

    private final RSA rsa;

    @Getter
    private final String publicKeyBase64;

    @Getter
    private final String privateKeyBase64;

    public RsaKeyPairHolder() {
        this.rsa = new RSA();
        this.publicKeyBase64 = Base64.getEncoder().encodeToString(rsa.getPublicKey().getEncoded());
        this.privateKeyBase64 = Base64.getEncoder().encodeToString(rsa.getPrivateKey().getEncoded());
        log.info("RSA 密钥对初始化完成");
    }

    public RsaKeyPairHolder(String publicKeyBase64, String privateKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
        this.privateKeyBase64 = privateKeyBase64;
        this.rsa = new RSA(privateKeyBase64, publicKeyBase64);
        log.info("RSA 密钥对从配置加载完成");
    }

    /**
     * 使用私钥解密数据
     * @param encryptedBase64 Base64编码的加密数据
     * @return 解密后的字符串
     */
    public String decryptByPrivateKey(String encryptedBase64) {
        byte[] decrypted = rsa.decrypt(Base64.getDecoder().decode(encryptedBase64), KeyType.PrivateKey);
        return new String(decrypted);
    }
    
    public String decryptByPrivateKeyNoBase64(String encrypted) {
        byte[] decrypted = rsa.decrypt(encrypted, KeyType.PrivateKey);
        return new String(decrypted);
    }

    /**
     * 使用公钥加密数据
     * @param data 原始数据
     * @return Base64编码的加密数据
     */
    public String encryptByPublicKey(String data) {
        byte[] encrypted = rsa.encrypt(data.getBytes(), KeyType.PublicKey);
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
