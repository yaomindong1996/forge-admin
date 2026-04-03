package com.mdframe.forge.starter.social.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 三方授权链接响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAuthUrl implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 平台类型
     */
    private String platform;

    /**
     * 平台名称
     */
    private String platformName;

    /**
     * 授权链接
     */
    private String authUrl;

    /**
     * 状态码
     */
    private String state;
}
