package com.mdframe.forge.plugin.capability.identity.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_capability_oauth_redirect_uri")
public class AiCapabilityOAuthRedirectUri extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private Long clientId;
    private String redirectUri;
    private String redirectUriHash;
    private String status;
    @TableLogic
    private Integer delFlag;
}
