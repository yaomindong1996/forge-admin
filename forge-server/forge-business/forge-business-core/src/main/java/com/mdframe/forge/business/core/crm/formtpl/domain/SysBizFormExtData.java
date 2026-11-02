package com.mdframe.forge.business.core.crm.formtpl.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** 平台/系统对象自定义扩展值（侧表，避免改系统主表结构） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_biz_form_ext_data")
public class SysBizFormExtData extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String objectType;
    private Long bizId;
    private String extJson;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
