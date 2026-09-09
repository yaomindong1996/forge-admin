package com.mdframe.forge.starter.flow.vo;

import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import lombok.Data;

import java.util.List;

/** 流程表达式模板分页响应。 */
@Data
public class FlowSpelTemplatePageVO {
    private List<FlowSpelTemplate> records;
    private long total;
    private long page;
    private long pageNum;
    private long pageSize;
}
