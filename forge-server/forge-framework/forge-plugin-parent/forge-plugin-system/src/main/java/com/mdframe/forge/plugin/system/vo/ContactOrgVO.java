package com.mdframe.forge.plugin.system.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 通讯录部门
 */
@Data
public class ContactOrgVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orgName;

    /**
     * 含下级部门的有效成员数
     */
    private Long memberCount;
}
