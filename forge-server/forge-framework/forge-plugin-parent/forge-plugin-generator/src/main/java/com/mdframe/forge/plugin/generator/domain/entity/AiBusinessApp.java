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
 * 业务应用平台应用入口。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_app")
public class AiBusinessApp extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String appCode;

    private String appName;

    /** BUSINESS/EMBEDDED/MOBILE/INTEGRATION */
    private String appType;

    private String suiteCode;

    private String objectCode;

    /** RUNTIME/ROUTE/IFRAME/EXTERNAL/H5/API */
    private String entryMode;

    private String entryUrl;

    private String configKey;

    private String icon;

    private String description;

    /** 1-启用，0-禁用 */
    private Integer status;

    private Integer sortOrder;

    /** 扩展配置 JSON */
    private String options;

    @TableLogic
    private String delFlag;
}
