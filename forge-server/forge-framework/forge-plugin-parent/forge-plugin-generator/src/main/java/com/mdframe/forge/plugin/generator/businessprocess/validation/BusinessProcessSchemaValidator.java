package com.mdframe.forge.plugin.generator.businessprocess.validation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessEdge;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessNode;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessSchema;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessProcessValidationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * businessProcessJson 1.0 归一化、稳定摘要和发布前失败关闭校验。
 */
@Component
public class BusinessProcessSchemaValidator {

    public static final String SCHEMA_VERSION = "1.0";

    private static final int MAX_NODES = 100;
    private static final int MAX_EDGES = 400;
    private static final int MAX_CONDITION_BRANCHES = 20;
    private static final int MAX_SUB_PROCESS_DEPTH = 5;

    private static final Pattern PROCESS_CODE_PATTERN = Pattern.compile("[a-z][a-z0-9_]{2,127}");
    private static final Pattern FREE_URL_PATTERN = Pattern.compile("(?i)^\\s*(https?|ftp|jdbc):.*");

    private static final Set<String> EVENT_TYPES = Set.of(
            "RECORD_CREATED", "RECORD_UPDATED", "RECORD_DELETED", "STATUS_CHANGED",
            "FIELD_CHANGED", "FORM_SUBMITTED", "ACTION_EXECUTED");
    private static final Set<String> ACTION_TYPES = Set.of(
            "UPDATE_RECORD", "CREATE_RECORD", "BUSINESS_ACTION", "EXECUTE_BUSINESS_ACTION",
            "DOMAIN_ACTION", "SEND_MESSAGE", "INVOKE_CAPABILITY");
    private static final List<String> APPROVAL_PORT_ORDER = List.of(
            "APPROVED", "REJECTED", "CANCELED", "FAILED");
    private static final Set<String> APPROVAL_PORTS = Set.of(
            "APPROVED", "REJECTED", "CANCELED", "FAILED");
    private static final Set<String> FLOW_STATUS_FIELDS = Set.of("flowStatus", "flow_status");
    private static final Set<String> CONDITION_OPERATORS = Set.of(
            "eq", "ne", "neq", "gt", "ge", "gte", "lt", "le", "lte", "between",
            "contains", "notcontains", "not_contains", "empty", "is_null",
            "notempty", "not_empty", "not_null");
    private static final Set<String> END_RESULTS = Set.of(
            "SUCCESS", "REJECTED", "CANCELED", "FAILED");

    private final ObjectMapper objectMapper;
    private final BusinessProcessGraphValidator graphValidator;

    public BusinessProcessSchemaValidator(ObjectMapper objectMapper) {
        this(objectMapper, new BusinessProcessGraphValidator());
    }

    @Autowired
    public BusinessProcessSchemaValidator(ObjectMapper objectMapper,
                                          BusinessProcessGraphValidator graphValidator) {
        this.objectMapper = objectMapper.copy();
        this.graphValidator = graphValidator;
        this.objectMapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.objectMapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.objectMapper.disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        this.objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        this.objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    public BusinessProcessSchema normalize(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            throw new IllegalArgumentException("业务流程协议不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(schemaJson);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("业务流程协议根节点必须是 JSON 对象");
            }
            requireStringIds(root);
            return normalizeSchema(objectMapper.treeToValue(root, BusinessProcessSchema.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("业务流程协议 JSON 解析失败", exception);
        }
    }

    public String canonicalJson(BusinessProcessSchema schema) {
        if (schema == null) {
            throw new IllegalArgumentException("业务流程协议不能为空");
        }
        try {
            return objectMapper.writeValueAsString(normalizeSchema(schema));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("业务流程协议规范化失败", exception);
        }
    }

    public String schemaHash(BusinessProcessSchema schema) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalJson(schema).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    public BusinessProcessValidationVO validate(BusinessProcessSchema schema,
                                                 BusinessProcessValidationContext context) {
        BusinessProcessValidationVO result = new BusinessProcessValidationVO();
        if (schema == null) {
            error(result, "SCHEMA_REQUIRED", "业务流程协议不能为空", null, "$", "重新打开设计器并保存草稿");
            return result.finish();
        }

        BusinessProcessSchema normalized = normalizeSchema(schema);
        BusinessProcessValidationContext effectiveContext = context;
        if (effectiveContext == null) {
            error(result, "VALIDATION_CONTEXT_REQUIRED", "缺少当前应用的受治理依赖目录", null,
                    "$", "在租户和应用上下文中重新执行校验");
            effectiveContext = new BusinessProcessValidationContext();
        }

        validateRoot(normalized, effectiveContext, result);
        validateSensitiveTree(objectMapper.valueToTree(normalized), "$", result);
        validateDependencies(normalized, effectiveContext, result);
        BusinessProcessValidationContext validationContext = effectiveContext;
        graphValidator.validate(normalized, result,
                (node, path) -> validateNodeConfig(normalized, node, path, validationContext, result));
        return result.finish();
    }

    private BusinessProcessSchema normalizeSchema(BusinessProcessSchema source) {
        BusinessProcessSchema schema = objectMapper.convertValue(source, BusinessProcessSchema.class);
        schema.setSchemaVersion(trim(schema.getSchemaVersion()));
        schema.setProcessCode(trim(schema.getProcessCode()));
        if (schema.getSubject() != null) {
            schema.getSubject().setObjectId(trim(schema.getSubject().getObjectId()));
            schema.getSubject().setObjectCode(trim(schema.getSubject().getObjectCode()));
            schema.getSubject().setObjectVersionId(trim(schema.getSubject().getObjectVersionId()));
            schema.getSubject().setRecordIdSource(upper(schema.getSubject().getRecordIdSource()));
        }

        List<BusinessProcessNode> nodes = mutableList(schema.getNodes());
        for (BusinessProcessNode node : nodes) {
            if (node == null) {
                continue;
            }
            node.setId(trim(node.getId()));
            node.setType(upper(node.getType()));
            node.setName(trim(node.getName()));
            node.setConfig(node.getConfig() == null ? new LinkedHashMap<>() : node.getConfig());
            node.setPorts(normalizeNodePorts(node));
        }
        nodes.sort(Comparator.comparing(BusinessProcessNode::getId,
                Comparator.nullsLast(String::compareTo)));
        schema.setNodes(nodes);

        List<BusinessProcessEdge> edges = mutableList(schema.getEdges());
        for (BusinessProcessEdge edge : edges) {
            if (edge == null) {
                continue;
            }
            edge.setId(trim(edge.getId()));
            edge.setSource(trim(edge.getSource()));
            edge.setTarget(trim(edge.getTarget()));
            edge.setSourcePort(upper(edge.getSourcePort()));
            edge.setCondition(edge.getCondition() == null ? new LinkedHashMap<>() : edge.getCondition());
        }
        edges.sort(Comparator.comparing(BusinessProcessEdge::getId,
                Comparator.nullsLast(String::compareTo)));
        schema.setEdges(edges);

        if (schema.getPolicies() != null) {
            schema.getPolicies().setApprovalConcurrency(upper(schema.getPolicies().getApprovalConcurrency()));
            if (schema.getPolicies().getRetry() != null) {
                schema.getPolicies().getRetry().setMode(upper(schema.getPolicies().getRetry().getMode()));
                schema.getPolicies().getRetry().setBackoffSeconds(
                        mutableList(schema.getPolicies().getRetry().getBackoffSeconds()));
            }
        }

        BusinessProcessSchema.Dependencies dependencies = schema.getDependencies();
        if (dependencies == null) {
            dependencies = new BusinessProcessSchema.Dependencies();
            schema.setDependencies(dependencies);
        }
        dependencies.setObjects(normalizeSortedList(dependencies.getObjects()));
        dependencies.setFlowModels(normalizeSortedList(dependencies.getFlowModels()));
        dependencies.setFormAssets(normalizeSortedList(dependencies.getFormAssets()));
        dependencies.setBusinessActions(normalizeSortedList(dependencies.getBusinessActions()));
        dependencies.setMessageTemplates(normalizeSortedList(dependencies.getMessageTemplates()));
        dependencies.setCapabilities(normalizeSortedList(dependencies.getCapabilities()));
        dependencies.setSubProcesses(normalizeSortedList(dependencies.getSubProcesses()));
        schema.setMetadata(schema.getMetadata() == null ? new LinkedHashMap<>() : schema.getMetadata());
        return schema;
    }

    private void validateRoot(BusinessProcessSchema schema,
                              BusinessProcessValidationContext context,
                              BusinessProcessValidationVO result) {
        if (!SCHEMA_VERSION.equals(schema.getSchemaVersion())) {
            error(result, "SCHEMA_VERSION_UNSUPPORTED", "仅支持 businessProcessJson 1.0", null,
                    "schemaVersion", "将协议版本设置为 1.0");
        }
        if (schema.getProcessCode() == null
                || !PROCESS_CODE_PATTERN.matcher(schema.getProcessCode()).matches()) {
            error(result, "PROCESS_CODE_INVALID", "流程编码必须使用小写字母、数字和下划线", null,
                    "processCode", "使用应用内唯一的小写下划线编码");
        }
        if (context.getExpectedProcessCode() != null
                && !Objects.equals(context.getExpectedProcessCode(), schema.getProcessCode())) {
            error(result, "PROCESS_CODE_MISMATCH", "协议流程编码与流程定义不一致", null,
                    "processCode", "恢复流程定义中的稳定编码，禁止在画布修改");
        }

        BusinessProcessSchema.Subject subject = schema.getSubject();
        if (subject == null) {
            error(result, "SUBJECT_REQUIRED", "业务流程必须绑定一个主业务对象", null,
                    "subject", "从当前应用已关联对象中选择主对象");
        } else {
            if (subject.getObjectId() == null || !subject.getObjectId().matches("\\d{1,20}")) {
                error(result, "SUBJECT_OBJECT_ID_INVALID", "主业务对象 ID 必须使用无损数字字符串", null,
                        "subject.objectId", "重新选择当前应用中的业务对象");
            }
            if (subject.getObjectCode() == null || subject.getObjectCode().isBlank()) {
                error(result, "SUBJECT_OBJECT_CODE_REQUIRED", "主业务对象编码不能为空", null,
                        "subject.objectCode", "重新选择当前应用中的业务对象");
            }
            String expectedId = safeMap(context.getObjectIdsByCode()).get(subject.getObjectCode());
            if (expectedId == null) {
                error(result, "SUBJECT_OBJECT_UNAVAILABLE", "主业务对象不属于当前应用或已失效", null,
                        "subject.objectCode", "选择当前应用中已启用并已关联的对象");
            } else if (!expectedId.equals(subject.getObjectId())) {
                error(result, "SUBJECT_OBJECT_MISMATCH", "主业务对象编码与 ID 不匹配", null,
                        "subject.objectId", "重新选择对象，禁止跨应用复用对象 ID");
            }
            if (!Set.of("RUNTIME_RECORD", "EVENT_RECORD", "SCHEDULE_SCAN_RECORD")
                    .contains(subject.getRecordIdSource())) {
                error(result, "RECORD_ID_SOURCE_INVALID", "记录来源策略无效", null,
                        "subject.recordIdSource", "按手动、事件或定时开始节点选择受控记录来源");
            }
        }

        validatePolicies(schema.getPolicies(), result);
        if (schema.getNodes().size() > MAX_NODES) {
            error(result, "NODE_LIMIT_EXCEEDED", "单个业务流程最多允许 100 个节点", null,
                    "nodes", "拆分为同应用子流程");
        }
        if (schema.getEdges().size() > MAX_EDGES) {
            error(result, "EDGE_LIMIT_EXCEEDED", "单个业务流程连线数量超过限制", null,
                    "edges", "减少分支或拆分子流程");
        }
    }

    private void validatePolicies(BusinessProcessSchema.Policies policies,
                                  BusinessProcessValidationVO result) {
        if (policies == null) {
            error(result, "POLICIES_REQUIRED", "业务流程必须声明执行策略", null,
                    "policies", "恢复审批并发、重试和子流程深度默认策略");
            return;
        }
        if (!"ONE_ACTIVE_PER_BUSINESS_KEY".equals(policies.getApprovalConcurrency())) {
            error(result, "APPROVAL_CONCURRENCY_INVALID", "首版只允许同一 businessKey 一个活动审批", null,
                    "policies.approvalConcurrency", "使用 ONE_ACTIVE_PER_BUSINESS_KEY");
        }
        Integer depth = policies.getMaxSubProcessDepth();
        if (depth == null || depth < 1 || depth > MAX_SUB_PROCESS_DEPTH) {
            error(result, "SUB_PROCESS_DEPTH_INVALID", "子流程最大调用深度必须在 1 到 5 之间", null,
                    "policies.maxSubProcessDepth", "使用不超过 5 的正整数");
        }
        BusinessProcessSchema.RetryPolicy retry = policies.getRetry();
        if (retry == null || !"LIMITED".equals(retry.getMode())) {
            error(result, "RETRY_POLICY_INVALID", "首版只支持有限次数重试", null,
                    "policies.retry", "使用 LIMITED 并设置最大次数和退避秒数");
            return;
        }
        Integer maxAttempts = retry.getMaxAttempts();
        if (maxAttempts == null || maxAttempts < 1 || maxAttempts > 10) {
            error(result, "RETRY_ATTEMPTS_INVALID", "重试次数必须在 1 到 10 之间", null,
                    "policies.retry.maxAttempts", "设置有限且可审计的重试次数");
        }
        List<Integer> backoffs = mutableList(retry.getBackoffSeconds());
        if (maxAttempts != null && backoffs.size() < maxAttempts) {
            error(result, "RETRY_BACKOFF_INCOMPLETE", "每次允许的尝试都必须配置退避时间", null,
                    "policies.retry.backoffSeconds", "为全部尝试配置正数秒数");
        }
        if (backoffs.stream().anyMatch(value -> value == null || value <= 0)) {
            error(result, "RETRY_BACKOFF_INVALID", "重试退避时间必须为正数", null,
                    "policies.retry.backoffSeconds", "移除零值或负数");
        }
    }

    private void validateDependencies(BusinessProcessSchema schema,
                                      BusinessProcessValidationContext context,
                                      BusinessProcessValidationVO result) {
        BusinessProcessSchema.Dependencies dependencies = schema.getDependencies();
        validateAvailable(dependencies.getObjects(), safeMap(context.getObjectIdsByCode()).keySet(),
                "OBJECT_DEPENDENCY_UNAVAILABLE", "dependencies.objects", result);
        validateAvailable(dependencies.getObjects(),
                safeMap(context.getPublishedObjectVersionIdsByCode()).keySet(),
                "OBJECT_VERSION_UNAVAILABLE", "dependencies.objects",
                "业务对象「{reference}」还没有发布版本。已发布的审批模型不算这个对象",
                "确认「{reference}」属于当前应用且页面已保存；不要把它和已发布的审批模型当成同一项", result);
        validateAvailable(dependencies.getFlowModels(), safeSet(context.getAvailableFlowModelKeys()),
                "FLOW_MODEL_UNAVAILABLE", "dependencies.flowModels",
                "审批模型未发布、未部署或已失效", "重新选择当前租户可用的已发布审批模型", result);
        validateAvailable(dependencies.getFormAssets(), safeSet(context.getAvailableFormAssetKeys()),
                "FORM_ASSET_UNAVAILABLE", "dependencies.formAssets",
                "任务表单「{reference}」不存在、未发布或不属于当前应用", "重新选择当前业务对象的可用表单", result);
        validateAvailable(dependencies.getBusinessActions(), safeSet(context.getAvailableBusinessActionCodes()),
                "BUSINESS_ACTION_UNAVAILABLE", "dependencies.businessActions", result);
        validateAvailable(dependencies.getMessageTemplates(), safeSet(context.getAvailableMessageTemplateCodes()),
                "MESSAGE_TEMPLATE_UNAVAILABLE", "dependencies.messageTemplates", result);
        validateAvailable(dependencies.getCapabilities(), safeSet(context.getAvailableCapabilityCodes()),
                "CAPABILITY_UNAVAILABLE", "dependencies.capabilities", result);
        validateAvailable(dependencies.getSubProcesses(), safeSet(context.getPublishedSubProcessCodes()),
                "SUB_PROCESS_UNAVAILABLE", "dependencies.subProcesses", result);
        if (!dependencies.getCapabilities().isEmpty() && !context.isCapabilityBridgeAvailable()) {
            error(result, "CAPABILITY_BRIDGE_UNAVAILABLE", "统一能力平台受控桥接尚不可用", null,
                    "dependencies.capabilities", "完成受控桥接后再发布能力调用节点");
        }
    }

    private void validateAvailable(Collection<String> declared, Set<String> available,
                                   String code, String path,
                                   BusinessProcessValidationVO result) {
        validateAvailable(declared, available, code, path,
                "依赖未发布、不属于当前应用或已失效", "重新选择当前应用中的已发布依赖", result);
    }

    private void validateAvailable(Collection<String> declared, Set<String> available,
                                   String code, String path, String message, String action,
                                   BusinessProcessValidationVO result) {
        for (String reference : declared) {
            if (!available.contains(reference)) {
                String resolvedReference = reference == null ? "" : reference;
                error(result, code, message.replace("{reference}", resolvedReference),
                        null, path, action.replace("{reference}", resolvedReference));
            }
        }
    }

    private void validateNodeConfig(BusinessProcessSchema schema,
                                    BusinessProcessNode node,
                                    String path,
                                    BusinessProcessValidationContext context,
                                    BusinessProcessValidationVO result) {
        Map<String, Object> config = node.getConfig();
        switch (node.getType()) {
            case "START_MANUAL" -> validateManualStart(node, path, config, context, result);
            case "START_EVENT" -> validateEventStart(node, path, config, result);
            case "START_SCHEDULE" -> validateScheduleStart(node, path, config, result);
            case "CONDITION" -> validateCondition(node, path, config, result);
            case "ACTION" -> validateAction(schema, node, path, config, context, result);
            case "APPROVAL" -> validateApproval(schema, node, path, config, context, result);
            case "SUB_PROCESS" -> validateSubProcess(schema, node, path, config, context, result);
            case "END" -> validateEnd(node, path, config, result);
            default -> {
                // 未知类型已由节点注册表校验报告。
            }
        }
        String subjectCode = schema.getSubject() == null ? null : schema.getSubject().getObjectCode();
        validateFieldReferences(config, subjectCode, safeMap(context.getFieldsByObjectCode()),
                node.getId(), path + ".config", result);
    }

    private void validateManualStart(BusinessProcessNode node, String path,
                                     Map<String, Object> config,
                                     BusinessProcessValidationContext context,
                                     BusinessProcessValidationVO result) {
        String permission = string(config.get("permission"));
        if (permission == null || !safeSet(context.getKnownPermissions()).contains(permission)) {
            error(result, "MANUAL_PERMISSION_UNAVAILABLE", "手动开始权限不存在或未授权", node.getId(),
                    path + ".config.permission", "选择当前应用已注册的开始权限");
        }
    }

    private void validateEventStart(BusinessProcessNode node, String path,
                                    Map<String, Object> config,
                                    BusinessProcessValidationVO result) {
        String eventType = string(config.get("eventType"));
        if (!EVENT_TYPES.contains(eventType)) {
            error(result, "EVENT_TYPE_UNSUPPORTED", "事件开始类型不受支持", node.getId(),
                    path + ".config.eventType", "选择受治理的记录或业务语义事件");
        }
    }

    private void validateScheduleStart(BusinessProcessNode node, String path,
                                       Map<String, Object> config,
                                       BusinessProcessValidationVO result) {
        if (string(config.get("dueField")) == null) {
            error(result, "SCHEDULE_FIELD_REQUIRED", "定时扫描必须指定日期字段", node.getId(),
                    path + ".config.dueField", "选择主业务对象中的日期字段");
        }
        Map<String, Object> actor = map(config.get("serviceActor"));
        String mode = string(actor.get("mode"));
        if ("CONFIGURED_USER".equals(mode)) {
            if (string(actor.get("userConfigKey")) == null) {
                error(result, "SCHEDULE_ACTOR_CONFIG_REQUIRED", "定时服务用户必须引用受控配置", node.getId(),
                        path + ".config.serviceActor.userConfigKey", "配置受限普通服务用户引用");
            }
        } else if ("RECORD_FIELD".equals(mode)) {
            if (string(actor.get("field")) == null) {
                error(result, "SCHEDULE_ACTOR_FIELD_REQUIRED", "记录解析用户必须指定字段", node.getId(),
                        path + ".config.serviceActor.field", "选择唯一解析普通用户的记录字段");
            }
        } else {
            error(result, "SCHEDULE_ACTOR_MODE_INVALID", "定时发起人策略无效", node.getId(),
                    path + ".config.serviceActor.mode", "使用 CONFIGURED_USER 或 RECORD_FIELD");
        }
        if (actor.containsKey("userId") || actor.containsKey("actorUserId")) {
            error(result, "TRUSTED_IDENTITY_OVERRIDE", "画布不得保存或覆盖执行用户 ID", node.getId(),
                    path + ".config.serviceActor", "改为引用受控配置或记录字段");
        }
    }

    private void validateCondition(BusinessProcessNode node, String path,
                                   Map<String, Object> config,
                                   BusinessProcessValidationVO result) {
        List<?> branches = list(config.get("branches"));
        if (branches.size() < 2 || branches.size() > MAX_CONDITION_BRANCHES) {
            error(result, "CONDITION_BRANCH_COUNT_INVALID", "条件节点至少需要一个判断分支和一个默认分支", node.getId(),
                    path + ".config.branches", "配置 2 到 20 个分支，并保留一个默认分支");
            return;
        }
        Set<String> branchPorts = new LinkedHashSet<>();
        int defaults = 0;
        for (int index = 0; index < branches.size(); index++) {
            Map<String, Object> branch = map(branches.get(index));
            String port = upper(string(branch.get("port")));
            if (port == null || !branchPorts.add(port)) {
                error(result, "CONDITION_PORT_INVALID", "条件分支出口为空或重复", node.getId(),
                        path + ".config.branches[" + index + "].port", "为每个分支配置唯一出口");
            }
            if (Boolean.TRUE.equals(booleanValue(branch.get("isDefault")))) {
                defaults++;
                if (!map(branch.get("condition")).isEmpty()) {
                    error(result, "CONDITION_DEFAULT_HAS_RULE", "默认分支不能配置判断规则", node.getId(),
                            path + ".config.branches[" + index + "].condition", "清空默认分支条件");
                }
            } else {
                validateStructuredCondition(node, path, branch, index, result);
            }
        }
        if (defaults != 1) {
            error(result, "CONDITION_DEFAULT_INVALID", "条件节点必须且只能保留一个默认分支", node.getId(),
                    path + ".config.branches", "设置一个默认分支处理其他情况");
        }
        if (!new LinkedHashSet<>(node.getPorts()).equals(branchPorts)) {
            error(result, "CONDITION_PORTS_MISMATCH", "条件节点出口与分支配置不一致", node.getId(),
                    path + ".ports", "同步分支 port 与节点 ports");
        }
    }

    private void validateStructuredCondition(BusinessProcessNode node,
                                               String path,
                                               Map<String, Object> branch,
                                               int branchIndex,
                                               BusinessProcessValidationVO result) {
        String conditionPath = path + ".config.branches[" + branchIndex + "].condition";
        Map<String, Object> condition = map(branch.get("condition"));
        List<?> rules = list(condition.get("rules"));
        String operator = upper(string(condition.get("operator")));
        if (condition.isEmpty() || rules.isEmpty()) {
            error(result, "CONDITION_RULE_REQUIRED", "非默认分支必须至少配置一条判断规则", node.getId(),
                    conditionPath, "选择业务字段、判断关系和比较值");
            return;
        }
        if (!Set.of("AND", "OR").contains(operator)) {
            error(result, "CONDITION_LOGIC_INVALID", "条件分支的满足方式无效", node.getId(),
                    conditionPath + ".operator", "选择满足全部规则或满足任意规则");
        }
        for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
            Map<String, Object> rule = map(rules.get(ruleIndex));
            String rulePath = conditionPath + ".rules[" + ruleIndex + "]";
            String field = string(rule.get("field"));
            String ruleOperator = string(rule.get("operator"));
            String normalizedRuleOperator = ruleOperator == null
                    ? null : ruleOperator.toLowerCase(Locale.ROOT);
            if (field == null) {
                error(result, "CONDITION_FIELD_REQUIRED", "判断规则必须选择业务字段", node.getId(),
                        rulePath + ".field", "选择当前主业务对象字段");
            }
            if (!CONDITION_OPERATORS.contains(normalizedRuleOperator)) {
                error(result, "CONDITION_OPERATOR_INVALID", "判断关系无效", node.getId(),
                        rulePath + ".operator", "重新选择受支持的判断关系");
                continue;
            }
            if (!Set.of("empty", "is_null", "notempty", "not_empty", "not_null")
                    .contains(normalizedRuleOperator)
                    && string(rule.get("value")) == null) {
                error(result, "CONDITION_VALUE_REQUIRED", "判断规则必须填写比较值", node.getId(),
                        rulePath + ".value", "填写用于比较的业务值");
            }
            if ("between".equals(normalizedRuleOperator) && string(rule.get("endValue")) == null) {
                error(result, "CONDITION_END_VALUE_REQUIRED", "区间判断必须填写结束值", node.getId(),
                        rulePath + ".endValue", "填写完整的起始值和结束值");
            }
        }
    }

    private void validateAction(BusinessProcessSchema schema,
                                BusinessProcessNode node,
                                String path,
                                Map<String, Object> config,
                                BusinessProcessValidationContext context,
                                BusinessProcessValidationVO result) {
        String actionType = string(config.get("actionType"));
        if (!ACTION_TYPES.contains(actionType)) {
            error(result, "ACTION_TYPE_UNSUPPORTED", "动作类型不在受控动作合同中", node.getId(),
                    path + ".config.actionType", "选择记录、业务动作、消息或受治理能力动作");
            return;
        }
        BusinessProcessSchema.Dependencies dependencies = schema.getDependencies();
        switch (actionType) {
            case "UPDATE_RECORD", "CREATE_RECORD" -> {
                String objectCode = string(config.get("objectCode"));
                requireDeclared(objectCode, dependencies.getObjects(), "OBJECT_DEPENDENCY_UNDECLARED",
                        node.getId(), path + ".config.objectCode", result);
                if (!safeMap(context.getObjectIdsByCode()).containsKey(objectCode)) {
                    error(result, "ACTION_OBJECT_UNAVAILABLE", "动作引用对象不属于当前应用或已失效", node.getId(),
                            path + ".config.objectCode", "选择当前应用已关联对象");
                }
                if (!hasConfiguredFieldMapping(config.get("fieldMappings"))) {
                    error(result, "ACTION_FIELD_MAPPING_REQUIRED", "更新/创建记录动作至少配置一个有效字段赋值",
                            node.getId(), path + ".config.fieldMappings",
                            "添加字段并选择目标字段后再发布");
                }
            }
            case "BUSINESS_ACTION", "EXECUTE_BUSINESS_ACTION", "DOMAIN_ACTION" -> {
                String actionCode = firstString(config, "businessActionCode", "actionCode");
                requireDeclared(actionCode, dependencies.getBusinessActions(), "BUSINESS_ACTION_UNDECLARED",
                        node.getId(), path + ".config.businessActionCode", result);
                requireAvailable(actionCode, safeSet(context.getAvailableBusinessActionCodes()),
                        "BUSINESS_ACTION_UNAVAILABLE", node.getId(), path + ".config.businessActionCode", result);
            }
            case "SEND_MESSAGE" -> {
                String template = string(config.get("messageTemplateCode"));
                requireDeclared(template, dependencies.getMessageTemplates(), "MESSAGE_TEMPLATE_UNDECLARED",
                        node.getId(), path + ".config.messageTemplateCode", result);
                requireAvailable(template, safeSet(context.getAvailableMessageTemplateCodes()),
                        "MESSAGE_TEMPLATE_UNAVAILABLE", node.getId(), path + ".config.messageTemplateCode", result);
            }
            case "INVOKE_CAPABILITY" -> {
                String capability = string(config.get("capabilityCode"));
                requireDeclared(capability, dependencies.getCapabilities(), "CAPABILITY_UNDECLARED",
                        node.getId(), path + ".config.capabilityCode", result);
                requireAvailable(capability, safeSet(context.getAvailableCapabilityCodes()),
                        "CAPABILITY_UNAVAILABLE", node.getId(), path + ".config.capabilityCode", result);
                if (!context.isCapabilityBridgeAvailable()) {
                    error(result, "CAPABILITY_BRIDGE_UNAVAILABLE", "统一能力平台受控桥接尚不可用", node.getId(),
                            path + ".config.capabilityCode", "完成受控桥接后再启用该节点");
                }
            }
            default -> {
                // 已由白名单穷尽。
            }
        }
    }

    private void validateApproval(BusinessProcessSchema schema,
                                  BusinessProcessNode node,
                                  String path,
                                  Map<String, Object> config,
                                  BusinessProcessValidationContext context,
                                  BusinessProcessValidationVO result) {
        if (!new LinkedHashSet<>(node.getPorts()).equals(APPROVAL_PORTS)) {
            error(result, "APPROVAL_PORTS_INVALID", "审批节点必须声明四个固定结果出口", node.getId(),
                    path + ".ports", "恢复审批通过、审批驳回、审批取消和执行失败四个结果出口");
        }
        String flowModelKey = string(config.get("flowModelKey"));
        requireDeclared(flowModelKey, schema.getDependencies().getFlowModels(), "FLOW_MODEL_UNDECLARED",
                node.getId(), path + ".config.flowModelKey", result);
        requireAvailable(flowModelKey, safeSet(context.getAvailableFlowModelKeys()),
                "FLOW_MODEL_UNAVAILABLE", node.getId(), path + ".config.flowModelKey",
                "审批模型未发布、未部署或已失效", "重新选择当前租户可用的已发布审批模型", result);
        if (!"PINNED_AT_APPLICATION_PUBLISH".equals(string(config.get("versionPolicy")))) {
            error(result, "APPROVAL_VERSION_POLICY_INVALID", "审批模型必须在应用发布时固定版本", node.getId(),
                    path + ".config.versionPolicy", "使用 PINNED_AT_APPLICATION_PUBLISH");
        }
        Map<String, Object> formAsset = map(config.get("formAsset"));
        String formKey = string(formAsset.get("formKey"));
        if (formKey != null) {
            requireDeclared(formKey, schema.getDependencies().getFormAssets(), "FORM_ASSET_UNDECLARED",
                    node.getId(), path + ".config.formAsset.formKey", result);
            requireAvailable(formKey, safeSet(context.getAvailableFormAssetKeys()),
                    "FORM_ASSET_UNAVAILABLE", node.getId(), path + ".config.formAsset.formKey",
                    "任务表单「" + formKey + "」不存在、未发布或不属于当前应用",
                    "重新选择当前业务对象的可用表单", result);
        }
        String formMode = firstString(formAsset, "formMode", "type");
        if ("BUSINESS_OBJECT_FORM".equalsIgnoreCase(formMode)) {
            String statusField = string(config.get("statusField"));
            if (statusField == null || !isFlowStatusField(statusField)) {
                error(result, "APPROVAL_FLOW_STATUS_REQUIRED",
                        "低代码审批必须绑定独立流程状态字段 flowStatus", node.getId(),
                        path + ".config.statusField", "在审批节点中一键添加流程状态字段并重新选择");
            }
        }
    }

    private boolean isFlowStatusField(String value) {
        return value != null
                && FLOW_STATUS_FIELDS.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(value)
                || candidate.replace("_", "").equalsIgnoreCase(value.replace("_", "")));
    }

    private void validateSubProcess(BusinessProcessSchema schema,
                                    BusinessProcessNode node,
                                    String path,
                                    Map<String, Object> config,
                                    BusinessProcessValidationContext context,
                                    BusinessProcessValidationVO result) {
        String processCode = firstString(config, "processCode", "subProcessCode");
        requireDeclared(processCode, schema.getDependencies().getSubProcesses(), "SUB_PROCESS_UNDECLARED",
                node.getId(), path + ".config.processCode", result);
        requireAvailable(processCode, safeSet(context.getPublishedSubProcessCodes()),
                "SUB_PROCESS_UNAVAILABLE", node.getId(), path + ".config.processCode", result);
        if (processCode == null) {
            return;
        }
        if (processCode.equals(schema.getProcessCode())
                || reaches(processCode, schema.getProcessCode(),
                safeDependencyMap(context.getSubProcessDependencies()), new HashSet<>())) {
            error(result, "SUB_PROCESS_RECURSION", "子流程引用形成直接或间接递归", node.getId(),
                    path + ".config.processCode", "改为同应用中不回调当前流程的已发布流程");
        }
        int maxDepth = schema.getPolicies() == null || schema.getPolicies().getMaxSubProcessDepth() == null
                ? MAX_SUB_PROCESS_DEPTH : schema.getPolicies().getMaxSubProcessDepth();
        int dependencyDepth = dependencyDepth(processCode,
                safeDependencyMap(context.getSubProcessDependencies()), new LinkedHashSet<>());
        if (dependencyDepth == Integer.MAX_VALUE || dependencyDepth + 1 > maxDepth) {
            error(result, "SUB_PROCESS_DEPTH_EXCEEDED", "子流程依赖链超过允许深度", node.getId(),
                    path + ".config.processCode", "缩短子流程调用链且总深度不超过 5");
        }
    }

    private void validateEnd(BusinessProcessNode node, String path,
                             Map<String, Object> config,
                             BusinessProcessValidationVO result) {
        if (!END_RESULTS.contains(string(config.get("result")))) {
            error(result, "END_RESULT_INVALID", "结束节点必须声明受支持的流程结果", node.getId(),
                    path + ".config.result", "使用 SUCCESS/REJECTED/CANCELED/FAILED");
        }
    }

    private void validateFieldReferences(Object value,
                                         String defaultObjectCode,
                                         Map<String, Set<String>> fieldsByObject,
                                         String nodeId,
                                         String path,
                                         BusinessProcessValidationVO result) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> current = map(rawMap);
            String objectCode = firstString(current, "objectCode");
            if (objectCode == null) {
                objectCode = defaultObjectCode;
            }
            String source = string(current.get("source"));
            for (Map.Entry<String, Object> entry : current.entrySet()) {
                String key = entry.getKey();
                if (("field".equals(key) || "dueField".equals(key))
                        && !"context".equalsIgnoreCase(source)) {
                    String field = string(entry.getValue());
                    Set<String> fields = safeSet(fieldsByObject.get(objectCode));
                    if (field != null && !fields.contains(field)) {
                        error(result, "FIELD_UNAVAILABLE", "节点引用字段不存在、未发布或无权访问", nodeId,
                                path + "." + key, "重新选择当前对象已发布字段");
                    }
                }
                validateFieldReferences(entry.getValue(), objectCode, fieldsByObject,
                        nodeId, path + "." + key, result);
            }
        } else if (value instanceof Collection<?> collection) {
            int index = 0;
            for (Object item : collection) {
                validateFieldReferences(item, defaultObjectCode, fieldsByObject,
                        nodeId, path + "[" + index++ + "]", result);
            }
        }
    }

    private void validateSensitiveTree(JsonNode node, String path,
                                       BusinessProcessValidationVO result) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                String childPath = path + "." + entry.getKey();
                if (isSensitiveKey(entry.getKey())) {
                    error(result, "SENSITIVE_KEY", "协议中包含禁止保存的敏感配置键", null,
                            childPath, "改为受治理连接、能力或服务端配置引用");
                }
                if (isIdKey(entry.getKey()) && entry.getValue().isNumber()) {
                    error(result, "ID_MUST_BE_STRING", "协议中的 ID 必须使用字符串传输", null,
                            childPath, "将长整型 ID 转为十进制字符串");
                }
                validateSensitiveTree(entry.getValue(), childPath, result);
            });
        } else if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                validateSensitiveTree(node.get(index), path + "[" + index + "]", result);
            }
        } else if (node.isTextual() && FREE_URL_PATTERN.matcher(node.textValue()).matches()) {
            error(result, "FREE_URL_FORBIDDEN", "协议中禁止保存自由 URL 或数据库地址", null,
                    path, "引用受治理能力或企业集成连接");
        }
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        return normalized.endsWith("url")
                || normalized.contains("webhook")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("password")
                || normalized.contains("privatekey")
                || normalized.contains("authorization")
                || normalized.contains("cookie")
                || normalized.contains("javaclass")
                || normalized.endsWith("sql")
                || normalized.endsWith("script")
                || normalized.endsWith("spel");
    }

    private boolean isIdKey(String key) {
        return key != null && (key.equals("id") || key.endsWith("Id") || key.endsWith("Ids")
                || key.endsWith("_id") || key.endsWith("_ids"));
    }

    private void requireStringIds(JsonNode node) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                if (isIdKey(entry.getKey()) && entry.getValue().isNumber()) {
                    throw new IllegalArgumentException("业务流程协议中的 ID 必须使用字符串");
                }
                requireStringIds(entry.getValue());
            });
        } else if (node.isArray()) {
            node.forEach(this::requireStringIds);
        }
    }

    private void requireDeclared(String reference, Collection<String> declared,
                                 String code, String nodeId, String path,
                                 BusinessProcessValidationVO result) {
        if (reference == null || !declared.contains(reference)) {
            error(result, code, "节点引用未在 dependencies 中声明", nodeId, path,
                    "从受治理依赖目录重新选择并保存");
        }
    }

    private void requireAvailable(String reference, Set<String> available,
                                  String code, String nodeId, String path,
                                  BusinessProcessValidationVO result) {
        requireAvailable(reference, available, code, nodeId, path,
                "节点引用未发布、不属于当前应用或已失效", "选择当前应用中的已发布依赖", result);
    }

    private void requireAvailable(String reference, Set<String> available,
                                  String code, String nodeId, String path,
                                  String message, String action,
                                  BusinessProcessValidationVO result) {
        if (reference == null || !available.contains(reference)) {
            error(result, code, message, nodeId, path, action);
        }
    }

    private boolean reaches(String current, String target,
                            Map<String, Set<String>> graph,
                            Set<String> visited) {
        if (Objects.equals(current, target)) {
            return true;
        }
        if (!visited.add(current)) {
            return false;
        }
        for (String next : graph.getOrDefault(current, Set.of())) {
            if (reaches(next, target, graph, visited)) {
                return true;
            }
        }
        return false;
    }

    private int dependencyDepth(String current,
                                Map<String, Set<String>> graph,
                                Set<String> path) {
        if (!path.add(current)) {
            return Integer.MAX_VALUE;
        }
        int max = 0;
        for (String child : graph.getOrDefault(current, Set.of())) {
            int childDepth = dependencyDepth(child, graph, path);
            if (childDepth == Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            max = Math.max(max, childDepth + 1);
        }
        path.remove(current);
        return max;
    }

    /** 至少一条 fieldMappings 项配置了非空 field，空动作节点不可发布。 */
    private boolean hasConfiguredFieldMapping(Object rawMappings) {
        if (!(rawMappings instanceof Collection<?> mappings) || mappings.isEmpty()) {
            return false;
        }
        for (Object item : mappings) {
            if (!(item instanceof Map<?, ?> mapping)) {
                continue;
            }
            Object field = mapping.get("field");
            if (field == null) {
                field = mapping.get("targetField");
            }
            if (string(field) != null) {
                return true;
            }
        }
        return false;
    }

    private void error(BusinessProcessValidationVO result, String code, String message,
                       String nodeId, String path, String suggestion) {
        result.addError(code, message, nodeId, path, suggestion);
    }

    private String firstString(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            String value = string(values.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String string(Object value) {
        if (value == null) {
            return null;
        }
        String result = String.valueOf(value).trim();
        return result.isEmpty() ? null : result;
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value == null ? null : Boolean.valueOf(String.valueOf(value));
    }

    private String trim(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String upper(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private List<String> normalizeUpperList(List<String> source) {
        List<String> result = new ArrayList<>();
        for (String value : mutableList(source)) {
            String normalized = upper(value);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> normalizeNodePorts(BusinessProcessNode node) {
        List<String> ports = normalizeUpperList(node.getPorts());
        if ("APPROVAL".equals(node.getType())
                && new LinkedHashSet<>(ports).equals(APPROVAL_PORTS)) {
            return new ArrayList<>(APPROVAL_PORT_ORDER);
        }
        if (!"CONDITION".equals(node.getType())) {
            return ports;
        }

        List<String> branchPorts = new ArrayList<>();
        for (Object value : list(node.getConfig().get("branches"))) {
            String port = upper(string(map(value).get("port")));
            if (port != null && !branchPorts.contains(port)) {
                branchPorts.add(port);
            }
        }
        return !branchPorts.isEmpty()
                && new LinkedHashSet<>(branchPorts).equals(new LinkedHashSet<>(ports))
                ? branchPorts : ports;
    }

    private List<String> normalizeSortedList(List<String> source) {
        Set<String> unique = new LinkedHashSet<>();
        for (String value : mutableList(source)) {
            String normalized = trim(value);
            if (normalized != null) {
                unique.add(normalized);
            }
        }
        return new ArrayList<>(unique.stream().sorted().toList());
    }

    private <T> List<T> mutableList(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private List<?> list(Object value) {
        return value instanceof List<?> values ? values : List.of();
    }

    private Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> values)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
        return result;
    }

    private <K, V> Map<K, V> safeMap(Map<K, V> values) {
        return values == null ? Map.of() : values;
    }

    private <T> Set<T> safeSet(Set<T> values) {
        return values == null ? Set.of() : values;
    }

    private Map<String, Set<String>> safeDependencyMap(Map<String, Set<String>> values) {
        return values == null ? Map.of() : values;
    }
}
