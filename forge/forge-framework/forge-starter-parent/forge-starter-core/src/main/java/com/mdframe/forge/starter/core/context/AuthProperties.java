package com.mdframe.forge.starter.core.context;

import com.mdframe.forge.starter.core.annotation.config.RefreshScope;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证授权配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.auth")
@RefreshScope
public class AuthProperties {

    /**
     * 是否启用API接口权限校验
     * 默认启用
     */
    private Boolean enableApiPermission = true;

    /**
     * API权限校验排除路径（支持通配符）
     */
    private String[] apiPermissionExcludePaths = new String[]{
            "/auth/**"
    };

    /**
     * 是否启用登录失败锁定功能
     * 默认启用
     */
    private Boolean enableLoginLock = true;

    /**
     * 最大登录失败尝试次数
     * 默认4次，可通过 sys_config 配置覆盖
     */
    private Integer maxLoginAttempts = 4;

    /**
     * 账号锁定时长（分钟）
     * 默认30分钟，可通过 sys_config 配置覆盖
     */
    private Long lockDuration = 30L;

    /**
     * 登录失败记录保留时长（分钟）
     * 在此时间内的失败次数会累计
     * 默认15分钟
     */
    private Long failRecordExpire = 15L;

    /**
     * 同一账号登录策略
     * - allow_concurrent: 允许并发登录
     * - replace_old: 新登录踢出旧登录(默认)
     * - reject_new: 拒绝新登录
     */
    private String sameAccountLoginStrategy = "replace_old";

    /**
     * 是否启用在线用户管理
     * 默认启用
     */
    private Boolean enableOnlineUserManagement = true;

    /**
     * 是否启用客户端验证
     * 启用后，登录时需要验证AppId和AppSecret
     * 默认不启用
     */
    private Boolean enableClientValidation = true;
}
