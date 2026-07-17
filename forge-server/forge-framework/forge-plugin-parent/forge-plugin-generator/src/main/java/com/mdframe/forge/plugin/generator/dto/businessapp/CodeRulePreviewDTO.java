package com.mdframe.forge.plugin.generator.dto.businessapp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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

    @Valid
    @Size(max = 32, message = "一条编码规则最多只能包含32个分段")
    private List<CodeRuleSegmentDTO> segments = new ArrayList<>();

    /** 新协议业务字段；只能用于 VARIABLE。 */
    @Size(max = 256, message = "业务上下文字段不能超过256个")
    private Map<String, Object> fields;

    @Size(max = 256, message = "业务上下文字段不能超过256个")
    private Map<String, Object> context;

    @Size(max = 256, message = "业务示例字段不能超过256个")
    private Map<String, Object> sampleData;
}
