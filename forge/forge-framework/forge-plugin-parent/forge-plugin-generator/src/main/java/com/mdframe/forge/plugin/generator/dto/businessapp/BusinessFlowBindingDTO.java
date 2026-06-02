package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务对象流程绑定保存参数。
 */
@Data
public class BusinessFlowBindingDTO {

    private String flowModelKey;

    private String flowModelName;

    private String titleTemplate;

    private String startMode;

    private List<VariableMappingDTO> variableMapping = new ArrayList<>();

    private List<Map<String, Object>> conditionFlows = new ArrayList<>();

    private Map<String, Object> options = new LinkedHashMap<>();

    @Data
    public static class VariableMappingDTO {

        private String formField;

        private String flowVariable;

        private String label;
    }
}
