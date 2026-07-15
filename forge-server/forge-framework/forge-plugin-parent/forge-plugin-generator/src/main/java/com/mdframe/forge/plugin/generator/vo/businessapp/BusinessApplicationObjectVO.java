package com.mdframe.forge.plugin.generator.vo.businessapp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用内业务对象关联视图。
 */
@Data
public class BusinessApplicationObjectVO {

    private Long id;

    private Long applicationId;

    private Long objectId;

    private String objectRole;

    private Integer sortOrder;

    private String options;

    private String suiteCode;

    private String objectCode;

    private String objectName;

    private String objectType;

    private String designStatus;

    private String configKey;

    private Integer objectStatus;

    private String datasourceCode;

    private String datasourceName;

    private String tableName;

    private String tableMode;

    private String layoutType;

    private Boolean allowDdl;

    private Boolean readonly;

    private String syncStatus;

    private Integer designVersion;

    private Long sharedApplicationCount;

    @JsonIgnore
    private String designerOptions;

    @JsonIgnore
    private String modelSchema;

    @JsonIgnore
    private String runtimeDatasourceSnapshot;

    private LocalDateTime updateTime;
}
