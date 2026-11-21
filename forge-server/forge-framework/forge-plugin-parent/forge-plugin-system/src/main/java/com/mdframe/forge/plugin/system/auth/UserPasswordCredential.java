package com.mdframe.forge.plugin.system.auth;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户密码凭证快照：只包含密码策略需要的列。
 */
@Data
public class UserPasswordCredential {

    private Long userId;

    /** 凭证归属租户（sys_user.tenant_id），密码历史按该租户存放。 */
    private Long tenantId;

    /** 当前密码哈希 */
    private String password;

    /** 最近一次密码变更时间 */
    private LocalDateTime passwordChangedTime;
}
