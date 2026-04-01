package com.mdframe.forge.starter.social.context;

import com.mdframe.forge.starter.core.annotation.config.RefreshScope;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 三方登录配置属性
 */
@Data
@ConfigurationProperties(prefix = "forge.social")
@RefreshScope
@Component
public class SocialProperties {

    /**
     * 是否启用三方登录
     */
    private Boolean enabled = true;

    /**
     * 默认回调地址前缀
     */
    private String callbackPrefix = "";

    /**
     * 是否自动注册新用户
     */
    private Boolean autoRegister = true;

    /**
     * 新用户默认角色ID列表
     */
    private Long[] defaultRoleIds = new Long[0];
}
