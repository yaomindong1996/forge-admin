package com.mdframe.forge.starter.config.config;

import lombok.Data;

/**
 * 登录配置
 */
@Data
public class LoginConfig {

    /**
     * 是否启用登录密码 RSA 加密。
     * 与通用 API 传输加密开关相互独立。
     */
    private Boolean enablePasswordEncryption = true;

    /**
     * 是否启用验证码
     */
    private Boolean enableCaptcha = true;

    /**
     * 验证码类型：graphical(图形验证码), slider(滑块验证码), sms(短信验证码)
     */
    private String captchaType = "graphical";

    /**
     * 是否开放匿名注册。默认关闭。
     */
    private Boolean enableRegister = false;

    /**
     * 是否启用记住我功能
     */
    private Boolean enableRememberMe = true;

    /**
     * 记住我有效天数
     */
    private Integer rememberMeDays = 30;

    /**
     * 是否启用登录日志
     */
    private Boolean enableLoginLog = true;

    /**
     * 是否启用IP限制
     */
    private Boolean enableIpLimit = false;

    /**
     * 允许的IP白名单（逗号分隔）
     */
    private String ipWhitelist = "";

    /**
     * Gitee 社区体验登录总开关。关闭后仍可用账号密码登录。
     */
    private Boolean giteeCommunityEnabled = false;

    /**
     * 是否要求已给指定仓库点 Star。
     */
    private Boolean giteeCommunityRequireStar = true;

    /**
     * 仓库所属空间，例如 ForgeLab。
     */
    private String giteeCommunityOwner = "ForgeLab";

    /**
     * 仓库路径，例如 forge-admin。
     */
    private String giteeCommunityRepo = "forge-admin";

    /**
     * 未 Star 时展示给用户的仓库地址。
     */
    private String giteeCommunityRepoUrl = "https://gitee.com/ForgeLab/forge-admin";

    /**
     * 社区体验租户 ID，需与种子数据一致。
     */
    private Long giteeCommunityTenantId = 9001L;

    /**
     * 社区体验角色编码。
     */
    private String giteeCommunityRoleKey = "gitee_community";

    /**
     * 调用 Gitee API 超时毫秒。
     */
    private Integer giteeCommunityTimeoutMs = 3000;

    /**
     * 是否启用群二维码引流验证码。独立于 enableCaptcha 总开关：
     * 总开关开启时与图形/滑块/短信并存、用户自选；总开关关闭时仅保留群二维码验证码。
     */
    private Boolean groupQrcodeEnabled = false;

    /**
     * 群二维码图片（fileId，登录页通过 /auth/loginQrcode 匿名接口展示，也支持完整 URL）
     */
    private String groupQrcodeImage;

    /**
     * 群名称（显示在二维码下方）
     */
    private String groupQrcodeName;

    /**
     * 群验证码（固定码，管理员定期更新）
     */
    private String groupCaptchaCode;

    /**
     * 引导文案（如"扫码进群获取验证码"）
     */
    private String groupQrcodeHint = "扫码加入用户群，获取验证码并完成登录";

    /**
     * 官网微信交流群看板地址（如 https://xxx/api/community/wechat-groups）。
     * 配置后登录页优先展示看板中的「当前开放群」与历史满员群；未配置则回退本地群二维码图片字段。
     */
    private String groupQrcodeBoardUrl;
}
