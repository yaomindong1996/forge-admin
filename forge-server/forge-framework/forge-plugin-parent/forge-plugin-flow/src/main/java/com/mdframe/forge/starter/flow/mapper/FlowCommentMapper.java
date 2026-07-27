package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程审批意见 Mapper
 */
@Mapper
public interface FlowCommentMapper extends BaseMapper<FlowComment> {

    int deleteByProcessInstanceIdPhysically(@Param("processInstanceId") String processInstanceId,
                                            @Param("tenantId") Long tenantId);
}
