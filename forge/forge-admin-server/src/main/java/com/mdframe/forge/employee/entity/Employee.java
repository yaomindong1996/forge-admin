package com.mdframe.forge.employee.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import com.mdframe.forge.starter.trans.annotation.DictTrans;
import com.mdframe.forge.starter.trans.annotation.TransField;
import com.mdframe.forge.starter.crypto.desensitize.annotation.Desensitize;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 员工信息表实体类
 *
 * @author Forge Generator
 * @date 2026-04-26 19:08:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_employee")
@DictTrans
public class Employee extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 姓名
     */
    @Desensitize(type = DesensitizeType.NAME)
    private String empName;

    /**
     * 工号
     */
    private String empNo;

    /**
     * 部门ID
     */
    @TransField(dictType = "sys_dept")
    private Long deptId;
    
    @TableField(exist = false)
    private String deptIdName;

    /**
     * 职位
     */
    private String position;

    /**
     * 入职日期
     */
    private LocalDateTime hireDate;

    /**
     * 手机号
     */
    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    /**
     * 邮箱
     */
    @Desensitize(type = DesensitizeType.EMAIL)
    private String email;

    /**
     * 状态(1:正常,0:停用)
     */
    @TransField(dictType = "sys_normal_disable")
    private Integer status;
    
    @TableField(exist = false)
    private String statusName;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 创建部门
     */
    private Long createDept;

    /**
     * 删除标志(0:未删除,1:已删除)
     */
    @TransField(dictType = "del_flag")
    private Integer delFlag;

    @TableField(exist = false)
    private String delFlagName;

}
