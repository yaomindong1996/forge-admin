package com.mdframe.forge.plugin.capability.secureaction.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class BusinessActionCapabilityPublisher {

    private static final Pattern SOURCE_SEGMENT = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final BusinessObjectActionService actionService;
    private final CapabilityCatalogService catalogService;
    private final SecureActionStepValidator stepValidator;
    private final SecureActionPublishedModelPolicy publishedModelPolicy;
    private final ObjectMapper objectMapper;

    public Long publish(Long tenantId, BusinessActionCapabilityPublishDTO dto) {
        return catalogService.publishBusinessAction(tenantId, buildDefinition(dto, "MEDIUM"));
    }

    public CapabilityPublishDTO buildDefinition(
            BusinessActionCapabilityPublishDTO dto,
            String riskLevel) {
        if (!Set.of("MEDIUM", "HIGH").contains(riskLevel)) {
            throw new BusinessException("业务动作风险等级无效");
        }
        var resolved = actionService.resolvePublishedAction(
                dto.getSuiteCode(), dto.getObjectCode(), dto.getActionCode(), null);
        BusinessObjectActionVO action = resolved.action();
        stepValidator.validate(action);
        Map<String, LowcodeFieldSchema> writable = publishedModelPolicy.writableFields(resolved.version());
        Set<String> allowedFields = normalizeFields(dto.getAllowedFields(), writable, "允许字段");
        Set<String> requiredFields = normalizeFields(dto.getRequiredFields(), writable, "必填字段");
        if (allowedFields.isEmpty()) {
            throw new BusinessException("受控业务动作必须配置至少一个允许字段");
        }
        if (!allowedFields.containsAll(requiredFields)) {
            throw new BusinessException("必填字段必须属于允许字段");
        }
        String suiteCode = StringUtils.defaultIfBlank(resolved.object().getSuiteCode(), "default");
        validateSourceSegment(suiteCode, "业务套件编码");
        validateSourceSegment(resolved.object().getObjectCode(), "业务对象编码");
        validateSourceSegment(action.getActionCode(), "业务动作编码");

        JsonNode inputSchema = buildInputSchema(allowedFields, requiredFields, writable);
        JsonNode outputSchema = buildOutputSchema();
        ObjectNode policy = objectMapper.createObjectNode();
        policy.set("allowedFields", toArray(allowedFields));
        policy.set("requiredFields", toArray(requiredFields));
        policy.set("allowedStepTypes", toArray(SecureActionStepValidator.ALLOWED_STEP_TYPES));
        policy.put("confirmationMode", "MCP_ELICITATION");
        policy.put("publishedObjectVersion", resolved.version().getPublishVersion());
        policy.put("permission", StringUtils.defaultIfBlank(
                action.getPermission(), "ai:businessAction:execute"));
        policy.put("actionName", action.getActionName());
        policy.put("objectName", resolved.object().getObjectName());
        policy.put("riskLevel", riskLevel);

        CapabilityPublishDTO command = new CapabilityPublishDTO(
                dto.getCapabilityCode(), dto.getCapabilityCode(), action.getActionName(),
                StringUtils.defaultIfBlank(dto.getDescription(), action.getActionName()),
                "BUSINESS_ACTION",
                suiteCode + "/" + resolved.object().getObjectCode() + "/" + action.getActionCode(),
                String.valueOf(resolved.version().getPublishVersion()),
                StringUtils.defaultIfBlank(dto.getVersion(), "1.0.0"),
                "ACTION", riskLevel, "DISCOVERABLE",
                inputSchema, outputSchema, policy);
        return command;
    }

    private Set<String> normalizeFields(
            Set<String> source,
            Map<String, LowcodeFieldSchema> writable,
            String label) {
        Set<String> result = new LinkedHashSet<>();
        if (source == null) {
            return result;
        }
        for (String item : source) {
            String field = StringUtils.trimToNull(item);
            if (field == null || !writable.containsKey(field)) {
                throw new BusinessException(label + "不存在或不可写: " + item);
            }
            result.add(field);
        }
        return result;
    }

    private ObjectNode buildInputSchema(
            Set<String> allowed,
            Set<String> required,
            Map<String, LowcodeFieldSchema> fields) {
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("type", "object");
        arguments.put("additionalProperties", false);
        ObjectNode argumentProperties = arguments.putObject("properties");
        for (String name : allowed) {
            LowcodeFieldSchema field = fields.get(name);
            ObjectNode property = argumentProperties.putObject(name);
            property.put("type", jsonType(field.getDataType()));
            property.put("description", StringUtils.defaultIfBlank(field.getLabel(), name));
        }
        arguments.set("required", toArray(required));

        ObjectNode root = objectMapper.createObjectNode();
        root.put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("recordId").put("type", "string").put("minLength", 1).put("maxLength", 128);
        properties.putObject("idempotencyKey").put("type", "string").put("minLength", 16).put("maxLength", 128);
        properties.set("arguments", arguments);
        root.set("required", toArray(Set.of("idempotencyKey", "arguments")));
        return root;
    }

    private ObjectNode buildOutputSchema() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("executeStatus").put("type", "string");
        properties.putObject("message").put("type", "string");
        properties.putObject("correlationId").put("type", "string");
        properties.putObject("idempotentHit").put("type", "boolean");
        properties.putObject("approvalRequestId").put("type", "string");
        root.set("required", toArray(Set.of("executeStatus", "message", "correlationId", "idempotentHit")));
        return root;
    }

    private ArrayNode toArray(Set<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.stream().sorted().forEach(array::add);
        return array;
    }

    private String jsonType(String dataType) {
        String normalized = StringUtils.defaultString(dataType).toLowerCase();
        if (Set.of("int", "integer", "long", "bigint").contains(normalized)) {
            return "integer";
        }
        if (Set.of("decimal", "double", "float", "number", "money").contains(normalized)) {
            return "number";
        }
        if (Set.of("boolean", "bool", "tinyint(1)").contains(normalized)) {
            return "boolean";
        }
        return "string";
    }

    private void validateSourceSegment(String value, String label) {
        if (value == null || !SOURCE_SEGMENT.matcher(value).matches()) {
            throw new BusinessException(label + "不符合受控能力绑定格式");
        }
    }
}
