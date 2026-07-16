package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 编码规则预览请求。
 */
@Data
public class CodeRulePreviewDTO {

    private String ruleCode;

    private String template;

    private Integer sequence;

    private Long id;

    private String ruleName;

    private String category;

    private List<CodeRuleSegmentDTO> segments = new ArrayList<>();

    /** 新协议业务字段；只能用于 VARIABLE。 */
    private Map<String, Object> fields;

    private Map<String, Object> context;

    private Map<String, Object> sampleData;
}
