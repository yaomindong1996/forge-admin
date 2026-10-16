package com.mdframe.forge.plugin.capability.secureaction.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.execution.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.model.CapabilityRiskLevel;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.secureaction.schema.LowcodeCapabilitySchemaTypeResolver;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStatus;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.manager.DynamicCrudCreateManager;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectService;
import com.mdframe.forge.plugin.generator.mapper.BusinessDocumentConfigMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** 从已发布单表表单创建记录，不需要把前端“打开页面”动作伪装成服务端动作。 */
public class LowcodeFormSystemService implements SystemServiceCapabilityDefinition {
    public static final String CODE = "lowcode.form.create";
    private static final Set<String> MANAGED = Set.of("id", "tenantid", "createby", "createtime", "createdept",
            "updateby", "updatetime", "delflag", "deleted", "businesskey", "processinstanceid",
            "status", "flowstatus", "businessstatus", "starterid", "starteruserid", "owneruserid", "ownerdeptid");
    private final BusinessObjectService objects;
    private final BusinessObjectActionService actions;
    private final AiCrudConfigService configs;
    private final DynamicCrudCreateManager formCreate;
    private final LowcodeFormInvocationGuard invocations;
    private final LowcodeRuntimeDataSourceResolver datasourceResolver;
    private final ObjectMapper mapper;
    private final CapabilitySchemaValidator validator;
    private final BusinessDocumentConfigMapper documents;

    public LowcodeFormSystemService(BusinessObjectService objects, BusinessObjectActionService actions,
            AiCrudConfigService configs, DynamicCrudCreateManager formCreate, LowcodeFormInvocationGuard invocations,
            LowcodeRuntimeDataSourceResolver datasourceResolver, ObjectMapper mapper, CapabilitySchemaValidator validator,
            BusinessDocumentConfigMapper documents) {
        this.objects = objects; this.actions = actions; this.configs = configs; this.formCreate = formCreate;
        this.invocations = invocations; this.datasourceResolver = datasourceResolver; this.mapper = mapper; this.validator = validator;
        this.documents = documents;
    }

    @Override public String serviceCode() { return CODE; }
    @Override public String definitionVersion() { return "1"; }
    @Override public String platformPermission() { return "ai:capability:system-service:invoke"; }

    @Override
    public SystemServiceRegistrationSource registrationSource(Long tenantId) {
        requireTenant(tenantId);
        ObjectNode options = mapper.createObjectNode().put("registrationKind", "FORM");
        var forms = options.putArray("forms");
        BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
        query.setStatus(EnableStatus.ENABLED.getCode());
        for (var object : objects.list(query)) {
            if (object.getLastPublishVersion() == null || object.getLastPublishVersion() <= 0) { continue; }
            addFormSource(forms, tenantId, object);
        }
        return registrationSource(options);
    }

    @Override
    public SystemServiceRegistrationSource registrationSource(
            Long tenantId,
            SystemServiceRegistrationContext context) {
        requireTenant(tenantId);
        if (context == null || context.objectId() == null || context.objectId() <= 0) {
            return registrationSource(tenantId);
        }
        ObjectNode options = mapper.createObjectNode().put("registrationKind", "FORM");
        ArrayNode forms = options.putArray("forms");
        BusinessObjectVO object = objects.detail(context.objectId());
        if (EnableStatus.ENABLED.matches(object.getStatus())
                && object.getLastPublishVersion() != null && object.getLastPublishVersion() > 0) {
            addFormSource(forms, tenantId, object);
        }
        return registrationSource(options);
    }

    private SystemServiceRegistrationSource registrationSource(ObjectNode options) {
        return new SystemServiceRegistrationSource(CODE, "低代码表单填报", "复用已发布表单的新增逻辑及原有创建事件；需要显式送审时请选择流程操作。",
                definitionVersion(), CapabilityActorType.USER.name(), CapabilityRiskLevel.MEDIUM.name(), object(), options);
    }

    private void addFormSource(ArrayNode forms, Long tenantId, BusinessObjectVO object) {
        ObjectNode item = forms.addObject().put("id", object.getSuiteCode() + "/" + object.getObjectCode())
                .put("objectId", String.valueOf(object.getId())).put("name", object.getObjectName())
                .put("suiteCode", object.getSuiteCode()).put("objectCode", object.getObjectCode());
        try {
            Source source = source(tenantId, object.getSuiteCode(), object.getObjectCode());
            item.put("available", true).put("publishedVersion", source.version());
            var fields = item.putArray("fields");
            source.fields().values().forEach(field -> fields.addObject().put("field", field.getField())
                    .put("label", field.getLabel()).put("required", Boolean.TRUE.equals(field.getRequired())));
        } catch (BusinessException exception) {
            item.put("available", false).put("unavailableReason", exception.getMessage());
        } catch (RuntimeException exception) {
            // 单项表单发布快照解析失败时不能拖垮整个 registration-source 列表
            item.put("available", false).put("unavailableReason", "表单发布版本不可用");
        }
    }

    @Override
    public SystemServicePublication preparePublication(Long tenantId, JsonNode parameters) {
        requireTenant(tenantId);
        if (parameters == null || !parameters.isObject()) { throw new BusinessException("缺少表单来源"); }
        parameters.fieldNames().forEachRemaining(key -> {
            if (!Set.of("suiteCode", "objectCode", "allowedFields", "requiredFields").contains(key)) {
                throw new BusinessException("未知表单发布参数");
            }
        });
        Source source = source(tenantId, parameters.path("suiteCode").asText(), parameters.path("objectCode").asText());
        Set<String> allowed = stringSet(parameters.path("allowedFields"));
        Set<String> required = stringSet(parameters.path("requiredFields"));
        source.fields().values().stream().filter(field -> Boolean.TRUE.equals(field.getRequired()))
                .forEach(field -> required.add(field.getField()));
        if (allowed.isEmpty() || !source.fields().keySet().containsAll(allowed) || !allowed.containsAll(required)) {
            throw new BusinessException("允许字段必须来自当前发布表单，且必须包含全部必填字段");
        }
        ObjectNode input = object().put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        ObjectNode data = object();
        ObjectNode fields = data.putObject("properties");
        allowed.forEach(name -> {
            LowcodeFieldSchema field = source.fields().get(name);
            ObjectNode schema = fields.putObject(name).put("type", LowcodeCapabilitySchemaTypeResolver.resolve(field))
                    .put("description", Objects.toString(field.getLabel(), name));
            if ("string".equals(schema.path("type").asText()) && field.getLength() != null && field.getLength() > 0) {
                schema.put("maxLength", field.getLength());
            }
        });
        data.set("required", mapper.valueToTree(required));
        input.putObject("properties").set("data", data);
        input.putArray("required").add("data");
        ObjectNode policy = mapper.createObjectNode().put("suiteCode", source.suite()).put("objectCode", source.code())
                .put("configKey", source.configKey()).put("publishedObjectVersion", source.version())
                .put("runtimeVersion", source.runtimeVersion()).put("permission", permission(source.code()))
                .put("storageKey", source.storageKey());
        policy.set("registrationParameters", parameters.deepCopy());
        policy.putObject("documentation").putArray("businessRules")
                .add("调用必须使用用户委托身份；只执行新增及原有创建事件，不额外发起审批。")
                .add("复用应用内表单新增接口的业务链路及数据源路由，无需另建业务动作。")
                .add("重复请求使用相同 Idempotency-Key；成功调用返回原记录。独立运行库结果不确定时先核对记录，不要换键自动重试。")
                .add("重新发布业务对象或运行配置后，应审核并发布能力新版本。");
        ObjectNode output = object().put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        output.putObject("properties").putObject("recordId").put("type", "string");
        ((ObjectNode) output.path("properties")).putObject("idempotentHit").put("type", "boolean");
        output.putArray("required").add("recordId");
        return new SystemServicePublication(source.name() + " · 表单填报", "向 " + source.name() + " 新增记录", input, output, policy);
    }

    @Override
    public Map<String, Object> prepareInput(Map<String, Object> payload) {
        if (payload == null || !Set.of("data").containsAll(payload.keySet())) { throw new BusinessException("表单输入只允许 data"); }
        return new LinkedHashMap<>(payload);
    }

    @Override
    public void validate(SecureActionDescriptor descriptor, Map<String, Object> input) {
        validatedSource(descriptor, input);
    }

    private Source validatedSource(SecureActionDescriptor descriptor, Map<String, Object> input) {
        ExecutionIdentity identity = identity();
        JsonNode policy = descriptor.policySnapshot();
        Source source = source(identity.loginUser().getTenantId(), policy.path("suiteCode").asText(), policy.path("objectCode").asText());
        if (!source.configKey().equals(policy.path("configKey").asText())
                || source.version() != policy.path("publishedObjectVersion").asInt()
                || source.runtimeVersion() != policy.path("runtimeVersion").asInt()
                || !permission(source.code()).equals(descriptor.permission())
                || policy.has("storageKey") && !source.storageKey().equals(policy.path("storageKey").asText())) {
            throw new BusinessException(409, "FORM_SOURCE_CHANGED");
        }
        if (!SessionHelper.hasPermission(descriptor.permission())) { throw new BusinessException(403, "无权填报该业务对象"); }
        descriptor.inputSchema().path("properties").path("data").path("properties").fieldNames().forEachRemaining(field -> {
            if (!source.fields().containsKey(field)) { throw new BusinessException(409, "FORM_FIELD_POLICY_CHANGED"); }
        });
        Map<String, Object> request = new LinkedHashMap<>(input);
        request.remove("idempotencyKey");
        validator.validateInstance(descriptor.inputSchema(), mapper.valueToTree(request));
        return source;
    }

    @Override
    public Map<String, Object> execute(SecureActionDescriptor descriptor, Map<String, Object> input, String requestId) {
        Source source = validatedSource(descriptor, input);
        ExecutionIdentity identity = identity();
        String key = Objects.toString(input.get("idempotencyKey"), "");
        if (key.isBlank()) { throw new BusinessException("missing_idempotency_key"); }
        String keyHash = hash(key);
        Long tenant = identity.loginUser().getTenantId();
        Long actor = identity.actorUserId();
        Long org = identity.loginUser().getActiveOrgId();
        String digest = hash(descriptor.version() + ":" + actor + ":" + org + ":" + canonical(mapper.valueToTree(input.get("data"))));
        var request = new LowcodeFormInvocationGuard.Request(tenant, identity.clientId(), descriptor.capabilityId(), keyHash, digest, actor, org);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = new LinkedHashMap<>((Map<String, Object>) input.get("data"));
        return invocations.execute(request, source.local(), () -> formCreate.create(source.configKey(), data));
    }

    private Source source(Long tenantId, String suite, String code) {
        var published = actions.resolvePublishedActions(suite, code, null);
        var version = published.version();
        if (!tenantId.equals(published.object().getTenantId()) || version == null || version.getConfigKey() == null) {
            throw new BusinessException("表单发布来源不可用");
        }
        var current = configs.getByConfigKey(version.getConfigKey());
        if (current == null || !tenantId.equals(current.getTenantId()) || !BusinessApplicationPublishStatus.PUBLISHED.matches(current.getPublishStatus())) {
            throw new BusinessException("表单运行配置未发布");
        }
        var runtime = configs.resolvePublishedRuntimeConfig(current);
        LowcodeModelSchema model;
        try { model = mapper.readValue(version.getModelSnapshot(), LowcodeModelSchema.class); }
        catch (Exception exception) { throw new BusinessException("表单发布模型不可解析"); }
        // 与普通新增使用同一解析器；独立运行数据源不是不支持表单新增的理由。
        var target = datasourceResolver.resolve(runtime);
        var modelTarget = datasourceResolver.resolve(model);
        if (target.isMaster() != modelTarget.isMaster() || !Objects.equals(target.getDatasourceId(), modelTarget.getDatasourceId())) {
            throw new BusinessException("表单发布模型与运行配置的数据源不一致，请重新发布表单");
        }
        if (target.isReadonly() || !target.isAllowWrite()) {
            throw new BusinessException("表单运行数据源不可写，请检查数据源配置");
        }
        if (model.getChildren() != null && !model.getChildren().isEmpty() || "MASTER_DETAIL".equals(model.getAppType())) {
            throw new BusinessException("主子表请注册包含明细校验的业务动作；通用填报当前支持单表表单");
        }
        Set<String> reserved = new HashSet<>(MANAGED);
        var document = documents.selectByObjectId(tenantId, published.object().getId());
        if (document != null) {
            reserved.add(normalize(document.getStatusField()));
            reserved.add(normalize(document.getStarterField()));
            reserved.add(normalize(document.getOwnerField()));
        }
        if (model.getPolicies() != null) {
            var p = model.getPolicies();
            for (String value : Arrays.asList(p.getUserField(), p.getUserColumn(), p.getOrgField(), p.getOrgColumn(),
                    p.getTenantField(), p.getTenantColumn(), p.getPrimaryKeyField(), p.getLogicDeleteField(), p.getLogicDeleteColumn())) {
                reserved.add(normalize(value));
            }
        }
        Map<String, LowcodeFieldSchema> fields = new LinkedHashMap<>();
        // 可选策略字段未配置时不能把空列名视为系统字段。
        reserved.remove("");
        for (LowcodeFieldSchema field : model.getFields() == null ? List.<LowcodeFieldSchema>of() : model.getFields()) {
            if (field == null || field.getField() == null || Boolean.TRUE.equals(field.getSystemField())
                    || Boolean.TRUE.equals(field.getReadonly()) || Boolean.TRUE.equals(field.getPrimaryKey())
                    || Boolean.TRUE.equals(field.getAutoIncrement()) || Boolean.FALSE.equals(field.getFormVisible())
                    || field.getFormulaConfig() != null || LowcodeFormFieldStatus.HIDDEN.matches(field.getFieldStatus())
                    || LowcodeFormFieldStatus.DISABLED.matches(field.getFieldStatus())
                    || reserved.contains(normalize(field.getField())) || reserved.contains(normalize(field.getColumnName()))) { continue; }
            if (Set.of("object", "array").contains(LowcodeCapabilitySchemaTypeResolver.resolve(field))) {
                throw new BusinessException("表单含结构化字段，请使用具备明确明细字段契约的业务动作");
            }
            fields.put(field.getField(), field);
        }
        if (fields.isEmpty() || version.getPublishVersion() == null || runtime.getPublishedVersion() == null) {
            throw new BusinessException("表单无可写字段或缺少发布版本");
        }
        String storageKey = (target.isMaster() ? "master" : "runtime:" + target.getDatasourceId()) + ":" + target.getTableName();
        return new Source(suite, code, published.object().getObjectName(), version.getConfigKey(), version.getPublishVersion(), runtime.getPublishedVersion(), fields, target.isMaster(), storageKey);
    }

    private ExecutionIdentity identity() {
        ExecutionIdentity identity = ExecutionIdentityContextHolder.current().orElseThrow(() -> new BusinessException(401, "缺少可信身份"));
        if (!CapabilityActorType.USER.name().equals(identity.actorType()) || identity.actorUserId() == null
                || identity.loginUser().getTenantId() == null || identity.loginUser().getActiveOrgId() == null) {
            throw new BusinessException(403, "USER_DELEGATION_REQUIRED");
        }
        return identity;
    }
    private void requireTenant(Long tenant) {
        if (tenant == null || !tenant.equals(SessionHelper.getTenantId())) { throw new BusinessException(403, "租户上下文不匹配"); }
    }
    private Set<String> stringSet(JsonNode node) {
        if (!node.isArray()) { throw new BusinessException("字段白名单必须为数组"); }
        Set<String> result = new LinkedHashSet<>();
        node.forEach(item -> { if (!item.isTextual()) { throw new BusinessException("字段名必须是字符串"); } result.add(item.asText()); });
        return result;
    }
    private String permission(String code) { return "ai:business:" + code + ":add"; }
    private String normalize(String value) { return Objects.toString(value, "").replace("_", "").toLowerCase(Locale.ROOT); }
    private ObjectNode object() { return mapper.createObjectNode().put("type", "object").put("additionalProperties", false); }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private JsonNode canonical(JsonNode node) {
        if (!node.isObject()) { return node; }
        ObjectNode result = mapper.createObjectNode();
        var names = new TreeSet<String>(); node.fieldNames().forEachRemaining(names::add);
        names.forEach(name -> result.set(name, canonical(node.path(name)))); return result;
    }
    private record Source(String suite, String code, String name, String configKey, int version, int runtimeVersion,
                          Map<String, LowcodeFieldSchema> fields, boolean local, String storageKey) { }
}
