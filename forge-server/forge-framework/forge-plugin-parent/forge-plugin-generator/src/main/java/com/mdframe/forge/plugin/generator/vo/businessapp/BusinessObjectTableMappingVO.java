package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务对象数据库表映射聚合视图。
 */
@Data
public class BusinessObjectTableMappingVO {

    private Long objectId;

    private String objectCode;

    private String objectName;

    private Long datasourceId;

    private String datasourceCode;

    private String datasourceName;

    private String dbType;

    private String tableName;

    private String tableMode;

    private Boolean allowDdl;

    private Boolean readonly;

    private Integer designVersion;

    private Long sharedApplicationCount;

    private Boolean tableExists;

    /** IN_SYNC/OUT_OF_SYNC/TABLE_MISSING/CHECK_FAILED。 */
    private String syncStatus;

    private Integer unsyncedChangeCount;

    private String lastSyncStatus;

    private LocalDateTime lastSyncTime;

    private String lastSyncMessage;

    private List<BusinessObjectTableFieldMappingVO> fields = new ArrayList<>();
}
