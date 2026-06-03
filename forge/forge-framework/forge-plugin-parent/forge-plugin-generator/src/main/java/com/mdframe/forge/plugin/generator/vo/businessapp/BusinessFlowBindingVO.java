package com.mdframe.forge.plugin.generator.vo.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务对象流程绑定视图。
 */
@Data
public class BusinessFlowBindingVO {

    private Long bindingId;

    private String objectCode;

    private String flowModelKey;

    private String flowModelName;

    private String titleTemplate;

    private String startMode;

    private List<BusinessFlowBindingDTO.VariableMappingDTO> variableMapping = new ArrayList<>();

    private List<Map<String, Object>> conditionFlows = new ArrayList<>();

    private Map<String, Object> options = new LinkedHashMap<>();

    private Integer status;

    private Map<String, Object> mainFlowSummary = new LinkedHashMap<>();

    private Boolean complete;

    private List<String> gaps = new ArrayList<>();

    private String compatibilitySource;
}
