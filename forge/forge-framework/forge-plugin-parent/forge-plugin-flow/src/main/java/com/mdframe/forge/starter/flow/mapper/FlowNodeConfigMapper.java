package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程审批节点配置Mapper
 */
@Mapper
public interface FlowNodeConfigMapper extends BaseMapper<FlowNodeConfig> {
}