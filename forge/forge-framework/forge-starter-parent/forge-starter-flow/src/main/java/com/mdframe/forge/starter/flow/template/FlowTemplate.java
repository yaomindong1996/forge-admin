package com.mdframe.forge.starter.flow.template;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 流程模板定义
 */
@Data
@Accessors(chain = true)
public class FlowTemplate {

    /**
     * 模板标识
     */
    private String templateKey;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 分类
     */
    private String category;

    /**
     * 描述
     */
    private String description;

    /**
     * BPMN XML 内容
     */
    private String bpmnXml;

    /**
     * 默认标题模板
     */
    private String titleTemplate;

    /**
     * 表单类型
     */
    private String formType;

    /**
     * 流程变量定义
     */
    private String variables;
}