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

    /**
     * 按租户读取未删除的流程表单实例，供 Flow 服务的 {@code @IgnoreTenant} 入口使用。
     */
    FlowFormInstance selectByProcessInstanceIdAndTenantId(@Param("processInstanceId") String processInstanceId,
                                                           @Param("tenantId") Long tenantId);

    FlowFormInstance selectByBusinessKey(@Param("businessKey") String businessKey);

    /**
     * 按租户读取未删除的业务表单实例。
     */
    FlowFormInstance selectByBusinessKeyAndTenantId(@Param("businessKey") String businessKey,
                                                     @Param("tenantId") Long tenantId);

    int updateProcessInstance(@Param("id") Long id,
                              @Param("processInstanceId") String processInstanceId,
                              @Param("status") String status);

    int updateStatusByProcessInstanceId(@Param("processInstanceId") String processInstanceId,
                                        @Param("status") String status,
                                        @Param("tenantId") Long tenantId);

    int deleteByProcessInstanceIdLogically(@Param("processInstanceId") String processInstanceId,
                                           @Param("tenantId") Long tenantId);
}
