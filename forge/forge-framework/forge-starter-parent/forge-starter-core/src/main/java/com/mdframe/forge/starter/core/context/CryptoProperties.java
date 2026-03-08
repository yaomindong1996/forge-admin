package com.mdframe.forge.starter.core.context;

import com.mdframe.forge.starter.core.annotation.config.RefreshScope;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 加密模块配置属性
 */
@Data
@ConfigurationProperties(prefix = "forge.crypto")
@RefreshScope
@Component
public class CryptoProperties {

    /**
     * 是否启用加密功能
     */
    private Boolean enabled = true;

    /**
     * 默认加密算法 (SM4/AES)
     */
    private String algorithm = "SM4";

    /**
     * 对称加密密钥 (Base64编码，16字节)
     * 当启用动态密钥时，此配置作为降级方案
     */
    private String secretKey;

    /**
     * 是否启用动态密钥协商
     */
    private Boolean enableDynamicKey = true;

    /**
     * RSA 公钥（Base64编码，可选，不配置则自动生成）
     */
    private String rsaPublicKey;

    /**
     * RSA 私钥（Base64编码，可选，不配置则自动生成）
     */
    private String rsaPrivateKey;

    /**
     * 会话密钥过期时间（秒），默认2小时
     */
    private Long sessionKeyExpire = 7200L;

    /**
     * 是否启用API级加解密
     */
    private Boolean enableApiCrypto = true;

    /**
     * 是否启用字段级加解密
     */
    private Boolean enableFieldCrypto = true;

    /**
     * 是否启用防重放攻击保护
     */
    private Boolean enableReplayProtection = false;

    /**
     * 防重放时间窗口(秒)
     */
    private Long replayTimeWindow = 300L;

    /**
     * 防重放包含路径
     */
    private List<String> replayIncludePaths = new ArrayList<>();

    /**
     * 防重放排除路径
     */
    private List<String> replayExcludePaths = new ArrayList<>();

    /**
     * API加解密排除路径
     */
    private List<String> excludePaths = new ArrayList<>();
}
