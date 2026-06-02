package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务对象单据模式配置视图。
 */
@Data
public class BusinessDocumentConfigVO {

    private Long id;

    private Long objectId;

    private String suiteCode;

    private String objectCode;

    private String configKey;

    private Boolean documentEnabled;

    private String documentName;

    private String documentNoRule;

    private String statusField;

    private String starterField;

    private String ownerField;

    private String defaultFlowKey;

    private Map<String, String> statusMapping = new LinkedHashMap<>();

    private Map<String, Object> options = new LinkedHashMap<>();

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
