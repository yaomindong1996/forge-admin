package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扩展中心列表和详情视图。
 */
@Data
public class BusinessExtensionVO {

    private Long id;

    private Long applicationId;

    private Long objectId;

    private String objectCode;

    private String objectName;

    private Long entryId;

    private String entryName;

    private String extensionCode;

    private String extensionName;

    private String extensionType;

    private String hookCode;

    private String scopeType;

    private String scopeKey;

    private Integer sortOrder;

    private String failurePolicy;

    private String riskLevel;

    private String status;

    private Integer draftVersion;

    private Integer enabledVersion;

    private Long lockUserId;

    private String lockUsername;

    private LocalDateTime lockExpireTime;

    private String content;

    private String processedContent;

    private String configJson;

    private Integer validationPassed;

    private String validationSummary;

    private Integer testPassed;

    private String testSummary;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
