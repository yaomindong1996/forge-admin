package com.mdframe.forge.starter.crypto.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加密响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedResponse {

    /**
     * 加密数据（Base64编码）
     */
    private String data;

    /**
     * 加密算法
     */
    private String algorithm;
}
