package com.mdframe.forge.plugin.capability.flowaction.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class FlowActionCapabilityPublisher {

    private static final Set<String> OPERATIONS = Set.of("START", "APPROVE", "REJECT");
    private static final Pattern SOURCE_SEGMENT = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final FlowActionSourceService sourceService;
    private final CapabilityCatalogService catalogService;
    private final ObjectMapper objectMapper;

    public Long publish(Long tenantId, FlowActionCapabilityPublishDTO dto) {
        String operation = StringUtils.defaultString(dto.getOperation())
                .trim().toUpperCase(Locale.ROOT);
        if (!OPERATIONS.contains(operation)) {
            throw new BusinessException("当前阶段只允许发布 START、APPROVE、REJECT 流程动作");
        }
        validateSegment(dto.getSuiteCode(), "业务套件编码");
        validateSegment(dto.getObjectCode(), "业务对象编码");
        var source = sourceService.requirePublished(tenantId, dto.getSuiteCode(), dto.getObjectCode());
        if ("START".equals(operation) && StringUtils.isBlank(source.row().getConfigKey())) {
            throw new BusinessException("当前对象不是平台托管的已发布运行对象，不能发布通用 START 能力");
        }

        ObjectNode policy = objectMapper.createObjectNode();
        policy.put("bindingId", source.row().getBindingId());
        policy.put("flowModelKey", source.flowModelKey());
        policy.put("operation", operation);
        policy.put("publishedObjectVersion", source.row().getPublishedObjectVersion());
        policy.put("permission", "START".equals(operation)
                ? "ai:businessFlow:start" : "ai:businessFlow:view");
        policy.put("confirmationMode", "MCP_ELICITATION");
        policy.set("allowedOperations", array(operation));

        String operationName = switch (operation) {
            case "START" -> "发起";
            case "APPROVE" -> "同意";
            default -> "驳回";
        };
        CapabilityPublishDTO command = new CapabilityPublishDTO(
                dto.getCapabilityCode(), dto.getCapabilityCode(),
                operationName + source.row().getObjectName() + "流程",
                StringUtils.defaultIfBlank(dto.getDescription(),
                        operationName + source.row().getObjectName() + "的已发布主流程"),
                "FLOW_ACTION",
                source.row().getSuiteCode() + "/" + source.row().getObjectCode() + "/" + operation,
                String.valueOf(source.row().getPublishedObjectVersion()),
                dto.getVersion(), "FLOW", "MEDIUM", "DISCOVERABLE",
                inputSchema(operation), outputSchema(), policy);
        return catalogService.publishFlowAction(tenantId, command);
    }

    private ObjectNode inputSchema(String operation) {
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("type", "object");
        arguments.put("additionalProperties", false);
        ObjectNode argumentProperties = arguments.putObject("properties");
        ArrayNode argumentRequired = objectMapper.createArrayNode();
        if (!"START".equals(operation)) {
            argumentProperties.putObject("taskId")
                    .put("type", "string").put("minLength", 1).put("maxLength", 128);
            argumentRequired.add("taskId");
            ObjectNode comment = argumentProperties.putObject("comment");
            comment.put("type", "string").put("maxLength", 500);
            if ("REJECT".equals(operation)) {
                comment.put("minLength", 1);
                argumentRequired.add("comment");
            }
        }
        arguments.set("required", argumentRequired);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("recordId")
                .put("type", "string").put("minLength", 1).put("maxLength", 128);
        properties.putObject("idempotencyKey")
                .put("type", "string").put("minLength", 16).put("maxLength", 128);
        properties.set("arguments", arguments);
        root.set("required", array("recordId", "idempotencyKey", "arguments"));
        return root;
    }

    private ObjectNode outputSchema() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("executeStatus").put("type", "string");
        properties.putObject("message").put("type", "string");
        properties.putObject("correlationId").put("type", "string");
        properties.putObject("idempotentHit").put("type", "boolean");
        root.set("required", array("executeStatus", "message", "correlationId", "idempotentHit"));
        return root;
    }

    private ArrayNode array(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private void validateSegment(String value, String label) {
        if (value == null || !SOURCE_SEGMENT.matcher(value).matches()) {
            throw new BusinessException(label + "不符合受控流程能力绑定格式");
        }
    }
}
