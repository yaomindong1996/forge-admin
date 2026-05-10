package com.mdframe.forge.plugin.external.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExternalApiVO {

    private Long id;

    private Long tenantId;

    private Long systemId;

    private String systemCode;

    private String systemName;

    private String apiCode;

    private String apiName;

    private String apiDesc;

    private String apiPath;

    private String apiMethod;

    private String requestContentType;

    private String requestHeaders;

    private String requestParams;

    private String requestBodyTemplate;

    private String responseContentType;

    private String responseDataPath;

    private String responseTotalPath;

    private Boolean paramMappingEnabled;

    private String paramMappings;

    private Boolean responseTransformEnabled;

    private String responseTransformScript;

    private String errorCodePath;

    private String errorMsgPath;

    private String successCodes;

    private Boolean rateLimitEnabled;

    private Integer rateLimitQps;

    private Boolean cacheEnabled;

    private Integer cacheTtl;

    private String cacheKeyTemplate;

    private Boolean permissionCheckEnabled;

    private String requiredPermission;

    private Integer apiStatus;

    private Integer sortOrder;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}