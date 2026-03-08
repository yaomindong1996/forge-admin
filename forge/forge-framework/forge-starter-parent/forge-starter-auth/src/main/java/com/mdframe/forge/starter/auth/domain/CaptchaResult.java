package com.mdframe.forge.starter.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 验证码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码key
     */
    private String codeKey;

    /**
     * 验证码（开发环境可返回，生产环境不返回）
     */
    private String code;

    /**
     * 验证码图片（Base64编码）
     */
    private String image;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
}
