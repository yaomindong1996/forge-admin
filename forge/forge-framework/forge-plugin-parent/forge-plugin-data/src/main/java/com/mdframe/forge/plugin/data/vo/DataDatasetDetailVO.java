package com.mdframe.forge.plugin.data.vo;

import com.mdframe.forge.plugin.data.entity.DataDatasetAcl;
import com.mdframe.forge.plugin.data.entity.DataDatasetRowScope;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DataDatasetDetailVO {

    private Long id;

    private String datasetCode;

    private String datasetName;

    private Long connectionId;

    private Long categoryId;

    private String connectionName;

    private String categoryCode;

    private String categoryName;

    private String datasetType;

    private String tableName;

    private String sqlText;

    private String paramSchemaJson;

    private String defaultOrderJson;

    private Integer maxRows;

    private Integer timeoutSeconds;

    private Integer cacheEnabled;

    private Integer cacheTtlSeconds;

    private Integer status;

    private Integer publishStatus;

    private String accessMode;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<DataDatasetFieldVO> fields;

    private List<DataDatasetAcl> aclItems;

    private DataDatasetRowScope rowScope;
}
