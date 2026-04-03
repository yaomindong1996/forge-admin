package com.mdframe.forge.starter.social.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 三方登录平台枚举
 */
@Getter
@AllArgsConstructor
public enum SocialPlatform {

    /**
     * 微信
     */
    WECHAT("WECHAT", "微信"),

    /**
     * 微信开放平台
     */
    WECHAT_OPEN("WECHAT_OPEN", "微信开放平台"),

    /**
     * 微信小程序
     */
    WECHAT_MINI("WECHAT_MINI", "微信小程序"),

    /**
     * 钉钉
     */
    DINGTALK("DINGTALK", "钉钉"),

    /**
     * 企业微信
     */
    WECHAT_ENTERPRISE("WECHAT_ENTERPRISE", "企业微信"),

    /**
     * GitHub
     */
    GITHUB("GITHUB", "GitHub"),

    /**
     * Gitee
     */
    GITEE("GITEE", "Gitee"),

    /**
     * QQ
     */
    QQ("QQ", "QQ"),

    /**
     * 微博
     */
    WEIBO("WEIBO", "微博"),

    /**
     * 支付宝
     */
    ALIPAY("ALIPAY", "支付宝"),

    /**
     * 百度
     */
    BAIDU("BAIDU", "百度"),

    /**
     * 谷歌
     */
    GOOGLE("GOOGLE", "谷歌"),

    /**
     * Facebook
     */
    FACEBOOK("FACEBOOK", "Facebook"),

    /**
     * Twitter
     */
    TWITTER("TWITTER", "Twitter"),

    /**
     * 小米
     */
    XIAOMI("XIAOMI", "小米"),

    /**
     * 华为
     */
    HUAWEI("HUAWEI", "华为"),

    /**
     * 飞书
     */
    FEISHU("FEISHU", "飞书"),

    /**
     * 钉钉企业内部应用
     */
    DINGTALK_ACCOUNT("DINGTALK_ACCOUNT", "钉钉企业内部"),

    /**
     * 自定义平台
     */
    CUSTOM("CUSTOM", "自定义");

    /**
     * 平台编码
     */
    private final String code;

    /**
     * 平台名称
     */
    private final String name;

    /**
     * 根据编码获取平台
     */
    public static SocialPlatform getByCode(String code) {
        for (SocialPlatform platform : values()) {
            if (platform.getCode().equalsIgnoreCase(code)) {
                return platform;
            }
        }
        return null;
    }

    /**
     * 验证平台编码是否有效
     */
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
}
