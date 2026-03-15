package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程审批意见 Mapper
 */
@Mapper
public interface FlowCommentMapper extends BaseMapper<FlowComment> {
}