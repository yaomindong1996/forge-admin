package com.mdframe.forge.plugin.ai.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型 Mapper
 */
@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {
}
