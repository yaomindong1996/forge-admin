package com.mdframe.forge.starter.crypto.crypto;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加密器工厂
 */
@Slf4j
public class EncryptorFactory {

    private final Map<CryptoAlgorithm, Encryptor> encryptorMap = new ConcurrentHashMap<>();
    private final CryptoProperties properties;

    public EncryptorFactory(CryptoProperties properties) {
        this.properties = properties;
    }

    /**
     * 注册加密器
     */
    public void register(Encryptor encryptor) {
        encryptorMap.put(encryptor.algorithm(), encryptor);
        log.debug("注册加密器: {}", encryptor.algorithm());
    }

    /**
     * 获取加密器
     *
     * @param algorithm 算法名称，为空时使用默认算法
     */
    public Encryptor getEncryptor(String algorithm) {
        CryptoAlgorithm algo = StrUtil.isNotBlank(algorithm)
                ? CryptoAlgorithm.fromCode(algorithm)
                : CryptoAlgorithm.fromCode(properties.getAlgorithm());
        return getEncryptor(algo);
    }

    /**
     * 获取加密器
     */
    public Encryptor getEncryptor(CryptoAlgorithm algorithm) {
        Encryptor encryptor = encryptorMap.get(algorithm);
        if (encryptor == null) {
            throw new IllegalArgumentException("未找到加密器: " + algorithm);
        }
        return encryptor;
    }

    /**
     * 获取默认加密器
     */
    public Encryptor getDefaultEncryptor() {
        return getEncryptor(properties.getAlgorithm());
    }
}
