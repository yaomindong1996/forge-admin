package com.mdframe.forge.plugin.print.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台打印业务来源。敏感连接信息只保存服务端受管引用，不在本表保存凭据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_print_business_source")
public class PrintBusinessSource extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String sourceCode;

    private String sourceName;

    private String sourceType;

    private String providerCode;

    private Long datasetId;

    private String objectCode;

    private String parameterSchemaJson;

    private String mappingJson;

    private String catalogJson;

    private String catalogHash;

    private Long sourceRevision;

    private Integer status;

    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
