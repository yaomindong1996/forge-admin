package com.mdframe.forge.starter.crypto.crypto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 加密算法枚举
 */
@Getter
@AllArgsConstructor
public enum CryptoAlgorithm {

    SM4("SM4", "国密SM4对称加密"),
    AES("AES", "AES对称加密");

    private final String code;
    private final String desc;

    /**
     * 根据算法名称获取枚举
     */
    public static CryptoAlgorithm fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return SM4;
        }
        for (CryptoAlgorithm algorithm : values()) {
            if (algorithm.getCode().equalsIgnoreCase(code)) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException("不支持的加密算法: " + code);
    }
}
