package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务对象自定义操作参数。
 */
@Data
public class BusinessObjectActionDTO {

    private String actionCode;

    private String actionName;

    /** toolbar/row/detail */
    private String actionPosition;

    /** openPage/callApi/startApproval/trigger/openExternal */
    private String actionType;

    private String permission;

    private Boolean confirmRequired;

    private String successMessage;

    private String failureMessage;

    private Integer status;

    private Integer sortOrder;

    private Map<String, Object> actionConfig = new LinkedHashMap<>();
}
