package com.mdframe.forge.plugin.data.dto;

import lombok.Data;

@Data
public class DataDatasetRowScopeDTO {

    private Long id;

    private Integer enabled;

    private String scopeMode;

    private String tenantColumn;

    private String orgColumn;

    private String userColumn;

    private String regionColumn;

    private String regionStrategy;

    private String remark;
}
