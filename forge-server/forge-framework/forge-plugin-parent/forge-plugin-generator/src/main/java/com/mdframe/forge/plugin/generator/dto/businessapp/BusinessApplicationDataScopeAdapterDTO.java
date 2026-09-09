package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 应用权限工作台中的对象数据范围字段适配参数。
 */
@Data
public class BusinessApplicationDataScopeAdapterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dataScope;

    private String userField;

    private String orgField;

    private String regionField;

    /**
     * 流程经手人可查看单据。null 视为开启。
     */
    private Boolean flowRelatedVisible;
}
