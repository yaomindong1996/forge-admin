package com.mdframe.forge.plugin.capability.secureaction.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.execution.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.model.CapabilityRiskLevel;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.starter.core.annotation.api.OpenRestCapability;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 显式声明的系统 REST 方法桥接。无 HTTP 转发、任意 URL、用户 Header 或反射方法名输入。 */
public class RestEndpointSystemService implements SystemServiceCapabilityDefinition {
    public static final String CODE = "system.rest.invoke";
    private final ObjectProvider<RequestMappingHandlerMapping> mappings;
    private final ObjectMapper mapper;
    private final Validator validator;

    public RestEndpointSystemService(ObjectProvider<RequestMappingHandlerMapping> mappings,
                                     ObjectMapper mapper, Validator validator) {
        this.mappings = mappings;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Override public String serviceCode() { return CODE; }
    @Override public String definitionVersion() { return "1"; }
    @Override public String platformPermission() { return "ai:capability:system-service:invoke"; }

    @Override
    public SystemServiceRegistrationSource registrationSource(Long tenantId) {
        ObjectNode options = mapper.createObjectNode().put("registrationKind", "REST");
        var endpoints = options.putArray("endpoints");
        for (Endpoint endpoint : endpoints()) {
            ObjectNode item = endpoints.addObject();
            item.put("id", endpoint.id()).put("name", endpoint.metadata().name())
                    .put("method", endpoint.method()).put("path", endpoint.path())
                    .put("description", endpoint.metadata().description())
                    .put("permission", endpoint.metadata().permission());
            try {
                item.set("inputSchema", inputSchema(endpoint));
                item.put("available", true);
            } catch (BusinessException exception) {
                item.put("available", false).put("unavailableReason", exception.getMessage());
            }
        }
        ObjectNode parameters = object();
        parameters.putObject("properties").putObject("endpointId").put("type", "string");
        parameters.putArray("required").add("endpointId");
        // BOTH：USER 委托与 SERVICE 服务账号均可调用；仍走真实 LoginUser RBAC，禁止匿名身份。
        return new SystemServiceRegistrationSource(CODE, "系统 REST 接口", "选择声明为可开放的系统接口，保留业务权限和参数校验。",
                definitionVersion(), "BOTH", CapabilityRiskLevel.MEDIUM.name(), parameters, options);
    }

    @Override
    public SystemServicePublication preparePublication(Long tenantId, JsonNode parameters) {
        if (parameters == null || !parameters.isObject() || parameters.size() != 1
                || !parameters.path("endpointId").isTextual()) {
            throw new BusinessException("请选择一个系统 REST 接口");
        }
        Endpoint endpoint = requireEndpoint(parameters.path("endpointId").asText());
        ObjectNode input = inputSchema(endpoint);
        ObjectNode policy = mapper.createObjectNode().put("endpointId", endpoint.id())
                .put("endpointFingerprint", fingerprint(endpoint, input))
                .put("permission", endpoint.metadata().permission());
        policy.putObject("registrationParameters").put("endpointId", endpoint.id());
        policy.putObject("documentation").putArray("businessRules")
                .add("执行身份来自 USER 委托或 SERVICE 绑定服务账号，原业务权限和 DTO 校验继续生效。")
                .add("接口实现升级时应修改 OpenRestCapability.version；签名或契约漂移后旧能力拒绝执行。")
                .add("仅调用服务器代码显式声明的接口，不接收 URL、Header、租户或执行用户覆盖。");
        ObjectNode output = object().put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        ObjectNode outputs = output.putObject("properties");
        outputs.putObject("result").put("type", "object").put("additionalProperties", true);
        outputs.putObject("idempotentHit").put("type", "boolean");
        output.putArray("required").add("result");
        return new SystemServicePublication(endpoint.metadata().name(), endpoint.metadata().description(), input, output, policy);
    }

    @Override public Map<String, Object> prepareInput(Map<String, Object> payload) {
        if (payload != null && payload.containsKey("idempotencyKey")) {
            throw new BusinessException("幂等键应通过 Idempotency-Key 请求头传入");
        }
        return new LinkedHashMap<>(payload == null ? Map.of() : payload);
    }

    @Override
    public void validate(SecureActionDescriptor descriptor, Map<String, Object> input) {
        var identity = ExecutionIdentityContextHolder.current()
                .orElseThrow(() -> new BusinessException(401, "缺少可信能力执行身份"));
        // USER 委托与 SERVICE 服务账号（绑定真实用户/组织）均可；与开放网关 requiredActorType=BOTH 对齐。
        boolean trustedActor = CapabilityActorType.USER.name().equals(identity.actorType())
                || CapabilityActorType.SERVICE.name().equals(identity.actorType());
        if (!trustedActor || identity.actorUserId() == null || identity.loginUser() == null
                || identity.loginUser().getTenantId() == null
                || identity.loginUser().getActiveOrgId() == null) {
            throw new BusinessException(403, "USER_DELEGATION_REQUIRED");
        }
        Endpoint endpoint = requireEndpoint(descriptor.policySnapshot().path("endpointId").asText());
        if (!fingerprint(endpoint, inputSchema(endpoint)).equals(descriptor.policySnapshot().path("endpointFingerprint").asText())
                || !endpoint.metadata().permission().equals(descriptor.permission())) {
            throw new BusinessException(409, "REST_ENDPOINT_CHANGED");
        }
        if (!SessionHelper.hasPermission(endpoint.metadata().permission())) {
            throw new BusinessException(403, "无权执行该系统接口");
        }
        Map<String, Object> request = new LinkedHashMap<>(input);
        request.remove("idempotencyKey");
        new CapabilitySchemaValidator().validateInstance(descriptor.inputSchema(), mapper.valueToTree(request));
        bind(endpoint, input);
    }

    @Override
    public Map<String, Object> execute(SecureActionDescriptor descriptor, Map<String, Object> input, String requestId) {
        validate(descriptor, input);
        Endpoint endpoint = requireEndpoint(descriptor.policySnapshot().path("endpointId").asText());
        HandlerMethod handler = endpoint.handler().createWithResolvedBean();
        Object[] arguments = bind(endpoint, input);
        if (!validator.forExecutables().validateParameters(handler.getBean(), handler.getMethod(), arguments).isEmpty()) {
            throw new BusinessException("系统接口参数校验未通过");
        }
        try {
            Object result = handler.getMethod().invoke(handler.getBean(), arguments);
            if (!(result instanceof RespInfo<?> response) || !Integer.valueOf(200).equals(response.getCode())) {
                throw new BusinessException("系统接口执行未成功，请通过调用记录定位");
            }
            return Map.of("result", mapper.convertValue(response, Map.class));
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("系统接口执行失败");
        } catch (IllegalAccessException exception) {
            throw new BusinessException("系统接口方法不可访问");
        }
    }

    private Object[] bind(Endpoint endpoint, Map<String, Object> input) {
        ObjectNode schema = inputSchema(endpoint);
        var names = new java.util.HashSet<String>();
        schema.path("properties").fieldNames().forEachRemaining(names::add);
        names.add("idempotencyKey"); // 网关在契约验证后注入，永不绑定为接口参数。
        if (!names.containsAll(input.keySet())) { throw new BusinessException("REST 参数包含未声明字段"); }
        var parameters = endpoint.handler().getMethodParameters();
        Object[] result = new Object[parameters.length];
        for (int index = 0; index < parameters.length; index++) {
            var parameter = parameters[index];
            String name = parameterName(parameter);
            if (!input.containsKey(name) && required(parameter)) { throw new BusinessException("缺少参数: " + name); }
            Object raw = input.get(name);
            RequestParam query = parameter.getParameterAnnotation(RequestParam.class);
            if (raw == null && query != null && !org.springframework.web.bind.annotation.ValueConstants.DEFAULT_NONE.equals(query.defaultValue())) {
                raw = query.defaultValue();
            }
            try {
                result[index] = mapper.convertValue(raw, mapper.constructType(parameter.getGenericParameterType()));
                if (result[index] == null && required(parameter)) { throw new BusinessException("必填参数不能为空: " + name); }
                if (result[index] != null && parameter.hasParameterAnnotation(RequestBody.class)
                        && !validator.validate(result[index]).isEmpty()) { throw new BusinessException("请求体校验未通过"); }
            } catch (IllegalArgumentException exception) { throw new BusinessException("参数类型不正确: " + name); }
        }
        return result;
    }

    private ObjectNode inputSchema(Endpoint endpoint) {
        if (endpoint.metadata().permission().isBlank() || !RespInfo.class.isAssignableFrom(endpoint.handler().getReturnType().getParameterType())) {
            throw new BusinessException("接口必须声明业务权限并返回 RespInfo");
        }
        ObjectNode schema = object().put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        ObjectNode properties = schema.putObject("properties");
        var required = schema.putArray("required");
        RestCapabilitySchema types = new RestCapabilitySchema(mapper);
        for (var parameter : endpoint.handler().getMethodParameters()) {
            String name = parameterName(parameter);
            if (properties.has(name) || "idempotencyKey".equals(name)) { throw new BusinessException("接口参数名重复或使用保留参数名"); }
            properties.set(name, types.describe(mapper.constructType(parameter.getGenericParameterType())));
            if (required(parameter)) { required.add(name); }
        }
        return schema;
    }

    private String parameterName(org.springframework.core.MethodParameter parameter) {
        parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
        if (parameter.hasParameterAnnotation(RequestBody.class)) { return "body"; }
        RequestParam query = parameter.getParameterAnnotation(RequestParam.class);
        PathVariable path = parameter.getParameterAnnotation(PathVariable.class);
        if (query == null && path == null) { throw new BusinessException("仅支持显式声明的路径参数、查询参数及 JSON 请求体"); }
        String name = query != null ? query.name() : path.name();
        if (name.isBlank()) { name = query != null ? query.value() : path.value(); }
        if (name.isBlank()) { name = parameter.getParameterName(); }
        if (name == null || name.isBlank()) { throw new BusinessException("接口缺少稳定参数名"); }
        return name;
    }

    private boolean required(org.springframework.core.MethodParameter parameter) {
        RequestBody body = parameter.getParameterAnnotation(RequestBody.class);
        RequestParam query = parameter.getParameterAnnotation(RequestParam.class);
        return body != null ? body.required() : query == null || query.required()
                && org.springframework.web.bind.annotation.ValueConstants.DEFAULT_NONE.equals(query.defaultValue());
    }

    private List<Endpoint> endpoints() {
        List<Endpoint> result = new ArrayList<>();
        mappings.orderedStream().forEach(mapping -> mapping.getHandlerMethods().forEach((route, handler) -> {
            OpenRestCapability metadata = handler.getMethodAnnotation(OpenRestCapability.class);
            if (metadata == null || !route.getHeadersCondition().isEmpty() || !route.getParamsCondition().isEmpty()) { return; }
            for (String path : route.getPatternValues()) {
                for (var method : route.getMethodsCondition().getMethods()) {
                    String id = DigestUtils.md5DigestAsHex((method.name() + " " + path).getBytes(StandardCharsets.UTF_8));
                    result.add(new Endpoint(id, method.name(), path, handler, metadata));
                }
            }
        }));
        return result.stream().sorted(Comparator.comparing(Endpoint::path)).toList();
    }

    private Endpoint requireEndpoint(String id) {
        List<Endpoint> matches = endpoints().stream().filter(endpoint -> endpoint.id().equals(id)).toList();
        if (matches.size() != 1) { throw new BusinessException("系统接口未声明为可开放、已移除或路由不唯一"); }
        return matches.get(0);
    }

    private String fingerprint(Endpoint endpoint, ObjectNode input) {
        String contract = endpoint.handler().getMethod().toGenericString() + endpoint.metadata().permission()
                + endpoint.metadata().version() + input;
        return DigestUtils.md5DigestAsHex(contract.getBytes(StandardCharsets.UTF_8));
    }

    private ObjectNode object() { return mapper.createObjectNode().put("type", "object").put("additionalProperties", false); }
    private record Endpoint(String id, String method, String path, HandlerMethod handler, OpenRestCapability metadata) { }
}
