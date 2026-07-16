package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 编码规则预览结果。
 */
@Data
public class CodeRulePreviewVO {

    private String template;

    private String previewCode;

    private Integer totalLength;

    private String formatExpression;

    private Long sequence;

    private String groupKey;

    private String period;

    private Boolean valid = true;

    private List<String> usedTokens = new ArrayList<>();

    private List<PreviewIssueVO> warnings = new ArrayList<>();

    private List<PreviewIssueVO> errors = new ArrayList<>();

    private List<SegmentPreviewVO> segmentPreviews = new ArrayList<>();

    @Data
    public static class PreviewIssueVO {
        private String token;
        private String message;
        private String suggestion;
    }

    @Data
    public static class SegmentPreviewVO {
        private String segmentKey;
        private Integer segmentOrder;
        private String segmentType;
        private String expression;
        private String value;
        private Boolean included;
        private Boolean grouped;
    }
}
