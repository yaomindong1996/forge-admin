package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 通讯录成员查询条件
 */
@Data
public class ContactMemberQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 只查该部门的直属成员；为空时查全租户
     */
    private Long orgId;

    /**
     * 匹配姓名或账号
     */
    private String keyword;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
