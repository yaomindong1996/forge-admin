package com.mdframe.forge.starter.crypto.keyexchange;

import lombok.Data;

/**
 * 密钥交换请求
 */
@Data
public class KeyExchangeRequest {

    /**
     * RSA公钥加密后的会话密钥（Base64编码）
     */
    private String encryptedKey;
}
