package com.mdframe.forge.plugin.generator.vo.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 编码规则详情及列表协议。
 */
@Data
public class CodeRuleDetailVO {

    private Long id;

    private Long tenantId;

    private String ruleCode;

    private String ruleName;

    private String scene;

    private String category;

    private Long sourceObjectId;

    private String sourceObjectCode;

    private String template;

    private Integer status;

    private Integer builtin;

    private Integer inCodeList;

    private Integer versionNo;

    private String remark;

    private Integer segmentCount;

    private String compatibilityMode;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<CodeRuleSegmentDTO> segments = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();
}
