package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 低代码业务扩展身份与治理状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_extension")
public class AiBusinessExtension extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long applicationId;

    private Long objectId;

    private Long entryId;

    private String extensionCode;

    private String extensionName;

    private String extensionType;

    private String hookCode;

    private String scopeType;

    private String scopeKey;

    private Integer sortOrder;

    private String failurePolicy;

    private String riskLevel;

    private String status;

    private Integer draftVersion;

    private Integer enabledVersion;

    private Long lockUserId;

    private String lockUsername;

    private String lockTokenHash;

    private LocalDateTime lockTime;

    private LocalDateTime lockExpireTime;

    private String remark;

    @TableLogic
    private String delFlag;
}
