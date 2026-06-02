package com.mdframe.forge.plugin.generator.dto.businessapp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程回调参数。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessFlowCallbackDTO {

    private String processInstanceId;

    private String businessKey;

    @JsonAlias("eventType")
    private String flowStatus;

    private String result;

    private Long tenantId;

    private String nodeKey;

    private String nodeName;

    private Long operatorId;

    private Map<String, Object> variables = new LinkedHashMap<>();
}
