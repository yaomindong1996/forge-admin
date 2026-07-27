package com.mdframe.forge.starter.core.context;

import com.mdframe.forge.starter.core.annotation.config.RefreshScope;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * 防重放排除路径（默认排除验证码相关接口）
     */
    private List<String> replayExcludePaths = new ArrayList<>(List.of(
            "/auth/captcha",
            "/auth/captcha/**",
            "/auth/loginConfig",
            "/crypto/public-key"
    ));

    /**
     * API加解密排除路径
     */
    private List<String> excludePaths = new ArrayList<>();

    /**
     * 是否启用字段脱敏
     */
    private Boolean enableDesensitize = true;

    /**
     * 持久化密文密钥配置。只服务数据库持久化密文，不改变浏览器会话密钥协议。
     */
    private PersistenceProperties persistence = new PersistenceProperties();

    @Data
    public static class PersistenceProperties {

        /**
         * 是否启用持久化密文服务
         */
        private Boolean enabled = true;

        /**
         * 是否写入 FPC1 版本化密文。首次兼容发布默认 false。
         */
        private Boolean writeVersioned = false;

        /**
         * 是否允许读取旧无版本密文
         */
        private Boolean legacyReadEnabled = true;

        /**
         * 新写入使用的活动 keyId
         */
        private String activeKeyId;

        /**
         * 新写入使用的活动密钥，Base64 编码
         */
        private String activeKey;

        /**
         * 旧无版本密文兼容密钥，Base64 编码
         */
        private String legacyKey;

        /**
         * 历史解密密钥，key 为 keyId，value 为 Base64 编码密钥
         */
        private Map<String, String> keys = new LinkedHashMap<>();
    }
}
