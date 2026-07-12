package com.mdframe.forge.plugin.ai.model.capability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.model.capability.domain.AiModelCapability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AiModelCapabilityMapper extends BaseMapper<AiModelCapability> {
    List<AiModelCapability> selectEnabledByModelIds(@Param("modelIds") Collection<Long> modelIds);
    int logicallyDeleteByModelId(@Param("modelId") Long modelId);
}
