package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 应用入口打开信息。
 */
@Data
public class BusinessAppOpenInfoVO {

    private Long appId;

    private String appCode;

    private String appName;

    private String appType;

    private String entryMode;

    private String openType;

    private String targetUrl;

    private String configKey;

    private String runtimeStatus;

    private String runtimeMessage;

    private Boolean permissionGranted;

    private Boolean canOpen;

    private String message;

    private String nextAction;

    private String nextActionLabel;
}
