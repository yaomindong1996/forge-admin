package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowApprovalLevel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程审批层级配置Mapper
 */
@Mapper
public interface FlowApprovalLevelMapper extends BaseMapper<FlowApprovalLevel> {
}