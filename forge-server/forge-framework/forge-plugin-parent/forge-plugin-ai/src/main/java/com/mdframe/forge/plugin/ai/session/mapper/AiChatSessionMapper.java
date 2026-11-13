package com.mdframe.forge.plugin.ai.session.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.dto.AiSessionPageQuery;
import com.mdframe.forge.plugin.ai.session.vo.AiExperienceMetricsVO;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionStatisticsVO;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionVO;
import com.mdframe.forge.plugin.ai.session.vo.DailyTrendItem;
import com.mdframe.forge.plugin.ai.session.vo.ExperienceTrendItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {

    Page<AiSessionVO> selectSessionPage(Page<?> page, @Param("query") AiSessionPageQuery query);

    AiSessionStatisticsVO selectStatistics(@Param("since90") LocalDateTime since90,
                                           @Param("todayStart") LocalDateTime todayStart,
                                           @Param("tomorrowStart") LocalDateTime tomorrowStart);

    List<DailyTrendItem> selectDailySessionTrend(@Param("since") LocalDateTime since);

    List<DailyTrendItem> selectDailyMessageTrend(@Param("since") LocalDateTime since);

    /** 对话体验聚合指标（近 30 天 assistant 回复） */
    AiExperienceMetricsVO selectExperienceMetrics(@Param("since") LocalDateTime since);

    /** 对话体验日趋势（近 14 天） */
    List<ExperienceTrendItem> selectExperienceDailyTrend(@Param("since") LocalDateTime since);
}
