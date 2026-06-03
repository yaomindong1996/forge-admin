package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单据编号规则预览结果。
 */
@Data
public class BusinessDocumentNoRulePreviewVO {

    private String template;

    private String previewNo;

    private Boolean valid;

    private List<String> usedTokens = new ArrayList<>();

    private List<PreviewIssueVO> errors = new ArrayList<>();

    private List<PreviewIssueVO> warnings = new ArrayList<>();

    @Data
    public static class PreviewIssueVO {

        private String token;

        private String message;

        private String suggestion;
    }
}
