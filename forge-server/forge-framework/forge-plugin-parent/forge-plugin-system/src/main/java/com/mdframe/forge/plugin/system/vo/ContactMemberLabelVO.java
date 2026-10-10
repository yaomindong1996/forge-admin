package com.mdframe.forge.plugin.system.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 成员的部门名或岗位名（批量查询结果行）
 */
@Data
public class ContactMemberLabelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String label;
}
