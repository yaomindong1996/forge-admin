package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import com.mdframe.forge.starter.trans.annotation.DictTrans;
import com.mdframe.forge.starter.trans.annotation.TransField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 组织表实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
@DictTrans
public class SysOrg extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 组织ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 父级组织ID（0为顶级）
     */
    private Long parentId;

    /**
     * 祖级编码（逗号分隔，如：1,2,3）
     */
    private String ancestors;

    /**
     * 排序（值越小越靠前）
     */
    private Integer sort;

    @TransField(dictType = "sys_org_type")
    private String orgType;

    @TableField(exist = false)
    private String orgTypeName;

    @TransField(dictType = "sys_normal_disable")
    private Integer orgStatus;

    @TableField(exist = false)
    private String orgStatusName;

    /**
     * 负责人ID（关联sys_user.id）
     */
    private Long leaderId;

    /**
     * 负责人姓名
     */
    private String leaderName;

    /**
     * 组织联系电话
     */
    private String phone;

    /**
     * 组织地址
     */
    private String address;

    /**
     * 备注
     */
    private String remark;

    /**
     * 行政区划编码
     */
    private String regionCode;

    /**
     * 删除标志：0-正常 1-删除
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 子组织列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysOrg> children;
}
