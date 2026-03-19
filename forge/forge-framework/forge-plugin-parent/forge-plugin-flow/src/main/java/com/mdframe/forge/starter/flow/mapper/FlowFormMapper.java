package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程表单定义Mapper
 * 
 * @author forge
 */
@Mapper
public interface FlowFormMapper extends BaseMapper<FlowForm> {
}