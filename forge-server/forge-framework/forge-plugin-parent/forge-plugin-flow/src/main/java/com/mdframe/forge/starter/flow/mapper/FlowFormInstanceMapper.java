package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowFormInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程表单实例 Mapper。
 */
@Mapper
public interface FlowFormInstanceMapper extends BaseMapper<FlowFormInstance> {

    FlowFormInstance selectByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    FlowFormInstance selectByBusinessKey(@Param("businessKey") String businessKey);

    int updateProcessInstance(@Param("id") Long id,
                              @Param("processInstanceId") String processInstanceId,
                              @Param("status") String status);

    int updateStatusByProcessInstanceId(@Param("processInstanceId") String processInstanceId,
                                        @Param("status") String status,
                                        @Param("tenantId") Long tenantId);

    int deleteByProcessInstanceIdLogically(@Param("processInstanceId") String processInstanceId,
                                           @Param("tenantId") Long tenantId);
}
