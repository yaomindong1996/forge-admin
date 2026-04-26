package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class AiPageTemplateDTO {

    private Long id;
    private String templateKey;
    private String templateName;
    private String description;
    private String icon;
    private String systemPrompt;
    private String schemaHint;
    private String defaultConfig;
    private Integer enabled;
    private Integer sort;
    /** 代码生成策略类型：TEMPLATE（默认） / AI */
    private String codegenType;
}
