package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程分类 Mapper
 */
@Mapper
public interface FlowCategoryMapper extends BaseMapper<FlowCategory> {
}