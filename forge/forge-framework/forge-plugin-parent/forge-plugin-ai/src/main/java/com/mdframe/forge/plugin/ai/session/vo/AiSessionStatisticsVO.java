package com.mdframe.forge.plugin.ai.session.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiSessionStatisticsVO {

    private Long totalSessions;
    private Long totalMessages;
    private Long todaySessions;
    private Long totalTokenUsage;
    private List<DailyTrendItem> dailyTrend;
}
