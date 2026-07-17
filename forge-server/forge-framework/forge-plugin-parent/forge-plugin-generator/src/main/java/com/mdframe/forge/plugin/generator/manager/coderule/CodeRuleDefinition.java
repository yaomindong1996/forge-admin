package com.mdframe.forge.plugin.generator.manager.coderule;

import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 编码规则引擎的无持久化定义。
 */
@Data
public class CodeRuleDefinition {

    private Long tenantId;

    private Long ruleId;

    private String ruleCode;

    private String ruleName;

    private Integer legacyCompatEnabled;

    private List<CodeRuleSegmentDTO> segments = new ArrayList<>();
}
