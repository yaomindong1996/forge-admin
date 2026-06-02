package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务应用平台-业务单据配置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_document_config")
public class AiBusinessDocumentConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long objectId;

    private String suiteCode;

    private String objectCode;

    private String configKey;

    private String documentName;

    private String documentNoRule;

    /** 1-启用单据模式，0-普通对象 */
    private Integer documentEnabled;

    private String statusField;

    private String starterField;

    private String ownerField;

    private String defaultFlowKey;

    /** 单据状态映射 JSON */
    private String statusMapping;

    /** 扩展配置 JSON */
    private String options;
}
