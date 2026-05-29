package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务对象自定义操作视图。
 */
@Data
public class BusinessObjectActionVO {

    private String actionCode;

    private String actionName;

    private String actionPosition;

    private String actionType;

    private String permission;

    private Boolean confirmRequired;

    private String successMessage;

    private String failureMessage;

    private Integer status;

    private Integer sortOrder;

    private Map<String, Object> actionConfig = new LinkedHashMap<>();
}
