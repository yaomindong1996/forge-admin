package com.mdframe.forge.plugin.capability.flowaction.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.secureaction.schema.LowcodeCapabilitySchemaTypeResolver;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class FlowActionCapabilityPublisher {

    private static final Set<String> OPERATIONS = Set.of("SUBMIT", "START", "APPROVE", "REJECT");
    private static final Pattern SOURCE_SEGMENT = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final FlowActionSourceService sourceService;
    private final CapabilityCatalogService catalogService;
    private final ObjectMapper objectMapper;

    public Long publish(Long tenantId, FlowActionCapabilityPublishDTO dto) {
        String operation = StringUtils.defaultString(dto.getOperation())
                .trim().toUpperCase(Locale.ROOT);
        if (!OPERATIONS.contains(operation)) {
            throw new BusinessException("当前阶段只允许发布 SUBMIT、START、APPROVE、REJECT 流程动作");
        }
        validateSegment(dto.getSuiteCode(), "业务套件编码");
        validateSegment(dto.getObjectCode(), "业务对象编码");
        var source = sourceService.requirePublished(tenantId, dto.getSuiteCode(), dto.getObjectCode());
        if ("START".equals(operation)
                && (StringUtils.isBlank(source.row().getConfigKey())
                || source.row().getRuntimeConfigId() == null)) {
            throw new BusinessException("当前对象不是平台托管的已发布运行对象，不能发布通用 START 能力");
        }
        Map<String, LowcodeFieldSchema> submissionFields = "SUBMIT".equals(operation)
                ? sourceService.requireSubmissionFields(source) : Map.of();
        Set<String> allowedFields = resolveAllowedFields(dto.getAllowedFields(), submissionFields);
        Set<String> requiredFields = resolveRequiredFields(
                dto.getRequiredFields(), allowedFields, submissionFields);

        ObjectNode policy = objectMapper.createObjectNode();
        policy.put("bindingId", source.row().getBindingId());
        policy.put("flowModelKey", source.flowModelKey());
        policy.put("operation", operation);
        policy.put("publishedObjectVersion", source.row().getPublishedObjectVersion());
        policy.put("permission", "START".equals(operation)
                ? "ai:businessFlow:start" : "ai:businessFlow:view");
        if ("SUBMIT".equals(operation)) {
            policy.put("permission", "ai:businessFlow:start");
            policy.set("allowedFields", array(allowedFields));
            policy.set("requiredFields", array(requiredFields));
        }
        policy.put("confirmationMode", "MCP_ELICITATION");
        policy.set("allowedOperations", array(operation));
        addDocumentation(policy, operation, source.row().getObjectName());

        String operationName = switch (operation) {
            case "SUBMIT" -> "提交";
            case "START" -> "发起";
            case "APPROVE" -> "同意";
            default -> "驳回";
        };
        CapabilityPublishDTO command = new CapabilityPublishDTO(
                dto.getCapabilityCode(), dto.getCapabilityCode(),
                "SUBMIT".equals(operation)
                        ? operationName + source.row().getObjectName() + "申请"
                        : operationName + source.row().getObjectName() + "流程",
                StringUtils.defaultIfBlank(dto.getDescription(),
                        "SUBMIT".equals(operation)
                                ? "创建" + source.row().getObjectName() + "记录并发起已发布主流程"
                                : operationName + source.row().getObjectName() + "的已发布主流程"),
                "FLOW_ACTION",
                source.row().getSuiteCode() + "/" + source.row().getObjectCode() + "/" + operation,
                String.valueOf(source.row().getPublishedObjectVersion()),
                dto.getVersion(), "FLOW", "MEDIUM", "DISCOVERABLE", "USER",
                inputSchema(operation, allowedFields, requiredFields, submissionFields),
                outputSchema(operation), policy);
        return catalogService.publishFlowAction(tenantId, command);
    }

    private ObjectNode inputSchema(
            String operation,
            Set<String> allowedFields,
            Set<String> requiredFields,
            Map<String, LowcodeFieldSchema> submissionFields) {
        if ("SUBMIT".equals(operation)) {
            return submissionInputSchema(allowedFields, requiredFields, submissionFields);
        }
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("type", "object");
        arguments.put("additionalProperties", false);
        arguments.put("description", "流程办理参数；START 必须保持为空对象");
        ObjectNode argumentProperties = arguments.putObject("properties");
        ArrayNode argumentRequired = objectMapper.createArrayNode();
        if (!"START".equals(operation)) {
            argumentProperties.putObject("taskId")
                    .put("type", "string").put("minLength", 1).put("maxLength", 128)
                    .put("description", "当前委托用户可办理且属于该业务记录的流程任务 ID");
            argumentRequired.add("taskId");
            ObjectNode comment = argumentProperties.putObject("comment");
            comment.put("type", "string").put("maxLength", 500)
                    .put("description", "审批意见；驳回时必填，最多 500 个字符");
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
                .put("type", "string")
                .put("minLength", 1)
                .put("maxLength", 19)
                .put("description", "已保存业务记录的真实主键；只接受 1 至 19 位十进制正整数，且必须在当前委托用户的数据权限范围内");
        properties.set("arguments", arguments);
        root.set("required", array("recordId", "arguments"));
        return root;
    }

    private ObjectNode submissionInputSchema(
            Set<String> allowedFields,
            Set<String> requiredFields,
            Map<String, LowcodeFieldSchema> fields) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("type", "object");
        data.put("additionalProperties", false);
        data.put("description", "业务申请数据；用户、租户、状态和流程字段由平台生成");
        ObjectNode properties = data.putObject("properties");
        for (String name : allowedFields) {
            appendSubmissionProperty(properties.putObject(name), fields.get(name));
        }
        data.set("required", array(requiredFields));

        ObjectNode root = objectMapper.createObjectNode();
        root.put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        root.put("type", "object");
        root.put("additionalProperties", false);
        root.putObject("properties").set("data", data);
        root.set("required", array("data"));
        return root;
    }

    private void appendSubmissionProperty(ObjectNode property, LowcodeFieldSchema field) {
        String type = jsonType(field);
        property.put("type", type);
        if ("string".equals(type) && field.getLength() != null && field.getLength() > 0) {
            property.put("maxLength", field.getLength());
        }
        if ("array".equals(type)) {
            appendArrayItems(property, field);
        }
        if ("object".equals(type)) {
            property.put("additionalProperties", true);
        }
        property.put("description", fieldDescription(field));
    }

    private void appendArrayItems(ObjectNode property, LowcodeFieldSchema field) {
        String itemType = configuredArrayItemType(field);
        ObjectNode items = property.putObject("items");
        items.put("type", itemType);
        if ("object".equals(itemType)) {
            items.put("additionalProperties", true);
        }
    }

    private String configuredArrayItemType(LowcodeFieldSchema field) {
        Object configured = field.getAdvancedProps() == null
                ? null : field.getAdvancedProps().get("itemType");
        String type = StringUtils.defaultString(configured == null ? null : String.valueOf(configured))
                .trim().toLowerCase(Locale.ROOT);
        return Set.of("string", "integer", "number", "boolean", "object").contains(type)
                ? type : "string";
    }

    private ObjectNode outputSchema(String operation) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("executeStatus").put("type", "string")
                .put("description", "执行状态；成功为 SUCCESS");
        properties.putObject("message").put("type", "string")
                .put("description", "面向排障的结果说明，程序判断请使用 code 和 executeStatus");
        properties.putObject("correlationId").put("type", "string")
                .put("description", "能力调用关联 ID，与响应 requestId 一致");
        properties.putObject("idempotentHit").put("type", "boolean")
                .put("description", "是否命中同一 Idempotency-Key 的历史成功结果");
        Set<String> required = new LinkedHashSet<>(
                Set.of("executeStatus", "message", "correlationId", "idempotentHit"));
        if ("SUBMIT".equals(operation)) {
            properties.putObject("recordId").put("type", "string")
                    .put("description", "平台新建业务申请记录的主键 ID");
            properties.putObject("businessKey").put("type", "string")
                    .put("description", "流程业务键，格式为 objectCode:recordId");
            properties.putObject("processInstanceId").put("type", "string")
                    .put("description", "已启动的流程实例 ID");
            properties.putObject("flowModelKey").put("type", "string")
                    .put("description", "本次实际启动的已发布流程模型编码");
            properties.putObject("flowStatus").put("type", "string")
                    .put("description", "流程运行状态，提交成功通常为 RUNNING");
            required.addAll(Set.of(
                    "recordId", "businessKey", "processInstanceId", "flowModelKey", "flowStatus"));
        }
        root.set("required", array(required));
        return root;
    }

    private Set<String> resolveAllowedFields(
            Set<String> requested,
            Map<String, LowcodeFieldSchema> fields) {
        if (fields.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        Set<String> source = requested == null || requested.isEmpty() ? fields.keySet() : requested;
        for (String item : source) {
            String field = StringUtils.trimToNull(item);
            if (field == null || !fields.containsKey(field)) {
                throw new BusinessException("申请允许字段不存在或不可由外围系统填写: " + item);
            }
            result.add(field);
        }
        if (result.isEmpty()) {
            throw new BusinessException("提交业务申请至少需要开放一个业务字段");
        }
        return Collections.unmodifiableSet(result);
    }

    private Set<String> resolveRequiredFields(
            Set<String> requested,
            Set<String> allowed,
            Map<String, LowcodeFieldSchema> fields) {
        Set<String> result = new LinkedHashSet<>();
        fields.forEach((name, field) -> {
            if (Boolean.TRUE.equals(field.getRequired())) {
                if (!allowed.contains(name)) {
                    throw new BusinessException("业务对象必填字段必须开放: " + name);
                }
                result.add(name);
            }
        });
        if (requested != null) {
            for (String item : requested) {
                String field = StringUtils.trimToNull(item);
                if (field == null || !allowed.contains(field)) {
                    throw new BusinessException("申请必填字段必须属于允许字段: " + item);
                }
                result.add(field);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private String jsonType(LowcodeFieldSchema field) {
        return LowcodeCapabilitySchemaTypeResolver.resolve(field);
    }

    private String fieldDescription(LowcodeFieldSchema field) {
        String label = StringUtils.defaultIfBlank(field.getLabel(), field.getField());
        String detail = StringUtils.firstNonBlank(
                field.getRemark(), basicText(field, "helpText"), basicText(field, "help"),
                basicText(field, "placeholder"));
        String description = detail == null || label.equals(detail) ? label : label + "；" + detail;
        if (StringUtils.isNotBlank(field.getDictType())) {
            description += "；字典类型=" + field.getDictType()
                    + "，请传当前启用字典项的 dict_value";
        }
        String businessType = StringUtils.defaultString(field.getBusinessFieldType())
                .toUpperCase(Locale.ROOT);
        if ("MONEY".equals(businessType)) {
            description += "；金额单位=分，请传整数";
        }
        else if (field.getPrecision() != null && field.getPrecision() >= 0
                && Set.of("decimal", "double", "float", "number")
                .contains(StringUtils.defaultString(field.getDataType()).toLowerCase(Locale.ROOT))) {
            description += "；小数位数=" + field.getPrecision();
        }
        String dataType = StringUtils.defaultString(field.getDataType()).toLowerCase(Locale.ROOT);
        if (Set.of("date", "localdate").contains(dataType)) {
            description += "；格式=yyyy-MM-dd";
        }
        else if (Set.of("datetime", "timestamp", "localdatetime").contains(dataType)) {
            description += "；格式=yyyy-MM-dd HH:mm:ss";
        }
        if ("array".equals(dataType)) {
            description += "；数组元素类型=" + configuredArrayItemType(field);
        }
        if (field.getDefaultValue() != null) {
            description += "；未传时由低代码运行模型应用默认值";
        }
        if (isUnique(field)) {
            description += "；当前租户内不可与已有记录重复";
        }
        return description;
    }

    private boolean isUnique(LowcodeFieldSchema field) {
        return booleanValue(field.getAdvancedProps() == null
                ? null : field.getAdvancedProps().get("unique"))
                || booleanValue(field.getAdvancedProps() == null
                ? null : field.getAdvancedProps().get("uniqueCheck"))
                || booleanValue(field.getBasicProps() == null
                ? null : field.getBasicProps().get("unique"));
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean bool
                ? bool : value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private String basicText(LowcodeFieldSchema field, String key) {
        if (field.getBasicProps() == null || field.getBasicProps().get(key) == null) {
            return null;
        }
        return StringUtils.trimToNull(String.valueOf(field.getBasicProps().get(key)));
    }

    private void addDocumentation(ObjectNode policy, String operation, String objectName) {
        ObjectNode documentation = policy.putObject("documentation");
        ArrayNode requestNotes = documentation.putArray("requestNotes");
        ArrayNode responseNotes = documentation.putArray("responseNotes");
        ArrayNode businessRules = documentation.putArray("businessRules");
        if ("SUBMIT".equals(operation)) {
            requestNotes.add("data 是" + objectName + "申请数据；不要传 recordId、用户、租户、状态或流程字段。");
            requestNotes.add("平台会使用 Token 对应的真实委托用户创建记录，并在同一次调用中发起主流程。");
            responseNotes.add("recordId 是新建业务记录主键，processInstanceId 是已启动流程实例 ID。");
            responseNotes.add("流程启动失败时请复用原 Idempotency-Key 重试，平台会继续使用同一业务记录。");
            businessRules.add("记录创建执行发布模型字段白名单、必填项、类型、长度、租户内唯一约束、字段加密、自动编号和数据库约束；任一校验失败都不会启动流程。");
            businessRules.add("流程启动执行实际委托用户数据权限、单据状态、重复运行流程和当前主流程绑定校验；已有运行中流程不会重复发起。");
            businessRules.add("申请人、租户、组织、审计字段和流程发起人只从可信委托身份生成。");
            return;
        }
        requestNotes.add("recordId 必须是当前委托用户可见的已保存业务记录主键。");
        businessRules.add("执行前重新校验业务对象发布版本、主流程绑定和实际委托用户权限。");
    }

    private ArrayNode array(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private ArrayNode array(Iterable<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.forEach(array::add);
        return array;
    }

    private void validateSegment(String value, String label) {
        if (value == null || !SOURCE_SEGMENT.matcher(value).matches()) {
            throw new BusinessException(label + "不符合受控流程能力绑定格式");
        }
    }
}
