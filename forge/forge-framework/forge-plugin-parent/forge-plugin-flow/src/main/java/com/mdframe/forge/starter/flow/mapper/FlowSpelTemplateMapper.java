package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SPEL表达式模板Mapper
 *
 * @author forge
 */
@Mapper
public interface FlowSpelTemplateMapper extends BaseMapper<FlowSpelTemplate> {

    /**
     * 查询启用状态的模板列表
     *
     * @param tenantId 租户ID
     * @return 模板列表
     */
    List<FlowSpelTemplate> selectEnabledList(@Param("tenantId") Long tenantId);
}