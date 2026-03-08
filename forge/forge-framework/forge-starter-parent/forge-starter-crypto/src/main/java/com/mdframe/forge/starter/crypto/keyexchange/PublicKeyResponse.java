package com.mdframe.forge.starter.crypto.keyexchange;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公钥响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyResponse {

    /**
     * RSA公钥（Base64编码）
     */
    private String publicKey;

    /**
     * 加密算法
     */
    private String algorithm;
}
