package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务权限摘要 VO。
 */
@Data
public class BusinessPermissionSummaryVO {

    private Long objectId;

    private String objectCode;

    private String objectName;

    private Boolean allRequiredConfigured;

    private List<ActionPermissionVO> actionPermissions = new ArrayList<>();

    @Data
    public static class ActionPermissionVO {

        private String actionCode;

        private String actionName;

        private List<String> permissionCodes = new ArrayList<>();

        private Boolean configured;

        private Boolean granted;

        private Boolean required;
    }
}
