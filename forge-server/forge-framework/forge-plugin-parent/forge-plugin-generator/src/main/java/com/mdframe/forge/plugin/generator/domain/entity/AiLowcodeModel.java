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
 * 低代码数据模型。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_lowcode_model")
public class AiLowcodeModel extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long domainId;

    private String domainCode;

    private String modelCode;

    private String modelName;

    private String modelDesc;

    /** ENABLED-启用，DISABLED-停用 */
    private String status;

    /** 是否启用多租户 */
    private Boolean tenantEnabled;

    /** 是否主数据模型 */
    private Boolean masterData;

    /** 模型运行数据源ID */
    private Long runtimeDatasourceId;

    /** 模型运行数据源编码 */
    private String runtimeDatasourceCode;

    /** 模型运行表名 */
    private String runtimeTableName;

    /** 表模式：CREATE-在线创建，EXISTING-绑定已有表 */
    private String tableMode;

    /** 模型结构协议 JSON */
    private String modelSchema;

    @TableLogic
    private String delFlag;
}
