package com.mdframe.forge.starter.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录配置响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginConfigResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否启用登录密码 RSA 加密。
     */
    private Boolean enablePasswordEncryption;

    /**
     * 是否启用验证码
     */
    private Boolean enableCaptcha;

    /**
     * 验证码类型：graphical(图形验证码), slider(滑块验证码), sms(短信验证码)
     */
    private String captchaType;

    /**
     * 当前解析的客户端编码
     */
    private String userClient;

    /**
     * 验证码类型来源：GLOBAL-全局配置，CLIENT-客户端覆盖
     */
    private String captchaTypeSource;

    /**
     * 全局验证码类型
     */
    private String globalCaptchaType;

    /**
     * 客户端覆盖验证码类型，空表示继承全局
     */
    private String clientCaptchaType;

    /**
     * 是否开放匿名注册
     */
    private Boolean enableRegister;

    /**
     * 已启用的找回密码通道：sms / email。空表示未启用找回。
     */
    private List<String> resetPasswordChannels;

    /**
     * 是否启用记住我功能
     */
    private Boolean enableRememberMe;

    /**
     * 是否启用登录日志
     */
    private Boolean enableLoginLog;

    /**
     * 是否启用IP限制
     */
    private Boolean enableIpLimit;

    /**
     * 当前登录页所选租户ID。
     */
    private Long tenantId;

    /**
     * 当前登录页所选租户名称。
     */
    private String tenantName;

    /**
     * 浏览器icon。
     */
    private String browserIcon;

    /**
     * 浏览器标签名称。
     */
    private String browserTitle;

    /**
     * 系统名称。
     */
    private String systemName;

    /**
     * 系统logo。
     */
    private String systemLogo;

    /**
     * 系统介绍。
     */
    private String systemIntro;

    /**
     * 版权显示文本。
     */
    private String copyrightInfo;

    /**
     * 系统布局。
     */
    private String systemLayout;

    /**
     * 系统主题。
     */
    private String systemTheme;

    /**
     * 主题配置。
     */
    private String themeConfig;

    /**
     * 已启用的三方登录平台列表
     */
    private List<SocialPlatformInfo> socialPlatforms;

    /**
     * 三方平台信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialPlatformInfo implements Serializable {
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
         * 是否启用
         */
        private Boolean enabled;
    }

    /**
     * 是否启用群二维码引流验证码
     */
    private Boolean groupQrcodeEnabled;

    /**
     * 群二维码图片（fileId）
     */
    private String groupQrcodeImage;

    /**
     * 群名称
     */
    private String groupQrcodeName;

    /**
     * 引导文案
     */
    private String groupQrcodeHint;

    /**
     * 官网微信交流群看板地址；非空时登录页通过 /auth/groupQrcodeBoard 拉取开放群。
     */
    private String groupQrcodeBoardUrl;
}
