package com.mdframe.forge.starter.crypto.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加密请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedRequest {

    /**
     * 加密数据（Base64编码）
     */
    private String data;

    /**
     * 加密算法
     */
    private String algorithm;
}
