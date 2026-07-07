package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户表实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（租户内唯一）
     */
    private String username;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 用户类型（0-系统管理员，1-租户管理员，2-普通用户）
     */
    private Integer userType;

    /**
     * 用户触点（app/pc/h5/wechat）
     */
    private String userClient;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 性别（0-未知，1-男，2-女）
     */
    private Integer gender;

    /**
     * 密码（加密存储）
     */
    @JsonIgnore
    private String password;

    /**
     * 密码盐值
     */
    @JsonIgnore
    private String salt;

    /**
     * 用户状态（0-禁用，1-正常，2-锁定）
     */
    private Integer userStatus;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 行政区划编码
     */
    private String regionCode;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 登录次数
     */
    private Integer loginCount;

    /**
     * 是否必须先修改密码
     */
    private Boolean forcePasswordChange;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建部门
     */
    private Long createDept;

    /**
     * 删除标志（0正常 1删除）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 所属组织名称（查询列表时填充）
     */
    @TableField(exist = false)
    private String orgName;

    /**
     * 所属租户名称（查询列表时填充）
     */
    @TableField(exist = false)
    private String tenantName;

    /**
     * 所属租户ID列表（查询详情时填充）
     */
    @TableField(exist = false)
    private List<Long> tenantIds;

    /**
     * 所属部门名称（前端选择器兼容字段）
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 用户岗位ID列表（查询详情时填充）
     */
    @TableField(exist = false)
    private List<Long> postIds;

    /**
     * 用户角色ID列表（查询详情时填充）
     */
    @TableField(exist = false)
    private List<Long> roleIds;
}
