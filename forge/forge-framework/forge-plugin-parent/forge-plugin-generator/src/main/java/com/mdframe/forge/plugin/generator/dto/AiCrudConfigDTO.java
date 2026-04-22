package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class AiCrudConfigDTO {

    private Long id;
    private String configKey;
    private String tableName;
    private String tableComment;
    private String searchSchema;
    private String columnsSchema;
    private String editSchema;
    private String apiConfig;
    private String options;
    private String mode;
    private String status;
    private String menuName;
    private Long menuParentId;
    private Integer menuSort;
    private String dictConfig;
    private String desensitizeConfig;
    private String encryptConfig;
    private String transConfig;
}
