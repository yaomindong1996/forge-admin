package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户套餐定义（平台级）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package")
public class SysTenantPackage extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String packageCode;

    private String packageName;

    /** 0 禁用 1 启用 */
    private Integer packageStatus;

    /** 0 否 1 是 */
    private Integer isDefault;

    private String description;

    private Integer sort;

    private String remark;

    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
