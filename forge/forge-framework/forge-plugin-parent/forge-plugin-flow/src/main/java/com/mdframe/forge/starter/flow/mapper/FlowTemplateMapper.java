package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程模板Mapper
 */
@Mapper
public interface FlowTemplateMapper extends BaseMapper<FlowTemplate> {
}