package com.mdframe.forge.plugin.generator.dto.businessapp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化编码规则新增/修改协议。
 */
@Data
public class CodeRuleSaveDTO {

    private Long id;

    private Integer versionNo;

    @NotBlank(message = "规则编码不能为空")
    @Size(max = 64, message = "规则编码长度不能超过64个字符")
    private String ruleCode;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称长度不能超过128个字符")
    private String ruleName;

    @Size(max = 64, message = "适用场景长度不能超过64个字符")
    private String scene;

    @NotBlank(message = "编码分类不能为空")
    @Size(max = 64, message = "编码分类长度不能超过64个字符")
    private String category;

    /** VARIABLE 分段映射的低代码业务对象。 */
    private Long sourceObjectId;

    /** 由服务端根据 sourceObjectId 回填，禁止信任前端自报。 */
    @Size(max = 64, message = "业务对象编码长度不能超过64个字符")
    private String sourceObjectCode;

    private Integer status;

    private Integer inCodeList;

    @Size(max = 500, message = "规则说明长度不能超过500个字符")
    private String remark;

    @Valid
    @NotEmpty(message = "编码规则至少需要一个分段")
    private List<CodeRuleSegmentDTO> segments = new ArrayList<>();
}
