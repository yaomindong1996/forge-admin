package com.mdframe.forge.plugin.ai.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.resource.domain.AiKnowledgeResourceProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiKnowledgeResourceProfileMapper extends BaseMapper<AiKnowledgeResourceProfile> {
    AiKnowledgeResourceProfile find(@Param("tenant") Long tenant);

    AiKnowledgeResourceProfile locked(@Param("tenant") Long tenant);

    int revise(@Param("row") AiKnowledgeResourceProfile row, @Param("expected") Integer expected);
}
