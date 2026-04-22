package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class AiCrudGenerateRequest {

    private String description;
    private String tableName;
    private String configKey;
    private Long menuParentId;
    private Integer menuSort;
}
