package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 单据编号规则内置变量。
 */
@Data
public class BusinessDocumentNoRuleTokenVO {

    private String token;

    private String insertText;

    private String label;

    private String groupName;

    private String description;

    private String example;

    private String sampleValue;
}
