package com.mdframe.forge.plugin.generator.dto.businessapp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编码规则分段保存协议。
 */
@Data
public class CodeRuleSegmentDTO {

    @Size(max = 32, message = "分段键长度不能超过32个字符")
    private String segmentKey;

    @Min(value = 1, message = "分段顺序必须从1开始")
    private Integer segmentOrder;

    @NotBlank(message = "分段类型不能为空")
    private String segmentType;

    @Size(max = 128, message = "分段配置值长度不能超过128个字符")
    private String segmentValue;

    @Min(value = 1, message = "分段长度不能小于1")
    @Max(value = 96, message = "分段长度不能超过96")
    private Integer segmentLength;

    private Integer padEnabled;

    @Size(max = 4, message = "补位字符只能包含一个Unicode字符")
    private String padChar;

    private String padDirection;

    private Integer groupEnabled;

    private Integer includeInCode;

    private String radixType;

    private Integer resetEnabled;

    private String resetPolicy;

    @Min(value = 0, message = "序列起始值不能小于0")
    private Long startValue;

    private Integer excludeAmbiguous;
}
