package com.mdframe.forge.plugin.external.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_external_system")
public class ExternalSystem extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String systemCode;

    private String systemName;

    private String systemDesc;

    private String baseUrl;

    private String authType;

    private String basicUsername;

    private String basicPassword;

    private String tokenValue;

    private String tokenHeaderName;

    private String tokenPrefix;

    private String oauth2TokenUrl;

    private String oauth2ClientId;

    private String oauth2ClientSecret;

    private String oauth2GrantType;

    private String oauth2Scope;

    private String apiKeyName;

    private String apiKeyValue;

    private String apiKeyPosition;

    private String customAuthAdapter;

    private String customAuthConfig;

    private Boolean trustedInternal;

    private Boolean proxyEnabled;

    private String proxyHost;

    private Integer proxyPort;

    private String proxyUsername;

    private String proxyPassword;

    private Boolean retryEnabled;

    private Integer retryMaxAttempts;

    private Integer retryBackoffInterval;

    private Integer connectTimeout;

    private Integer readTimeout;

    private Integer writeTimeout;

    private Boolean sslVerifyEnabled;

    private Boolean requestLoggingEnabled;

    private Integer systemStatus;

    private String remark;

    private Long createDept;

    @TableField(exist = false)
    private Integer apiCount;
}
