package com.mdframe.forge.starter.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.captcha")
public class CaptchaProperties {

    /**
     * 是否在接口响应中回显验证码明文（dev-echo）
     * 默认关闭；只有 dev/local Profile 且显式开启时，图形/短信验证码才会随响应返回 code 字段，
     * 同时短信走本地模拟发送。其他 Profile 即使误配置为 true 也不会生效
     */
    private boolean devEchoCode = false;
}
