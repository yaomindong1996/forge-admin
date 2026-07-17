package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 前端分段编辑器使用的服务端能力描述。
 */
@Data
public class CodeRuleCapabilityVO {

    private List<OptionVO> segmentTypes = new ArrayList<>();

    private List<OptionVO> dateFormats = new ArrayList<>();

    private List<OptionVO> radixTypes = new ArrayList<>();

    private List<OptionVO> resetPolicies = new ArrayList<>();

    private List<OptionVO> systemVariables = new ArrayList<>();

    private List<OptionVO> variableSources = new ArrayList<>();

    /** 当前租户可用于规则字段映射的低代码业务对象。 */
    private List<OptionVO> businessObjects = new ArrayList<>();

    /** 本次 sourceObjectId 对应的可用业务字段。 */
    private List<OptionVO> businessFields = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionVO {
        private String label;
        private String value;
        private String description;
    }
}
