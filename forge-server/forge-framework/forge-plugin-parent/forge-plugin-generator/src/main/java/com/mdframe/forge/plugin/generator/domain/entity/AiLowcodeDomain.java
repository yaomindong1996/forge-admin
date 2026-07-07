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
 * 低代码业务领域。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_lowcode_domain")
public class AiLowcodeDomain extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;

    private String domainCode;

    private String domainName;

    private String domainDesc;

    private String icon;

    private Integer sort;

    /** ENABLED-启用，DISABLED-停用 */
    private String status;

    private Long menuParentId;

    private String tablePrefix;

    private String configKeyPrefix;

    private String defaultAppType;

    private String defaultLayoutType;

    private String defaultTableMode;

    /** 领域扩展协议 JSON */
    private String domainSchema;

    @TableLogic
    private String delFlag;
}
