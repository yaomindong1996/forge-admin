package com.mdframe.forge.employee.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工信息表新增/修改DTO
 * 
 * @author Forge Generator
 * @date 2026-04-26 19:08:42
 */
@Data
public class EmployeeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 姓名
     */
    private String empName;

    /**
     * 工号
     */
    private String empNo;

    /**
     * 部门ID
     */
    private Long deptId;

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
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态(1:正常,0:停用)
     */
    private Integer status;

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
    private Integer delFlag;

}
