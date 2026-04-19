package com.mdframe.forge.plugin.ai.session.vo;

import lombok.Data;

@Data
public class DailyTrendItem {

    private String date;
    private Long sessionCount;
    private Long messageCount;
}
