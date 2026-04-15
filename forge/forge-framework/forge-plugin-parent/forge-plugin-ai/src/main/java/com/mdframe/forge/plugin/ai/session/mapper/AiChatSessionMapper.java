package com.mdframe.forge.plugin.ai.session.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {
}
