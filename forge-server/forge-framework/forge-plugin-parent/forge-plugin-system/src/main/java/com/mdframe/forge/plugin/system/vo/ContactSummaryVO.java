package com.mdframe.forge.plugin.system.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 通讯录首页概要
 */
@Data
public class ContactSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantName;

    private Long memberCount;

    /**
     * 当前用户主部门，没有时为空
     */
    private Long myOrgId;

    private String myOrgName;
}
