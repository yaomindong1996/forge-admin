package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 业务对象运行态信息。
 */
@Data
public class BusinessObjectRuntimeInfoVO {

    private Long objectId;

    private String objectCode;

    private String objectName;

    private Integer objectStatus;

    private Long appId;

    private String appCode;

    private String appName;

    private String configKey;

    private String routePath;

    private Boolean permissionGranted;

    private Boolean canOpen;

    private String message;
}
