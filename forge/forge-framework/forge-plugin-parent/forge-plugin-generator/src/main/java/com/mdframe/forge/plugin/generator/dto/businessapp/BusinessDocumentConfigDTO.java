package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务对象单据模式保存参数。
 */
@Data
public class BusinessDocumentConfigDTO {

    private Boolean documentEnabled;

    private String documentName;

    private String documentNoRule;

    private String statusField;

    private String starterField;

    private String ownerField;

    private String defaultFlowKey;

    private Map<String, String> statusMapping = new LinkedHashMap<>();

    private Map<String, Object> options = new LinkedHashMap<>();
}
