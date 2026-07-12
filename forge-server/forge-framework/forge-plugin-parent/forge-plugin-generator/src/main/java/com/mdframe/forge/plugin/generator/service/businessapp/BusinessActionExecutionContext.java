package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionExecuteDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用业务动作执行上下文。
 */
@Data
public class BusinessActionExecutionContext {

    private Long tenantId;

    private String correlationId;

    private AiBusinessObject businessObject;

    private BusinessObjectActionVO action;

    private Integer publishedVersion;

    private String capabilityRequestId;

    private Long capabilityClientId;

    private Long capabilityServiceUserId;

    private String capabilityActorType;

    private BusinessActionExecuteDTO request;

    private Map<String, Object> recordData = new LinkedHashMap<>();

    private Map<String, Object> formData = new LinkedHashMap<>();

    private Map<String, Object> extraContext = new LinkedHashMap<>();

    private Map<String, Object> scopedVariables = new LinkedHashMap<>();
}
