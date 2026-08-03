package com.mdframe.forge.plugin.capability.controlplane.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityVersion;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityMapper;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityVersionMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CapabilityOpenApiDocumentService {

    private static final String DEFAULT_OPENAPI_RESOURCE = "http://localhost:8580/openapi";

    private static final Map<String, String> ERROR_CODES = Map.ofEntries(
            Map.entry("UNAUTHORIZED", "凭据无效、过期或 Token resource 不匹配"),
            Map.entry("REPLAY_REJECTED", "签名时间戳、Nonce 或防重放校验失败"),
            Map.entry("FORBIDDEN", "客户端未授权、scope 不足或调用用户权限不足"),
            Map.entry("ACTOR_TYPE_NOT_ALLOWED", "SERVICE/USER 调用主体与能力要求不匹配"),
            Map.entry("RESOURCE_NOT_FOUND", "业务记录不存在或不在当前委托用户的数据权限范围内"),
            Map.entry("RATE_LIMITED", "客户端调用频率超过平台限制"),
            Map.entry("SCHEMA_INVALID", "Header 或请求体不符合当前发布版本契约"),
            Map.entry("CONFLICT", "发布来源、授权策略或运行时业务快照已变化"),
            Map.entry("INTERNAL_ERROR", "网关依赖不可用或能力执行失败"));

    private final AiCapabilityMapper capabilityMapper;
    private final AiCapabilityVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final String openapiResource;

    @Autowired
    public CapabilityOpenApiDocumentService(
            AiCapabilityMapper capabilityMapper,
            AiCapabilityVersionMapper versionMapper,
            ObjectMapper objectMapper,
            @Value("${forge.capability.identity.openapi-resource:http://localhost:8580/openapi}")
            String openapiResource) {
        this.capabilityMapper = capabilityMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
        this.openapiResource = StringUtils.defaultIfBlank(openapiResource, DEFAULT_OPENAPI_RESOURCE);
    }

    CapabilityOpenApiDocumentService(
            AiCapabilityMapper capabilityMapper,
            AiCapabilityVersionMapper versionMapper,
            ObjectMapper objectMapper) {
        this(capabilityMapper, versionMapper, objectMapper, DEFAULT_OPENAPI_RESOURCE);
    }

    public CapabilityOpenApiDocument generate(Long tenantId, Long capabilityId) {
        DocumentContext context = requirePublished(tenantId, capabilityId);
        ObjectNode document = objectMapper.createObjectNode();
        document.put("openapi", "3.1.0");
        document.set("info", info(context));
        document.putArray("servers").addObject().put("url", baseUrl());
        document.set("paths", paths(context));
        document.set("components", components(context));
        document.put("x-forge-capability-code", context.capability().getCapabilityCode());
        document.put("x-forge-capability-version", context.version().getVersion());
        document.put("x-forge-schema-checksum", context.version().getSchemaChecksum());
        document.put("x-forge-required-actor-type", context.version().getRequiredActorType());
        document.put("x-forge-source-type", context.version().getSourceType());
        document.put("x-forge-behavior", context.version().getBehavior());
        document.put("x-forge-oauth-resource", openapiResource);
        try {
            byte[] content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(document);
            return new CapabilityOpenApiDocument(fileBase(context) + "-openapi.json", content);
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException("能力 OpenAPI 文档生成失败");
        }
    }

    public CapabilityMarkdownDocument generateMarkdown(Long tenantId, Long capabilityId) {
        DocumentContext context = requirePublished(tenantId, capabilityId);
        String content = markdown(context);
        return new CapabilityMarkdownDocument(
                fileBase(context) + "-调用指南.md", content.getBytes(StandardCharsets.UTF_8));
    }

    public JsonNode requestExample(Long tenantId, Long capabilityId) {
        return example(requirePublished(tenantId, capabilityId).inputSchema());
    }

    public JsonNode requestExample(Long tenantId, Long capabilityId, String version) {
        if (tenantId == null || tenantId <= 0 || capabilityId == null || capabilityId <= 0
                || StringUtils.isBlank(version)) {
            throw new BusinessException("能力请求示例参数无效");
        }
        AiCapabilityVersion capabilityVersion = versionMapper.selectVersion(
                tenantId, capabilityId, version);
        if (capabilityVersion == null || !"PUBLISHED".equals(capabilityVersion.getStatus())) {
            throw new BusinessException("能力授权版本不存在或未发布");
        }
        JsonNode inputSchema = sanitizeRequestSchema(
                readObject(capabilityVersion.getInputSchema(), "输入"));
        return example(inputSchema);
    }

    private DocumentContext requirePublished(Long tenantId, Long capabilityId) {
        if (tenantId == null || tenantId <= 0 || capabilityId == null || capabilityId <= 0) {
            throw new BusinessException("能力文档参数无效");
        }
        AiCapability capability = capabilityMapper.selectTenantById(tenantId, capabilityId);
        if (capability == null || !"PUBLISHED".equals(capability.getPublishStatus())
                || !Integer.valueOf(1).equals(capability.getEnabled())
                || StringUtils.isBlank(capability.getCurrentVersion())) {
            throw new BusinessException("能力未发布或不可用");
        }
        AiCapabilityVersion version = versionMapper.selectVersion(
                tenantId, capabilityId, capability.getCurrentVersion());
        if (version == null || !"PUBLISHED".equals(version.getStatus())) {
            throw new BusinessException("能力当前发布版本不存在");
        }
        JsonNode inputSchema = sanitizeRequestSchema(readObject(version.getInputSchema(), "输入"));
        JsonNode outputSchema = readObject(version.getOutputSchema(), "输出");
        JsonNode policy = readOptionalObject(version.getPolicySnapshot());
        return new DocumentContext(capability, version, inputSchema, outputSchema, policy);
    }

    private ObjectNode info(DocumentContext context) {
        ObjectNode info = objectMapper.createObjectNode();
        info.put("title", context.capability().getCapabilityName());
        info.put("version", context.version().getVersion());
        info.put("description", StringUtils.defaultIfBlank(
                context.capability().getDescription(), "Forge 统一能力开放网关调用契约"));
        return info;
    }

    private ObjectNode paths(DocumentContext context) {
        ObjectNode paths = objectMapper.createObjectNode();
        ObjectNode operation = paths.putObject(invokePath(context)).putObject("post");
        operation.put("operationId", "invoke_"
                + context.capability().getCapabilityCode().replace('.', '_'));
        operation.put("summary", "调用" + context.capability().getCapabilityName());
        operation.put("description", operationDescription(context));
        operation.putArray("tags").add("Forge Capability");
        operation.set("security", security(context.version().getRequiredActorType()));
        operation.set("parameters", parameters(isWrite(context)));
        operation.putObject("requestBody")
                .put("required", true)
                .putObject("content")
                .putObject("application/json")
                .set("schema", context.inputSchema());
        operation.set("responses", responses());
        return paths;
    }

    private String operationDescription(DocumentContext context) {
        List<String> rules = documentationItems(context.policy(), "businessRules");
        return rules.isEmpty()
                ? "通过 Forge 统一能力开放网关调用当前不可变发布版本。"
                : "通过 Forge 统一能力开放网关调用当前不可变发布版本。业务规则："
                + String.join("；", rules);
    }

    private ArrayNode security(String actorType) {
        ArrayNode security = objectMapper.createArrayNode();
        security.addObject().putArray("bearerAuth");
        if (!"USER".equals(actorType)) {
            ObjectNode hmac = security.addObject();
            hmac.putArray("hmacAppId");
            hmac.putArray("hmacTimestamp");
            hmac.putArray("hmacNonce");
            hmac.putArray("hmacSignature");
        }
        return security;
    }

    private ArrayNode parameters(boolean idempotencyRequired) {
        ArrayNode parameters = objectMapper.createArrayNode();
        ObjectNode idempotency = parameters.addObject();
        idempotency.put("name", "Idempotency-Key");
        idempotency.put("in", "header");
        idempotency.put("required", idempotencyRequired);
        idempotency.put("description", "写能力必须提供；相同客户端、能力和 Key 复用首次结果。");
        idempotency.putObject("schema")
                .put("type", "string").put("minLength", 8).put("maxLength", 128)
                .put("pattern", "^[A-Za-z0-9._:-]+$");
        return parameters;
    }

    private ObjectNode responses() {
        ObjectNode responses = objectMapper.createObjectNode();
        response(responses, "200", "调用成功");
        response(responses, "400", "请求参数无效");
        response(responses, "401", "认证失败或签名重放");
        response(responses, "403", "未授权或主体类型不匹配");
        response(responses, "404", "业务资源不存在或当前调用用户不可见");
        response(responses, "409", "能力来源、授权或业务快照冲突");
        response(responses, "429", "调用频率超限");
        response(responses, "500", "能力执行失败");
        response(responses, "503", "依赖服务暂不可用");
        return responses;
    }

    private void response(ObjectNode responses, String status, String description) {
        ObjectNode response = responses.putObject(status);
        response.put("description", description);
        response.putObject("headers").putObject("X-Request-Id")
                .put("description", "调用追踪标识")
                .putObject("schema").put("type", "string");
        response.putObject("content").putObject("application/json")
                .putObject("schema").put("$ref", "#/components/schemas/CapabilityResponse");
    }

    private ObjectNode components(DocumentContext context) {
        ObjectNode components = objectMapper.createObjectNode();
        ObjectNode schemes = components.putObject("securitySchemes");
        schemes.putObject("bearerAuth")
                .put("type", "http")
                .put("scheme", "bearer")
                .put("bearerFormat", "Forge fdu access token")
                .put("description", "通过 /oauth2/token 获取，resource 必须为 " + openapiResource);
        apiKey(schemes, "hmacAppId", "X-Forge-App-Id", "SERVICE/HYBRID 客户端 ID");
        apiKey(schemes, "hmacTimestamp", "X-Forge-Timestamp", "Unix epoch 毫秒时间戳");
        apiKey(schemes, "hmacNonce", "X-Forge-Nonce", "单次随机数，禁止重放");
        apiKey(schemes, "hmacSignature", "X-Forge-Signature",
                "HMAC-SHA256 hex(appId\\ntimestamp\\nnonce\\nMETHOD\\nURI\\nsha256Hex(body))");

        ObjectNode response = components.putObject("schemas").putObject("CapabilityResponse");
        response.put("type", "object");
        response.put("additionalProperties", false);
        response.putArray("required").add("code").add("message")
                .add("requestId").add("timestamp").add("data");
        ObjectNode properties = response.putObject("properties");
        ArrayNode codes = properties.putObject("code").put("type", "string").putArray("enum");
        codes.add("SUCCESS");
        ERROR_CODES.keySet().stream().sorted().forEach(codes::add);
        properties.putObject("message").put("type", "string");
        properties.putObject("requestId").put("type", "string");
        properties.putObject("timestamp").put("type", "integer").put("format", "int64");
        properties.set("data", context.outputSchema());
        return components;
    }

    private void apiKey(ObjectNode schemes, String key, String header, String description) {
        schemes.putObject(key).put("type", "apiKey").put("in", "header")
                .put("name", header).put("description", description);
    }

    private String markdown(DocumentContext context) {
        StringBuilder output = new StringBuilder(8192);
        output.append("# ").append(context.capability().getCapabilityName()).append(" 调用指南\n\n");
        output.append(StringUtils.defaultIfBlank(
                context.capability().getDescription(), "通过 Forge 统一能力开放网关调用该能力。"))
                .append("\n\n");
        output.append("> 本文档由已发布能力版本自动生成，版本内容不可变。文档不包含任何真实密钥或 Token。\n\n");
        section(output, "基本信息");
        table(output, List.of("项目", "值"), List.of(
                List.of("能力编码", code(context.capability().getCapabilityCode())),
                List.of("能力版本", code(context.version().getVersion())),
                List.of("请求地址", code("POST " + invokeUrl(context))),
                List.of("来源类型", code(context.version().getSourceType())),
                List.of("行为/风险", code(context.version().getBehavior() + " / " + context.version().getRiskLevel())),
                List.of("调用主体", actorDescription(context.version().getRequiredActorType())),
                List.of("Schema 校验和", code(context.version().getSchemaChecksum()))));

        section(output, "认证方式");
        output.append("OAuth 2.1 Bearer Token 始终可用，申请 Token 时 `resource` 必须精确填写 `")
                .append(openapiResource).append("`。MCP resource 签发的 Token 不能调用本 REST 接口。\n\n");
        if (!"USER".equals(context.version().getRequiredActorType())) {
            output.append("SERVICE/HYBRID 客户端也可使用 HMAC-SHA256 签名。签名密钥只在创建或轮换时展示一次。\n\n");
        }
        else {
            output.append("该能力必须使用 USER 委托 Token；HMAC 和 SERVICE `client_credentials` 不可调用。\n\n");
            output.append("有统一身份源时使用受信 OIDC JWT；没有统一 OIDC 时，在客户端页面启用 RSA 用户断言并预绑定外围 `sub`，"
                    + "然后使用专用 `urn:forge:params:oauth:token-type:user-assertion+jwt`。两种验签路径不会互相回退。\n\n");
        }
        table(output, List.of("Header", "必填", "说明"), headerRows(context));

        section(output, "请求参数");
        appendSchemaTable(output, context.inputSchema(), "body");
        appendNotes(output, "请求说明", documentationItems(context.policy(), "requestNotes"));
        jsonBlock(output, example(context.inputSchema()));

        section(output, "返回参数");
        table(output, List.of("字段", "类型", "说明"), List.of(
                List.of("code", "string", "`SUCCESS` 或稳定错误码"),
                List.of("message", "string", "结果说明，不应依赖文案做程序判断"),
                List.of("requestId", "string", "调用追踪标识，排障时提供"),
                List.of("timestamp", "integer(int64)", "服务端 Unix 毫秒时间戳"),
                List.of("data", "object", "能力返回数据，结构如下")));
        appendSchemaTable(output, context.outputSchema(), "data");
        appendNotes(output, "返回说明", documentationItems(context.policy(), "responseNotes"));
        ObjectNode responseExample = objectMapper.createObjectNode();
        responseExample.put("code", "SUCCESS");
        responseExample.put("message", "调用成功");
        responseExample.put("requestId", "req-example-001");
        responseExample.put("timestamp", 1785598800000L);
        responseExample.set("data", example(context.outputSchema()));
        jsonBlock(output, responseExample);

        section(output, "业务校验与权限");
        List<String> rules = new ArrayList<>(documentationItems(context.policy(), "businessRules"));
        if (rules.isEmpty()) {
            rules.add("执行前重新校验发布来源、授权版本和运行时业务状态；任何快照不一致均拒绝执行。");
        }
        rules.add("租户、用户和活动组织只从可信凭据解析，不接受请求方指定。");
        rules.add(isWrite(context)
                ? "写能力必须携带 Idempotency-Key；相同客户端、能力和 Key 返回首次结果。"
                : "只读能力不强制 Idempotency-Key。");
        bulletList(output, rules);
        output.append("所需平台权限：").append(code(platformPermission(context))).append("\n\n");
        String permission = context.policy().path("permission").asText();
        output.append("所需业务权限：").append(code(StringUtils.defaultIfBlank(permission, "未声明（调用会失败关闭）")))
                .append("\n\n");
        if ("USER".equals(context.version().getRequiredActorType())) {
            output.append("> 最终权限按实际委托用户实时校验，客户端授权不会替代用户权限。\n\n");
        }

        section(output, "限流与幂等");
        output.append("平台默认读能力 120 次/分钟/客户端、写能力 20 次/分钟/客户端，实际值以部署配置为准。")
                .append("限流或 Redis 依赖不可用时失败关闭。\n\n");
        output.append("`Idempotency-Key` 长度 8–128，仅允许字母、数字、`.`、`_`、`:`、`-`。")
                .append("请按一次业务操作生成并在重试时复用，不要跨业务操作复用。\n\n");

        section(output, "OAuth 调用示例");
        output.append("以下示例使用占位符，不会读取或展示已保存的客户端密钥。\n\n");
        shellBlock(output, oauthExample(context));
        if (!"USER".equals(context.version().getRequiredActorType())) {
            section(output, "HMAC 调用示例");
            shellBlock(output, hmacExample(context));
        }

        section(output, "错误码");
        List<List<String>> errorRows = ERROR_CODES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> List.of(code(entry.getKey()), entry.getValue()))
                .toList();
        table(output, List.of("错误码", "处理建议"), errorRows);

        section(output, "常见问题排查");
        bulletList(output, List.of(
                "401：确认 Token 未过期且 resource 为 " + openapiResource + "；HMAC 检查时间戳、Nonce、URI 与 Body 原文。",
                "403：确认客户端已授权该能力，主体模式匹配，并同时具备平台权限和业务权限。",
                "400 SCHEMA_INVALID：严格按当前版本参数表传值，不要增加 tenantId、userId 等未声明字段。",
                "409 CONFLICT：能力来源或业务配置已变化，请管理员发布新版本或重新授权。",
                "排障时提供响应中的 requestId，不要发送 Secret、签名密钥或完整 Bearer Token。"));
        return output.toString();
    }

    private List<List<String>> headerRows(DocumentContext context) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Authorization", "OAuth 时必填", "`Bearer <ACCESS_TOKEN>`"));
        rows.add(List.of("Content-Type", "是", "`application/json`"));
        rows.add(List.of("Idempotency-Key", isWrite(context) ? "是" : "否", "写操作幂等键"));
        if (!"USER".equals(context.version().getRequiredActorType())) {
            rows.add(List.of("X-Forge-App-Id", "HMAC 时必填", "客户端 ID / AppId"));
            rows.add(List.of("X-Forge-Timestamp", "HMAC 时必填", "Unix 毫秒时间戳，默认允许 ±5 分钟"));
            rows.add(List.of("X-Forge-Nonce", "HMAC 时必填", "单次随机数，不可重放"));
            rows.add(List.of("X-Forge-Signature", "HMAC 时必填", "规范串的 HMAC-SHA256 十六进制摘要"));
        }
        return rows;
    }

    private String oauthExample(DocumentContext context) {
        boolean userDelegation = "USER".equals(context.version().getRequiredActorType());
        String subjectVariables = userDelegation
                ? "SUBJECT_TOKEN='<TRUSTED_OIDC_JWT>'\n"
                + "# 无统一 OIDC 时，SUBJECT_TOKEN 改为客户端 RS256 用户断言，并使用下一行的专用类型\n"
                + "SUBJECT_TOKEN_TYPE='urn:ietf:params:oauth:token-type:jwt'\n"
                + "# SUBJECT_TOKEN_TYPE='urn:forge:params:oauth:token-type:user-assertion+jwt'\n"
                : "";
        String grant = userDelegation
                ? "  --data-urlencode 'grant_type=urn:ietf:params:oauth:grant-type:token-exchange' \\\n"
                + "  --data-urlencode \"subject_token=$SUBJECT_TOKEN\" \\\n"
                + "  --data-urlencode \"subject_token_type=$SUBJECT_TOKEN_TYPE\" \\\n"
                + "  --data-urlencode 'requested_token_type=urn:ietf:params:oauth:token-type:access_token' \\\n"
                : "  --data-urlencode 'grant_type=client_credentials' \\\n";
        return "BASE_URL='" + baseUrl() + "'\n"
                + "CLIENT_ID='<CLIENT_ID>'\n"
                + "CLIENT_SECRET='<CLIENT_SECRET>'\n"
                + subjectVariables + "\n"
                + "ACCESS_TOKEN=$(curl -sS -X POST \"$BASE_URL/oauth2/token\" \\\n"
                + "  -u \"$CLIENT_ID:$CLIENT_SECRET\" \\\n"
                + "  -H 'Content-Type: application/x-www-form-urlencoded' \\\n"
                + grant
                + "  --data-urlencode 'resource=" + openapiResource + "' \\\n"
                + "  --data-urlencode 'scope=capability:invoke:" + context.capability().getCapabilityCode() + "' \\\n"
                + "  | jq -r '.access_token')\n\n"
                + "curl -sS -X POST '" + invokeUrl(context) + "' \\\n"
                + "  -H \"Authorization: Bearer $ACCESS_TOKEN\" \\\n"
                + (isWrite(context) ? "  -H 'Idempotency-Key: <UNIQUE_BUSINESS_REQUEST_KEY>' \\\n" : "")
                + "  -H 'Content-Type: application/json' \\\n"
                + "  --data '" + compactJson(example(context.inputSchema())) + "'";
    }

    private String hmacExample(DocumentContext context) {
        String body = compactJson(example(context.inputSchema()));
        return "APP_ID='<CLIENT_ID>'\n"
                + "SIGNING_KEY='<SIGNING_KEY>'\n"
                + "TIMESTAMP=$(date +%s000)\n"
                + "NONCE=$(uuidgen | tr '[:upper:]' '[:lower:]')\n"
                + "METHOD='POST'\n"
                + "PATH='" + invokePath(context) + "'\n"
                + "BODY='" + body + "'\n"
                + "BODY_SHA256=$(printf '%s' \"$BODY\" | openssl dgst -sha256 -hex | awk '{print $2}')\n"
                + "CANONICAL=$(printf '%s\\n%s\\n%s\\n%s\\n%s\\n%s' \"$APP_ID\" \"$TIMESTAMP\" \"$NONCE\" \"$METHOD\" \"$PATH\" \"$BODY_SHA256\")\n"
                + "SIGNATURE=$(printf '%s' \"$CANONICAL\" | openssl dgst -sha256 -hmac \"$SIGNING_KEY\" -hex | awk '{print $2}')\n\n"
                + "curl -sS -X POST '" + invokeUrl(context) + "' \\\n"
                + "  -H \"X-Forge-App-Id: $APP_ID\" \\\n"
                + "  -H \"X-Forge-Timestamp: $TIMESTAMP\" \\\n"
                + "  -H \"X-Forge-Nonce: $NONCE\" \\\n"
                + "  -H \"X-Forge-Signature: $SIGNATURE\" \\\n"
                + (isWrite(context) ? "  -H 'Idempotency-Key: <UNIQUE_BUSINESS_REQUEST_KEY>' \\\n" : "")
                + "  -H 'Content-Type: application/json' \\\n"
                + "  --data \"$BODY\"";
    }

    private void appendSchemaTable(StringBuilder output, JsonNode schema, String root) {
        List<List<String>> rows = new ArrayList<>();
        collectSchemaRows(schema, root, true, rows);
        table(output, List.of("参数路径", "必填", "类型", "约束", "说明"), rows);
    }

    private void collectSchemaRows(
            JsonNode schema,
            String path,
            boolean required,
            List<List<String>> rows) {
        rows.add(List.of(code(path), required ? "是" : "否", schemaType(schema),
                constraints(schema), escapeTable(schema.path("description").asText("-"))));
        Set<String> requiredFields = textSet(schema.path("required"));
        JsonNode properties = schema.path("properties");
        if (properties.isObject()) {
            properties.fields().forEachRemaining(entry -> collectSchemaRows(
                    entry.getValue(), path + "." + entry.getKey(),
                    requiredFields.contains(entry.getKey()), rows));
        }
        if (schema.path("items").isObject()) {
            collectSchemaRows(schema.path("items"), path + "[]", required, rows);
        }
    }

    private JsonNode example(JsonNode schema) {
        if (schema.has("example")) {
            return schema.path("example").deepCopy();
        }
        if (schema.has("default")) {
            return schema.path("default").deepCopy();
        }
        if (schema.path("enum").isArray() && !schema.path("enum").isEmpty()) {
            return schema.path("enum").get(0).deepCopy();
        }
        String type = schemaType(schema);
        if (type.startsWith("object")) {
            ObjectNode result = objectMapper.createObjectNode();
            schema.path("properties").fields().forEachRemaining(
                    entry -> result.set(entry.getKey(), example(entry.getValue())));
            return result;
        }
        if (type.startsWith("array")) {
            return objectMapper.createArrayNode().add(example(schema.path("items")));
        }
        if (type.startsWith("integer")) {
            return objectMapper.getNodeFactory().numberNode(1);
        }
        if (type.startsWith("number")) {
            return objectMapper.getNodeFactory().numberNode(1.0);
        }
        if (type.startsWith("boolean")) {
            return objectMapper.getNodeFactory().booleanNode(true);
        }
        return objectMapper.getNodeFactory().textNode(exampleText(schema));
    }

    private String exampleText(JsonNode schema) {
        String format = schema.path("format").asText();
        if ("date".equals(format)) {
            return "2026-09-01";
        }
        if ("date-time".equals(format)) {
            return "2026-09-01T09:00:00+08:00";
        }
        String description = schema.path("description").asText().toLowerCase(Locale.ROOT);
        if (description.contains("business") || description.contains("业务")) {
            return "biz-20260801-001";
        }
        if (description.contains("title") || description.contains("标题")) {
            return "示例流程标题";
        }
        if (description.contains("reason") || description.contains("原因")
                || description.contains("说明")) {
            return "个人原因";
        }
        return "string";
    }

    private String schemaType(JsonNode schema) {
        String type = schema.path("type").asText("object");
        String format = schema.path("format").asText();
        return format.isBlank() ? type : type + "(" + format + ")";
    }

    private String constraints(JsonNode schema) {
        List<String> values = new ArrayList<>();
        addConstraint(values, "minLength", schema);
        addConstraint(values, "maxLength", schema);
        addConstraint(values, "minimum", schema);
        addConstraint(values, "maximum", schema);
        addConstraint(values, "multipleOf", schema);
        addConstraint(values, "pattern", schema);
        if (schema.path("enum").isArray()) {
            List<String> options = new ArrayList<>();
            schema.path("enum").forEach(item -> options.add(item.asText()));
            values.add("enum=" + String.join(",", options));
        }
        if (schema.has("additionalProperties")) {
            values.add("额外字段=" + (schema.path("additionalProperties").asBoolean() ? "允许" : "禁止"));
        }
        return values.isEmpty() ? "-" : escapeTable(String.join("；", values));
    }

    private void addConstraint(List<String> values, String name, JsonNode schema) {
        if (schema.has(name)) {
            values.add(name + "=" + schema.path(name).asText());
        }
    }

    private JsonNode sanitizeRequestSchema(JsonNode schema) {
        JsonNode copy = schema.deepCopy();
        if (!(copy instanceof ObjectNode root)) {
            return copy;
        }
        if (root.path("properties") instanceof ObjectNode properties) {
            properties.remove("idempotencyKey");
        }
        if (root.path("required") instanceof ArrayNode required) {
            ArrayNode filtered = objectMapper.createArrayNode();
            required.forEach(item -> {
                if (!"idempotencyKey".equals(item.asText())) {
                    filtered.add(item);
                }
            });
            root.set("required", filtered);
        }
        return root;
    }

    private List<String> documentationItems(JsonNode policy, String field) {
        JsonNode values = policy.path("documentation").path(field);
        if (!values.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        values.forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText().trim());
            }
        });
        return List.copyOf(result);
    }

    private void appendNotes(StringBuilder output, String title, List<String> notes) {
        if (notes.isEmpty()) {
            return;
        }
        output.append("**").append(title).append("**\n\n");
        bulletList(output, notes);
    }

    private void section(StringBuilder output, String title) {
        output.append("## ").append(title).append("\n\n");
    }

    private void bulletList(StringBuilder output, List<String> values) {
        values.forEach(value -> output.append("- ").append(value).append("\n"));
        output.append("\n");
    }

    private void table(
            StringBuilder output,
            List<String> headers,
            List<List<String>> rows) {
        output.append("| ").append(String.join(" | ", headers)).append(" |\n");
        output.append("|");
        headers.forEach(ignored -> output.append("---|"));
        output.append("\n");
        rows.forEach(row -> output.append("| ")
                .append(String.join(" | ", row.stream().map(this::escapeTable).toList()))
                .append(" |\n"));
        output.append("\n");
    }

    private void jsonBlock(StringBuilder output, JsonNode value) {
        output.append("```json\n");
        try {
            output.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException("能力示例生成失败");
        }
        output.append("\n```\n\n");
    }

    private void shellBlock(StringBuilder output, String value) {
        output.append("```bash\n").append(value).append("\n```\n\n");
    }

    private String compactJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value).replace("'", "'\\''");
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException("能力示例生成失败");
        }
    }

    private String code(String value) {
        return "`" + StringUtils.defaultString(value, "-").replace("`", "\\`") + "`";
    }

    private String escapeTable(String value) {
        return StringUtils.defaultString(value, "-")
                .replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    private Set<String> textSet(JsonNode values) {
        Set<String> result = new LinkedHashSet<>();
        if (values.isArray()) {
            values.forEach(item -> result.add(item.asText()));
        }
        return result;
    }

    private JsonNode readObject(String rawSchema, String label) {
        try {
            JsonNode schema = objectMapper.readTree(rawSchema);
            if (schema == null || !schema.isObject()) {
                throw new BusinessException("能力" + label + " Schema 无效");
            }
            return schema;
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException("能力" + label + " Schema 无法解析");
        }
    }

    private JsonNode readOptionalObject(String content) {
        if (StringUtils.isBlank(content)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode value = objectMapper.readTree(content);
            return value != null && value.isObject() ? value : objectMapper.createObjectNode();
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException("能力发布策略无法解析");
        }
    }

    private String platformPermission(DocumentContext context) {
        return switch (context.version().getSourceType()) {
            case "FLOW_ACTION" -> "ai:capability:flow-action:invoke";
            case "SYSTEM_SERVICE" -> StringUtils.defaultIfBlank(
                    context.policy().path("platformPermission").asText(),
                    "ai:capability:flow-action:invoke");
            default -> "ai:capability:business-action:invoke";
        };
    }

    private String actorDescription(String actorType) {
        return switch (StringUtils.defaultString(actorType)) {
            case "USER" -> "USER（必须使用用户委托 Token）";
            case "SERVICE" -> "SERVICE（服务身份 OAuth 或 HMAC）";
            case "BOTH" -> "BOTH（用户委托或服务身份）";
            default -> code(actorType);
        };
    }

    private boolean isWrite(DocumentContext context) {
        return !"READ_ONLY".equals(context.version().getBehavior());
    }

    private String fileBase(DocumentContext context) {
        return context.capability().getCapabilityCode() + "-" + context.version().getVersion();
    }

    private String invokePath(DocumentContext context) {
        return "/openapi/v1/capabilities/" + context.capability().getCapabilityCode() + "/invoke";
    }

    private String invokeUrl(DocumentContext context) {
        return baseUrl() + invokePath(context);
    }

    private String baseUrl() {
        try {
            URI uri = URI.create(openapiResource);
            return uri.getScheme() + "://" + uri.getRawAuthority();
        }
        catch (IllegalArgumentException exception) {
            throw new BusinessException("开放平台 OpenAPI resource 配置无效");
        }
    }

    private record DocumentContext(
            AiCapability capability,
            AiCapabilityVersion version,
            JsonNode inputSchema,
            JsonNode outputSchema,
            JsonNode policy) {
    }

    public record CapabilityOpenApiDocument(String filename, byte[] content) {
    }

    public record CapabilityMarkdownDocument(String filename, byte[] content) {
    }
}
