package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 自定义查询方案。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_custom_query_scheme")
public class CustomQueryScheme extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String configKey;

    private String schemeName;

    private String conditionsJson;

    private String columnsJson;

    private String sortJson;

    private String displayJson;

    private Integer isDefault;

    @TableLogic
    private String delFlag;

    private String remark;
}
