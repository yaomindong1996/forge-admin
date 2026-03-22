package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 流程业务 Mapper
 */
@Mapper
public interface FlowBusinessMapper extends BaseMapper<FlowBusiness> {
    
    /**
     * 根据流程实例ID查询业务信息
     */
    @Select("SELECT * FROM sys_flow_business WHERE process_instance_id = #{processInstanceId}")
    FlowBusiness selectByProcessInstanceId(String processInstanceId);
    
    /**
     * 根据业务Key查询业务信息
     */
    @Select("SELECT * FROM sys_flow_business WHERE business_key = #{businessKey}")
    FlowBusiness selectByBusinessKey(String businessKey);
}