package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户套餐开通快照。grant_status 首期仅 ACTIVE，二期可扩展 CHANGED/EXPIRED。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package_grant")
public class SysTenantPackageGrant extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long packageId;

    private String packageCode;

    private String snapshotJson;

    /** ACTIVE；二期：CHANGED / EXPIRED */
    private String grantStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime grantedAt;

    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
