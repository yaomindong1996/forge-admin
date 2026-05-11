package com.mdframe.forge.plugin.external.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_external_api")
public class ExternalApi extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
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

    private Long createDept;

    @TableField(exist = false)
    private String systemName;

    @TableField(exist = false)
    private String systemCode;
}
