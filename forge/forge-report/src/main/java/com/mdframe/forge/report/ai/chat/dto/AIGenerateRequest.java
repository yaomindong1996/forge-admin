package com.mdframe.forge.report.ai.chat.dto;

import lombok.Data;

/**
 * AI 生成请求
 */
@Data
public class AIGenerateRequest {
    private String prompt;
    private String style;
    private Integer canvasWidth;
    private Integer canvasHeight;
}
