package com.mdframe.forge.flow.dto;

import lombok.Data;

/** 表达式模板创建请求。 */
@Data
public class FlowSpelTemplateCreateDTO {

    private String templateName;
    private String templateCode;
    private String expression;
    private String description;
    private String category;
    private String exampleParams;
    private Integer status;
    private Integer sort;
    private String remark;
}
