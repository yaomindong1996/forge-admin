package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowCc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程抄送 Mapper
 */
@Mapper
public interface FlowCcMapper extends BaseMapper<FlowCc> {

    /**
     * 统计工作台未读抄送数。
     */
    Long countWorkspaceUnread(@Param("userId") String userId);

    int deleteByProcessInstanceIdPhysically(@Param("processInstanceId") String processInstanceId,
                                            @Param("tenantId") Long tenantId);
}
