package com.mdframe.forge.starter.social.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 三方平台配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialPlatformInfo implements Serializable {

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
     * 平台Logo
     */
    private String platformLogo;

    /**
     * 平台Logo的Base64编码（用于前端无需认证的场景）
     */
    private String platformLogoBase64;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
