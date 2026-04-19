package com.mdframe.forge.plugin.ai.session.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.dto.AiSessionPageQuery;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionStatisticsVO;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionVO;
import com.mdframe.forge.plugin.ai.session.vo.DailyTrendItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {

    Page<AiSessionVO> selectSessionPage(Page<?> page, @Param("query") AiSessionPageQuery query);

    AiSessionStatisticsVO selectStatistics();

    List<DailyTrendItem> selectDailyTrend();
}
