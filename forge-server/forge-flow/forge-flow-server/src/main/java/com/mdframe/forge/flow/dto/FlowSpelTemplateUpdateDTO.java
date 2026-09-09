package com.mdframe.forge.flow.dto;

import lombok.Data;

/** 表达式模板更新请求。 */
@Data
public class FlowSpelTemplateUpdateDTO {

    private Long id;
    private String templateName;
    private String expression;
    private String description;
    private String category;
    private String exampleParams;
    private Integer status;
    private Integer sort;
    private String remark;
}
