package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 编码生成结果。
 */
@Data
public class CodeRuleGenerateVO {

    private String code;

    private Long sequence;

    /** 分组值摘要，不返回原始业务字段。 */
    private String groupKey;

    private String period;
}
