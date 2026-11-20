package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户套餐组成项。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package_item")
public class SysTenantPackageItem extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long packageId;

    /** BOOTSTRAP / RESOURCE / QUOTA */
    private String itemType;

    private String itemCode;

    private String itemValue;

    private Integer sort;

    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
