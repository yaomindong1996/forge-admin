package com.mdframe.forge.plugin.external.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_external_api_log")
public class ExternalApiLog extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long systemId;

    private Long apiId;

    private String systemName;

    private String apiName;

    private String apiCode;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String requestBody;

    private String responseBody;

    private Integer httpStatusCode;

    private Integer callStatus;

    private String errorMessage;

    private Long durationMs;

    private Boolean debugFlag;
}
