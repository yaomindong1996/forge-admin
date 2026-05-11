package com.mdframe.forge.plugin.external.dto;

import lombok.Data;

@Data
public class ExternalApiDTO {

    private Long id;

    private Long systemId;

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

    private String docFileId;

    private String docFileName;

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
}
