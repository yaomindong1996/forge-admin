package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务单据发起流程参数。
 */
@Data
public class BusinessFlowStartDTO {

    private String objectCode;

    private Long recordId;

    private String flowModelKey;

    private String title;

    private Map<String, Object> variables = new LinkedHashMap<>();
}
