package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_crud_config")
public class AiCrudConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
    private Long menuResourceId;
    private String dictConfig;
    private String desensitizeConfig;
    private String encryptConfig;
    private String transConfig;
}
