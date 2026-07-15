package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 业务扩展新增和元数据修改参数。
 */
@Data
public class BusinessExtensionDTO {

    private Long id;

    private Long applicationId;

    private Long objectId;

    private Long entryId;

    private String extensionCode;

    private String extensionName;

    private String extensionType;

    private String hookCode;

    private String scopeType;

    private String scopeKey;

    private Integer sortOrder;

    private String failurePolicy;

    private String riskLevel;

    private String content;

    private String processedContent;

    private String configJson;

    private String changeSummary;

    private String lockToken;

    private String remark;
}
