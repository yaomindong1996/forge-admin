package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单据编号规则预览参数。
 */
@Data
public class BusinessDocumentNoRulePreviewDTO {

    private String template;

    private String suiteCode;

    private String objectCode;

    private String starter;

    private String deptCode;

    private Integer sequence;

    private Map<String, Object> sampleData = new LinkedHashMap<>();
}
