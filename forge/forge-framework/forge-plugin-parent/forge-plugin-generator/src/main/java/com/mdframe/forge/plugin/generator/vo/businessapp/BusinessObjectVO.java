package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务对象视图。
 */
@Data
public class BusinessObjectVO {

    private Long id;

    private String suiteCode;

    private String suiteName;

    private String objectCode;

    private String objectName;

    private String objectType;

    private Long modelId;

    private String modelCode;

    private String displayField;

    private String icon;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private String options;

    private String designStatus;

    private String configKey;

    private LocalDateTime lastPublishTime;

    private Integer lastPublishVersion;

    private String designerOptions;

    private Long relationCount;

    private Long bindingCount;

    private Long appCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
