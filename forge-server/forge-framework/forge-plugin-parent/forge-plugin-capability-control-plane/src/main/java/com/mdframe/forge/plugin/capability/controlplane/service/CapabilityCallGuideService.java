package com.mdframe.forge.plugin.capability.controlplane.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityGrant;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityVersion;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityGrantMapper;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityVersionMapper;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityClientActorMode;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityCallGuideCheckVO;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityCallGuideVO;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityClientVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class CapabilityCallGuideService {

    private static final String TOKEN_EXCHANGE =
            "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    private static final String USER_ASSERTION_TOKEN_TYPE =
            "urn:forge:params:oauth:token-type:user-assertion+jwt";
    private static final long USER_ASSERTION_TTL_SECONDS = 120L;

    private final CapabilityCatalogService catalogService;
    private final CapabilityClientService clientService;
    private final AiCapabilityGrantMapper grantMapper;
    private final AiCapabilityVersionMapper versionMapper;
    private final CapabilityOpenApiDocumentService documentService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean gatewayEnabled;
    private final boolean identityEnabled;
    private final boolean flowActionsEnabled;
    private final String identityIssuer;
    private final long userAssertionMaxTtlSeconds;
    private final String openapiResource;

    public CapabilityCallGuideService(
            CapabilityCatalogService catalogService,
            CapabilityClientService clientService,
            AiCapabilityGrantMapper grantMapper,
            AiCapabilityVersionMapper versionMapper,
            CapabilityOpenApiDocumentService documentService,
            ObjectMapper objectMapper,
            Clock capabilityClock,
            @Value("${forge.capability.open-gateway.enabled:false}") boolean gatewayEnabled,
            @Value("${forge.capability.identity.enabled:true}") boolean identityEnabled,
            @Value("${forge.capability.flow-actions.enabled:false}") boolean flowActionsEnabled,
            @Value("${forge.capability.identity.issuer:http://localhost:8580}")
            String identityIssuer,
            @Value("${forge.capability.identity.user-assertion-max-ttl:PT2M}")
            Duration userAssertionMaxTtl,
            @Value("${forge.capability.identity.openapi-resource:http://localhost:8580/openapi}")
            String openapiResource) {
        this.catalogService = catalogService;
        this.clientService = clientService;
        this.grantMapper = grantMapper;
        this.versionMapper = versionMapper;
        this.documentService = documentService;
        this.objectMapper = objectMapper;
        this.clock = capabilityClock;
        this.gatewayEnabled = gatewayEnabled;
        this.identityEnabled = identityEnabled || gatewayEnabled;
        this.flowActionsEnabled = flowActionsEnabled;
        this.identityIssuer = StringUtils.removeEnd(StringUtils.trim(identityIssuer), "/");
        long configuredTtlSeconds = userAssertionMaxTtl == null
                ? USER_ASSERTION_TTL_SECONDS : userAssertionMaxTtl.toSeconds();
        this.userAssertionMaxTtlSeconds = Math.min(
                USER_ASSERTION_TTL_SECONDS, Math.max(30L, configuredTtlSeconds));
        this.openapiResource = openapiResource;
    }

    public CapabilityCallGuideVO guide(Long tenantId, Long capabilityId, Long clientId) {
        AiCapability capability = catalogService.getById(tenantId, capabilityId);
        AiCapabilityClient client = clientService.requireClient(tenantId, clientId);
        AiCapabilityGrant grant = grantMapper.selectActiveGrant(
                tenantId, client.getId(), capability.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        List<CapabilityCallGuideCheckVO> checks = new ArrayList<>();

        add(checks, "GATEWAY", "开放网关", gatewayEnabled, true,
                gatewayEnabled ? "已启用" : "未启用，请设置 forge.capability.open-gateway.enabled=true");
        boolean capabilityAvailable = "PUBLISHED".equals(capability.getPublishStatus())
                && Integer.valueOf(1).equals(capability.getEnabled())
                && !"HIGH".equals(capability.getRiskLevel());
        add(checks, "CAPABILITY", "能力状态", capabilityAvailable, true,
                capabilityAvailable ? "能力已发布且可授权" : "能力未发布、已停用或属于 HIGH 风险");
        boolean clientAvailable = "ENABLED".equals(client.getStatus())
                && !expired(client.getExpiresAt(), now);
        add(checks, "CLIENT", "客户端状态", clientAvailable, true,
                clientAvailable ? "客户端已启用" : "客户端已停用、吊销或过期");

        CapabilityClientActorMode actorMode = actorMode(client.getActorMode());
        boolean actorCompatible = actorCompatible(capability.getRequiredActorType(), actorMode);
        add(checks, "ACTOR", "调用主体", actorCompatible, true,
                actorCompatible
                        ? "客户端主体模式满足 " + capability.getRequiredActorType() + " 要求"
                        : "客户端主体模式与能力要求不匹配");

        Set<String> authModes = authModes(client.getAuthModes());
        List<String> availableAuthModes = availableAuthModes(
                capability.getRequiredActorType(), actorMode, authModes, client);
        boolean authReady = availableAuthModes.contains("HMAC")
                || (availableAuthModes.contains("OAUTH") && identityEnabled);
        add(checks, "AUTH", "认证方式", authReady, true,
                authReady
                        ? "可使用 " + String.join(" / ", availableAuthModes)
                        : "客户端未配置与能力主体匹配的 OAuth 或 HMAC 认证方式");

        boolean grantAvailable = grant != null && "ENABLED".equals(grant.getStatus())
                && !expired(grant.getExpiresAt(), now);
        add(checks, "GRANT", "能力授权", grantAvailable, true,
                grantAvailable ? "客户端已获得有效授权" : "客户端尚未授权该能力或授权已失效");

        String resolvedVersion = resolveVersion(capability, grant);
        AiCapabilityVersion version = resolvedVersion == null ? null
                : versionMapper.selectVersion(tenantId, capability.getId(), resolvedVersion);
        boolean versionAvailable = version != null && "PUBLISHED".equals(version.getStatus());
        String currentVersion = capability.getCurrentVersion();
        AiCapabilityVersion currentPublishedVersion = Objects.equals(resolvedVersion, currentVersion)
                ? version
                : versionMapper.selectVersion(tenantId, capability.getId(), currentVersion);
        boolean currentVersionAvailable = currentPublishedVersion != null
                && "PUBLISHED".equals(currentPublishedVersion.getStatus());
        boolean versionUpgradeAvailable = grantAvailable
                && grant.getId() != null
                && currentVersionAvailable
                && !Objects.equals(resolvedVersion, currentVersion);
        String versionMessage;
        if (!versionAvailable) {
            versionMessage = "授权版本不存在、版本策略不匹配或版本未发布";
        }
        else if (versionUpgradeAvailable) {
            versionMessage = "客户端实际调用 v" + resolvedVersion
                    + "，能力当前版本为 v" + currentVersion;
        }
        else {
            versionMessage = "客户端实际调用版本为 v" + resolvedVersion;
        }
        add(checks, "VERSION", "授权版本", versionAvailable, true,
                versionMessage);

        addFlowBindingCheck(
                checks, capability, version, currentPublishedVersion,
                resolvedVersion, currentVersion);
        String actionCode = actionCode(version);
        addSubmissionGrantCheck(checks, version, actionCode, grant);
        List<String> requestNotes = requestNotes(version, actionCode);

        ExecutionAvailability execution = executionAvailability(version);
        add(checks, "EXECUTOR", "执行能力", execution.available(), true,
                execution.message());

        List<String> runtimePermissions = runtimePermissions(version);
        boolean runtimeCheck = "USER".equals(capability.getRequiredActorType());
        checks.add(new CapabilityCallGuideCheckVO(
                "RUNTIME_PERMISSION", "运行时权限", runtimeCheck ? "RUNTIME" : "INFO", false,
                runtimeCheck
                        ? "实际委托用户在调用时必须具备: " + String.join("、", runtimePermissions)
                        : "服务账号在调用时必须具备: " + String.join("、", runtimePermissions)));

        boolean ready = checks.stream().noneMatch(
                check -> check.blocking() && "FAILED".equals(check.status()));
        String baseUrl = baseUrl();
        String invokeUrl = baseUrl + "/openapi/v1/capabilities/"
                + capability.getCapabilityCode() + "/invoke";
        JsonNode requestExample = versionAvailable
                ? documentService.requestExample(tenantId, capabilityId, resolvedVersion)
                : objectMapper.createObjectNode();
        requestExample = prepareRequestExample(
                version, actionCode, grantAvailable ? grant.getFieldPolicy() : null, requestExample);
        boolean tokenExchangeRequired = requiresTokenExchange(capability, client);
        boolean userAssertionEnabled = tokenExchangeRequired
                && Integer.valueOf(1).equals(client.getUserAssertionEnabled())
                && StringUtils.isNotBlank(client.getUserAssertionKeyId())
                && client.getUserAssertionKeyVersion() != null
                && client.getUserAssertionKeyVersion() > 0;
        String oauthExample = availableAuthModes.contains("OAUTH")
                ? oauthExample(
                        capability, client, invokeUrl, requestExample, userAssertionEnabled)
                : null;
        String hmacExample = availableAuthModes.contains("HMAC")
                ? hmacExample(capability, client, invokeUrl, requestExample) : null;
        String oauthJavaExample = availableAuthModes.contains("OAUTH")
                ? oauthJavaExample(
                        capability, client, invokeUrl, requestExample, tokenExchangeRequired)
                : null;
        String hmacJavaExample = availableAuthModes.contains("HMAC")
                ? hmacJavaExample(capability, client, invokeUrl, requestExample) : null;
        String userAssertionJavaExample = userAssertionEnabled
                ? userAssertionJavaExample(capability, client, invokeUrl, requestExample)
                : null;
        return new CapabilityCallGuideVO(
                capability.getId(), capability.getCapabilityCode(), capability.getCapabilityName(),
                resolvedVersion, currentVersion,
                grantAvailable ? grant.getId() : null,
                grantAvailable ? grant.getVersionStrategy() : null,
                grantAvailable ? grant.getFixedVersion() : null,
                versionUpgradeAvailable,
                client.getId(), client.getClientCode(), client.getClientName(),
                ready, capability.getBehavior(),
                version == null ? capability.getSourceType() : version.getSourceType(),
                actionCode, capability.getRequiredActorType(),
                tokenExchangeRequired, availableAuthModes, openapiResource,
                invokeUrl, baseUrl + "/oauth2/token", requestExample,
                requestNotes, runtimePermissions,
                List.copyOf(checks), oauthExample, hmacExample,
                oauthJavaExample, hmacJavaExample, userAssertionEnabled,
                client.getUserAssertionKeyId(), client.getClientCode(), identityIssuer,
                USER_ASSERTION_TOKEN_TYPE, userAssertionMaxTtlSeconds,
                userAssertionJavaExample);
    }

    private String actionCode(AiCapabilityVersion version) {
        if (version == null) {
            return null;
        }
        String operation = StringUtils.trimToNull(
                readPolicy(version.getPolicySnapshot()).path("operation").asText());
        if (operation != null) {
            return operation;
        }
        String[] source = StringUtils.defaultString(version.getSourceKey()).split("/", -1);
        return source.length == 3 ? StringUtils.trimToNull(source[2]) : null;
    }

    private List<String> requestNotes(AiCapabilityVersion version, String actionCode) {
        if (version == null) {
            return List.of();
        }
        LinkedHashSet<String> notes = new LinkedHashSet<>();
        JsonNode documented = readPolicy(version.getPolicySnapshot())
                .path("documentation").path("requestNotes");
        if (documented.isArray()) {
            documented.forEach(item -> {
                String note = item.isTextual() ? StringUtils.trimToNull(item.asText()) : null;
                if (note != null) {
                    notes.add(note);
                }
            });
        }
        if ("FLOW_ACTION".equals(version.getSourceType())) {
            if ("SUBMIT".equals(actionCode)) {
                notes.add("SUBMIT 直接创建业务申请并启动主流程，请按 data 字段说明提交申请内容，不需要也不能传 recordId。");
                notes.add("申请人、归属人、租户、组织、单据初始状态和流程发起人均从本次 Token 的可信委托用户生成。");
                notes.add("流程启动失败时必须复用原 Idempotency-Key 重试，平台会复用已经创建的申请记录。");
                return List.copyOf(notes);
            }
            String[] source = StringUtils.defaultString(version.getSourceKey()).split("/", -1);
            String objectCode = source.length == 3 ? source[1] : "当前业务对象";
            notes.add("recordId 必须填写业务对象「" + objectCode
                    + "」中已经保存的真实记录主键，不能填写任意测试数字。");
            notes.add("记录必须在本次 Token 对应的实际委托用户数据权限范围内；平台不会泄露记录究竟不存在还是不可见。");
            if ("START".equals(actionCode)) {
                notes.add("START 只为已有业务记录发起流程，不会创建业务记录；请先在业务页面保存记录，再复制记录 ID 调用。");
            }
            else {
                notes.add("APPROVE/REJECT 还必须填写属于该记录、该流程且由当前委托用户可办理的真实 taskId。");
            }
        }
        return List.copyOf(notes);
    }

    private JsonNode prepareRequestExample(
            AiCapabilityVersion version,
            String actionCode,
            String grantFieldPolicy,
            JsonNode requestExample) {
        if (version == null || !(requestExample instanceof ObjectNode root)) {
            return requestExample;
        }
        Set<String> grantedFields = readTextSet(
                readPolicy(grantFieldPolicy).path("allowedFields"));
        if ("BUSINESS_ACTION".equals(version.getSourceType())) {
            retainExampleFields(root.path("arguments"), grantedFields);
            return root;
        }
        if (!"FLOW_ACTION".equals(version.getSourceType())) {
            return root;
        }
        if ("SUBMIT".equals(actionCode)) {
            retainExampleFields(root.path("data"), grantedFields);
            return root;
        }
        root.put("recordId", "<REAL_RECORD_ID>");
        if (!"START".equals(actionCode) && root.path("arguments") instanceof ObjectNode arguments) {
            arguments.put("taskId", "<REAL_TASK_ID>");
        }
        return root;
    }

    private void retainExampleFields(JsonNode node, Set<String> allowedFields) {
        if (!(node instanceof ObjectNode object) || allowedFields.isEmpty()) {
            return;
        }
        List<String> fields = new ArrayList<>();
        object.fieldNames().forEachRemaining(fields::add);
        fields.stream()
                .filter(field -> !allowedFields.contains(field))
                .forEach(object::remove);
    }

    private void addFlowBindingCheck(
            List<CapabilityCallGuideCheckVO> checks,
            AiCapability capability,
            AiCapabilityVersion resolvedVersion,
            AiCapabilityVersion currentVersion,
            String resolvedVersionNumber,
            String currentVersionNumber) {
        if (!"FLOW_ACTION".equals(capability.getSourceType())) {
            return;
        }
        if (resolvedVersion == null || !"PUBLISHED".equals(resolvedVersion.getStatus())) {
            add(checks, "FLOW_BINDING", "流程绑定", false, true,
                    "授权版本可用后才能核对流程绑定快照");
            return;
        }
        if (Objects.equals(resolvedVersionNumber, currentVersionNumber)) {
            add(checks, "FLOW_BINDING", "流程绑定", true, true,
                    "实际调用版本与能力当前版本的流程绑定一致");
            return;
        }
        if (currentVersion == null || !"PUBLISHED".equals(currentVersion.getStatus())) {
            add(checks, "FLOW_BINDING", "流程绑定", false, true,
                    "能力当前版本不存在或未发布，无法核对流程绑定快照");
            return;
        }
        JsonNode resolvedPolicy = readPolicy(resolvedVersion.getPolicySnapshot());
        JsonNode currentPolicy = readPolicy(currentVersion.getPolicySnapshot());
        if (sameFlowBindingSnapshot(resolvedPolicy, currentPolicy)) {
            add(checks, "FLOW_BINDING", "流程绑定", true, true,
                    "客户端使用旧版本 v" + resolvedVersionNumber
                            + "，但流程绑定快照仍与当前版本一致");
            return;
        }
        add(checks, "FLOW_BINDING", "流程绑定", false, true,
                "客户端授权仍使用 v" + resolvedVersionNumber
                        + "，能力当前 v" + currentVersionNumber
                        + " 已更新流程绑定；继续测试会返回 FLOW_BINDING_MISMATCH，请先切换到当前版本");
    }

    private boolean sameFlowBindingSnapshot(JsonNode left, JsonNode right) {
        return sameRequiredPolicyValue(left, right, "bindingId")
                && sameRequiredPolicyValue(left, right, "flowModelKey")
                && sameRequiredPolicyValue(left, right, "publishedObjectVersion");
    }

    private boolean sameRequiredPolicyValue(JsonNode left, JsonNode right, String field) {
        String leftValue = StringUtils.trimToNull(left.path(field).asText());
        String rightValue = StringUtils.trimToNull(right.path(field).asText());
        return leftValue != null && leftValue.equals(rightValue);
    }

    private void addSubmissionGrantCheck(
            List<CapabilityCallGuideCheckVO> checks,
            AiCapabilityVersion version,
            String actionCode,
            AiCapabilityGrant grant) {
        if (version == null || !"FLOW_ACTION".equals(version.getSourceType())
                || !"SUBMIT".equals(actionCode)) {
            return;
        }
        Set<String> versionFields = readTextSet(readPolicy(version.getPolicySnapshot())
                .path("allowedFields"));
        Set<String> requiredFields = readTextSet(readPolicy(version.getPolicySnapshot())
                .path("requiredFields"));
        Set<String> grantFields = grant == null
                ? Set.of() : readTextSet(readPolicy(grant.getFieldPolicy()).path("allowedFields"));
        boolean valid = !versionFields.isEmpty()
                && !grantFields.isEmpty()
                && versionFields.containsAll(grantFields)
                && grantFields.containsAll(requiredFields);
        add(checks, "FIELD_POLICY", "申请字段授权", valid, true,
                valid ? "客户端申请字段白名单已按能力版本收窄"
                        : "客户端申请字段授权缺失或超出能力版本，请重新配置授权");
    }

    public List<CapabilityClientVO> clients(Long tenantId) {
        return clientService.listGrantOptions(tenantId);
    }

    private void add(
            List<CapabilityCallGuideCheckVO> checks,
            String code,
            String label,
            boolean passed,
            boolean blocking,
            String message) {
        checks.add(new CapabilityCallGuideCheckVO(
                code, label, passed ? "PASSED" : "FAILED", blocking, message));
    }

    private List<String> availableAuthModes(
            String requiredActorType,
            CapabilityClientActorMode actorMode,
            Set<String> configured,
            AiCapabilityClient client) {
        List<String> result = new ArrayList<>();
        if (configured.contains("OAUTH") && Integer.valueOf(1).equals(client.getOauthEnabled())
                && (("USER".equals(requiredActorType) && actorMode != null && actorMode.allowsUserDelegation())
                || ("SERVICE".equals(requiredActorType) && actorMode != null && actorMode.requiresServiceIdentity())
                || "BOTH".equals(requiredActorType))) {
            result.add("OAUTH");
        }
        if (!"USER".equals(requiredActorType)
                && configured.contains("SIGNATURE")
                && actorMode != null && actorMode.requiresServiceIdentity()
                && client.getSigningKeyVersion() != null && client.getSigningKeyVersion() > 0) {
            result.add("HMAC");
        }
        return List.copyOf(result);
    }

    private boolean actorCompatible(String requiredActorType, CapabilityClientActorMode actorMode) {
        if (actorMode == null) {
            return false;
        }
        return switch (StringUtils.defaultString(requiredActorType)) {
            case "USER" -> actorMode.allowsUserDelegation();
            case "SERVICE" -> actorMode.requiresServiceIdentity();
            case "BOTH" -> true;
            default -> false;
        };
    }

    private String resolveVersion(AiCapability capability, AiCapabilityGrant grant) {
        if (grant == null || StringUtils.isBlank(grant.getFixedVersion())) {
            return null;
        }
        if ("PINNED".equals(grant.getVersionStrategy())) {
            return grant.getFixedVersion();
        }
        if ("FOLLOW_MAJOR".equals(grant.getVersionStrategy())
                && sameMajor(capability.getCurrentVersion(), grant.getFixedVersion())) {
            return capability.getCurrentVersion();
        }
        return null;
    }

    private List<String> runtimePermissions(AiCapabilityVersion version) {
        if (version == null) {
            return List.of("授权版本可用后展示");
        }
        JsonNode policy = readPolicy(version.getPolicySnapshot());
        String fallbackPlatform = switch (version.getSourceType()) {
            case "FLOW_ACTION", "SYSTEM_SERVICE" -> "ai:capability:flow-action:invoke";
            default -> "ai:capability:business-action:invoke";
        };
        String platform = StringUtils.defaultIfBlank(
                policy.path("platformPermission").asText(), fallbackPlatform);
        String business = StringUtils.trimToNull(policy.path("permission").asText());
        return business == null ? List.of(platform) : List.of(platform, business);
    }

    private ExecutionAvailability executionAvailability(AiCapabilityVersion version) {
        if (version == null) {
            return new ExecutionAvailability(false, "授权版本可用后才能检查执行适配器");
        }
        String sourceType = StringUtils.defaultString(version.getSourceType());
        String behavior = StringUtils.defaultString(version.getBehavior());
        if ("BUSINESS_ACTION".equals(sourceType) && "ACTION".equals(behavior)) {
            return new ExecutionAvailability(true, "低代码业务动作执行适配器已启用");
        }
        if ("SYSTEM_SERVICE".equals(sourceType) && "ACTION".equals(behavior)) {
            return new ExecutionAvailability(true, "受控系统服务执行适配器已启用");
        }
        if ("FLOW_ACTION".equals(sourceType) && "FLOW".equals(behavior)) {
            return flowActionsEnabled
                    ? new ExecutionAvailability(true, "流程动作执行适配器已启用")
                    : new ExecutionAvailability(false,
                    "流程动作执行器未启用，请设置 forge.capability.flow-actions.enabled=true");
        }
        return new ExecutionAvailability(false,
                "来源 " + sourceType + "/" + behavior
                        + " 尚无开放网关执行适配器，请使用受控业务动作、流程动作或系统服务重新注册");
    }

    private String oauthExample(
            AiCapability capability,
            AiCapabilityClient client,
            String invokeUrl,
            JsonNode requestExample,
            boolean userAssertionEnabled) {
        String subjectToken = userAssertionEnabled
                ? "<CLIENT_SIGNED_USER_ASSERTION_JWT>" : "<TRUSTED_OIDC_JWT>";
        String subjectTokenType = userAssertionEnabled
                ? USER_ASSERTION_TOKEN_TYPE : JWT_TOKEN_TYPE;
        String grant = requiresTokenExchange(capability, client)
                ? "  --data-urlencode 'grant_type=" + TOKEN_EXCHANGE + "' \\\n"
                + "  --data-urlencode 'subject_token=" + subjectToken + "' \\\n"
                + "  --data-urlencode 'subject_token_type=" + subjectTokenType + "' \\\n"
                + "  --data-urlencode 'requested_token_type=urn:ietf:params:oauth:token-type:access_token' \\\n"
                : "  --data-urlencode 'grant_type=client_credentials' \\\n";
        return "CLIENT_ID='" + client.getId() + "'\n"
                + "CLIENT_SECRET='<CLIENT_SECRET>'\n\n"
                + "ACCESS_TOKEN=$(curl -sS -X POST '" + baseUrl() + "/oauth2/token' \\\n"
                + "  -u \"$CLIENT_ID:$CLIENT_SECRET\" \\\n"
                + "  -H 'Content-Type: application/x-www-form-urlencoded' \\\n"
                + grant
                + "  --data-urlencode 'resource=" + openapiResource + "' \\\n"
                + "  --data-urlencode 'scope=capability:invoke:"
                + capability.getCapabilityCode() + "' | jq -r '.access_token')\n\n"
                + "curl -sS -X POST '" + invokeUrl + "' \\\n"
                + "  -H \"Authorization: Bearer $ACCESS_TOKEN\" \\\n"
                + idempotencyHeader(capability)
                + "  -H 'Content-Type: application/json' \\\n"
                + "  --data '" + compactJson(requestExample) + "'";
    }

    private String hmacExample(
            AiCapability capability,
            AiCapabilityClient client,
            String invokeUrl,
            JsonNode requestExample) {
        String path = URI.create(invokeUrl).getPath();
        return "APP_ID='" + client.getId() + "'\n"
                + "SIGNING_KEY='<SIGNING_KEY>'\n"
                + "TIMESTAMP=$(date +%s000)\nNONCE=$(uuidgen | tr '[:upper:]' '[:lower:]')\n"
                + "METHOD='POST'\nPATH='" + path + "'\nBODY='" + compactJson(requestExample) + "'\n"
                + "BODY_SHA256=$(printf '%s' \"$BODY\" | openssl dgst -sha256 -hex | awk '{print $2}')\n"
                + "CANONICAL=$(printf '%s\\n%s\\n%s\\n%s\\n%s\\n%s' \"$APP_ID\" \"$TIMESTAMP\" \"$NONCE\" \"$METHOD\" \"$PATH\" \"$BODY_SHA256\")\n"
                + "SIGNATURE=$(printf '%s' \"$CANONICAL\" | openssl dgst -sha256 -hmac \"$SIGNING_KEY\" -hex | awk '{print $2}')\n\n"
                + "curl -sS -X POST '" + invokeUrl + "' \\\n"
                + "  -H \"X-Forge-App-Id: $APP_ID\" \\\n"
                + "  -H \"X-Forge-Timestamp: $TIMESTAMP\" \\\n"
                + "  -H \"X-Forge-Nonce: $NONCE\" \\\n"
                + "  -H \"X-Forge-Signature: $SIGNATURE\" \\\n"
                + idempotencyHeader(capability)
                + "  -H 'Content-Type: application/json' \\\n"
                + "  --data \"$BODY\"";
    }

    private String oauthJavaExample(
            AiCapability capability,
            AiCapabilityClient client,
            String invokeUrl,
            JsonNode requestExample,
            boolean tokenExchangeRequired) {
        String grantType = tokenExchangeRequired ? TOKEN_EXCHANGE : "client_credentials";
        String subjectToken = tokenExchangeRequired
                ? "\n                + \"&subject_token=\" + encode(SUBJECT_TOKEN)"
                + "\n                + \"&subject_token_type=\" + encode(\"urn:ietf:params:oauth:token-type:jwt\")"
                + "\n                + \"&requested_token_type=\" + encode(\"urn:ietf:params:oauth:token-type:access_token\")"
                : "";
        String subjectConstant = tokenExchangeRequired
                ? "    private static final String SUBJECT_TOKEN = requireEnvironment(\"FORGE_SUBJECT_TOKEN\");\n"
                : "";
        String idempotency = "READ_ONLY".equals(capability.getBehavior())
                ? ""
                : "\n                .header(\"Idempotency-Key\", UUID.randomUUID().toString())";
        return """
                import java.net.URI;
                import java.net.URLEncoder;
                import java.net.http.HttpClient;
                import java.net.http.HttpRequest;
                import java.net.http.HttpResponse;
                import java.nio.charset.StandardCharsets;
                import java.util.Base64;
                import java.util.UUID;

                public final class CapabilityClient {
                    private static final HttpClient HTTP = HttpClient.newHttpClient();
                    private static final String CLIENT_ID = "%s";
                    private static final String CLIENT_SECRET = requireEnvironment("FORGE_CLIENT_SECRET");
                %s    private static final String TOKEN_URL = "%s";
                    private static final String INVOKE_URL = "%s";
                    private static final String RESOURCE = "%s";
                    private static final String SCOPE = "capability:invoke:%s";
                    private static final String BODY = "%s";

                    public static void main(String[] args) throws Exception {
                        String tokenForm = "grant_type=" + encode("%s")%s
                                + "&resource=" + encode(RESOURCE)
                                + "&scope=" + encode(SCOPE);
                        String basic = Base64.getEncoder().encodeToString(
                                (CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
                        HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                                .header("Authorization", "Basic " + basic)
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString(tokenForm))
                                .build();
                        HttpResponse<String> tokenResponse = HTTP.send(
                                tokenRequest, HttpResponse.BodyHandlers.ofString());
                        requireSuccess("获取访问令牌", tokenResponse);
                        String accessToken = readJsonString(tokenResponse.body(), "access_token");

                        HttpRequest invokeRequest = HttpRequest.newBuilder(URI.create(INVOKE_URL))
                                .header("Authorization", "Bearer " + accessToken)
                                .header("Content-Type", "application/json")%s
                                .POST(HttpRequest.BodyPublishers.ofString(BODY))
                                .build();
                        HttpResponse<String> invokeResponse = HTTP.send(
                                invokeRequest, HttpResponse.BodyHandlers.ofString());
                        requireSuccess("调用能力", invokeResponse);
                        System.out.println(invokeResponse.body());
                    }

                    private static String encode(String value) {
                        return URLEncoder.encode(value, StandardCharsets.UTF_8);
                    }

                    private static String requireEnvironment(String name) {
                        String value = System.getenv(name);
                        if (value == null || value.isBlank()) {
                            throw new IllegalStateException("缺少环境变量: " + name);
                        }
                        return value;
                    }

                    private static String readJsonString(String json, String field) {
                        int name = json.indexOf("\\\"" + field + "\\\"");
                        int colon = name < 0 ? -1 : json.indexOf(':', name);
                        int start = colon < 0 ? -1 : json.indexOf('\\\"', colon);
                        int end = start < 0 ? -1 : json.indexOf('\\\"', start + 1);
                        if (start < 0 || end < 0) {
                            throw new IllegalStateException("Token 响应缺少 " + field + ": " + json);
                        }
                        return json.substring(start + 1, end);
                    }

                    private static void requireSuccess(
                            String stage, HttpResponse<String> response) {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw new IllegalStateException(
                                    stage + "失败，HTTP " + response.statusCode() + ": " + response.body());
                        }
                    }
                }
                """.formatted(
                client.getId(), subjectConstant, baseUrl() + "/oauth2/token", invokeUrl,
                openapiResource, capability.getCapabilityCode(), javaStringLiteral(requestExample),
                grantType, subjectToken, idempotency);
    }

    private String hmacJavaExample(
            AiCapability capability,
            AiCapabilityClient client,
            String invokeUrl,
            JsonNode requestExample) {
        String idempotency = "READ_ONLY".equals(capability.getBehavior())
                ? ""
                : "\n                .header(\"Idempotency-Key\", UUID.randomUUID().toString())";
        return """
                import java.net.URI;
                import java.net.http.HttpClient;
                import java.net.http.HttpRequest;
                import java.net.http.HttpResponse;
                import java.nio.charset.StandardCharsets;
                import java.security.MessageDigest;
                import java.util.HexFormat;
                import java.util.UUID;
                import javax.crypto.Mac;
                import javax.crypto.spec.SecretKeySpec;

                public final class CapabilityClient {
                    private static final HttpClient HTTP = HttpClient.newHttpClient();
                    private static final String APP_ID = "%s";
                    private static final String SIGNING_KEY = requireEnvironment("FORGE_SIGNING_KEY");
                    private static final String INVOKE_URL = "%s";
                    private static final String BODY = "%s";

                    public static void main(String[] args) throws Exception {
                        String timestamp = Long.toString(System.currentTimeMillis());
                        String nonce = UUID.randomUUID().toString();
                        String path = URI.create(INVOKE_URL).getRawPath();
                        String canonical = String.join("\\n", APP_ID, timestamp, nonce,
                                "POST", path, sha256Hex(BODY));
                        String signature = hmacSha256Hex(SIGNING_KEY, canonical);

                        HttpRequest request = HttpRequest.newBuilder(URI.create(INVOKE_URL))
                                .header("X-Forge-App-Id", APP_ID)
                                .header("X-Forge-Timestamp", timestamp)
                                .header("X-Forge-Nonce", nonce)
                                .header("X-Forge-Signature", signature)
                                .header("Content-Type", "application/json")%s
                                .POST(HttpRequest.BodyPublishers.ofString(BODY))
                                .build();
                        HttpResponse<String> response = HTTP.send(
                                request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw new IllegalStateException(
                                    "调用失败，HTTP " + response.statusCode() + ": " + response.body());
                        }
                        System.out.println(response.body());
                    }

                    private static String sha256Hex(String value) throws Exception {
                        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                                .digest(value.getBytes(StandardCharsets.UTF_8)));
                    }

                    private static String hmacSha256Hex(String key, String value) throws Exception {
                        Mac mac = Mac.getInstance("HmacSHA256");
                        mac.init(new SecretKeySpec(
                                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                        return HexFormat.of().formatHex(
                                mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
                    }

                    private static String requireEnvironment(String name) {
                        String value = System.getenv(name);
                        if (value == null || value.isBlank()) {
                            throw new IllegalStateException("缺少环境变量: " + name);
                        }
                        return value;
                    }
                }
                """.formatted(
                client.getId(), invokeUrl, javaStringLiteral(requestExample), idempotency);
    }

    private String userAssertionJavaExample(
            AiCapability capability,
            AiCapabilityClient client,
            String invokeUrl,
            JsonNode requestExample) {
        String idempotency = "READ_ONLY".equals(capability.getBehavior())
                ? ""
                : "\n                .header(\"Idempotency-Key\", UUID.randomUUID().toString())";
        return """
                import java.net.URI;
                import java.net.URLEncoder;
                import java.net.http.HttpClient;
                import java.net.http.HttpRequest;
                import java.net.http.HttpResponse;
                import java.nio.charset.StandardCharsets;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.security.KeyFactory;
                import java.security.PrivateKey;
                import java.security.Signature;
                import java.security.spec.PKCS8EncodedKeySpec;
                import java.time.Instant;
                import java.util.Base64;
                import java.util.UUID;

                public final class CapabilityUserAssertionClient {
                    private static final HttpClient HTTP = HttpClient.newHttpClient();
                    private static final String CLIENT_ID = "%s";
                    private static final String CLIENT_CODE = "%s";
                    private static final String CLIENT_SECRET = requireEnvironment("FORGE_CLIENT_SECRET");
                    private static final String PRIVATE_KEY_FILE =
                            requireEnvironment("FORGE_USER_ASSERTION_PRIVATE_KEY_FILE");
                    private static final String EXTERNAL_SUBJECT =
                            requireEnvironment("FORGE_EXTERNAL_SUBJECT");
                    private static final String KEY_ID = "%s";
                    private static final String AUDIENCE = "%s";
                    private static final String TOKEN_URL = "%s";
                    private static final String INVOKE_URL = "%s";
                    private static final String RESOURCE = "%s";
                    private static final String SCOPE = "capability:invoke:%s";
                    private static final String SUBJECT_TOKEN_TYPE = "%s";
                    private static final String BODY = "%s";
                    private static final long ASSERTION_TTL_SECONDS = %dL;

                    public static void main(String[] args) throws Exception {
                        String subjectToken = createUserAssertion();
                        String tokenForm = "grant_type=" + encode("%s")
                                + "&subject_token=" + encode(subjectToken)
                                + "&subject_token_type=" + encode(SUBJECT_TOKEN_TYPE)
                                + "&requested_token_type=" + encode(
                                        "urn:ietf:params:oauth:token-type:access_token")
                                + "&resource=" + encode(RESOURCE)
                                + "&scope=" + encode(SCOPE);
                        String basic = Base64.getEncoder().encodeToString(
                                (CLIENT_ID + ":" + CLIENT_SECRET)
                                        .getBytes(StandardCharsets.UTF_8));
                        HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                                .header("Authorization", "Basic " + basic)
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString(tokenForm))
                                .build();
                        HttpResponse<String> tokenResponse = HTTP.send(
                                tokenRequest, HttpResponse.BodyHandlers.ofString());
                        requireSuccess("获取访问令牌", tokenResponse);
                        String accessToken = readJsonString(tokenResponse.body(), "access_token");

                        HttpRequest invokeRequest = HttpRequest.newBuilder(URI.create(INVOKE_URL))
                                .header("Authorization", "Bearer " + accessToken)
                                .header("Content-Type", "application/json")%s
                                .POST(HttpRequest.BodyPublishers.ofString(BODY))
                                .build();
                        HttpResponse<String> invokeResponse = HTTP.send(
                                invokeRequest, HttpResponse.BodyHandlers.ofString());
                        requireSuccess("调用能力", invokeResponse);
                        System.out.println(invokeResponse.body());
                    }

                    private static String createUserAssertion() throws Exception {
                        if (!EXTERNAL_SUBJECT.matches("[A-Za-z0-9._:@/-]{1,512}")) {
                            throw new IllegalStateException(
                                    "示例要求 FORGE_EXTERNAL_SUBJECT 使用稳定的 ASCII 标识");
                        }
                        long issuedAt = Instant.now().getEpochSecond();
                        long expiresAt = issuedAt + ASSERTION_TTL_SECONDS;
                        String jwtId = UUID.randomUUID().toString();
                        String header = "{\\\"alg\\\":\\\"RS256\\\",\\\"typ\\\":\\\"JWT\\\","
                                + "\\\"kid\\\":\\\"" + KEY_ID + "\\\"}";
                        String payload = "{\\\"iss\\\":\\\"" + CLIENT_CODE
                                + "\\\",\\\"aud\\\":\\\"" + AUDIENCE
                                + "\\\",\\\"client_id\\\":" + CLIENT_ID
                                + ",\\\"sub\\\":\\\"" + EXTERNAL_SUBJECT
                                + "\\\",\\\"iat\\\":" + issuedAt
                                + ",\\\"exp\\\":" + expiresAt
                                + ",\\\"jti\\\":\\\"" + jwtId + "\\\"";
                        String preferredOrgId = System.getenv("FORGE_ACTIVE_ORG_ID");
                        if (preferredOrgId != null && !preferredOrgId.isBlank()) {
                            if (!preferredOrgId.matches("[1-9][0-9]*")) {
                                throw new IllegalStateException("FORGE_ACTIVE_ORG_ID 必须是正整数");
                            }
                            payload += ",\\\"forge_org_id\\\":" + preferredOrgId;
                        }
                        payload += "}";
                        String signingInput = base64Url(header.getBytes(StandardCharsets.UTF_8))
                                + "." + base64Url(payload.getBytes(StandardCharsets.UTF_8));
                        Signature signature = Signature.getInstance("SHA256withRSA");
                        signature.initSign(readPrivateKey());
                        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
                        return signingInput + "." + base64Url(signature.sign());
                    }

                    private static PrivateKey readPrivateKey() throws Exception {
                        String pem = Files.readString(Path.of(PRIVATE_KEY_FILE), StandardCharsets.US_ASCII)
                                .replace("-----BEGIN PRIVATE KEY-----", "")
                                .replace("-----END PRIVATE KEY-----", "")
                                .replaceAll("\\\\s", "");
                        byte[] encoded = Base64.getDecoder().decode(pem);
                        return KeyFactory.getInstance("RSA")
                                .generatePrivate(new PKCS8EncodedKeySpec(encoded));
                    }

                    private static String base64Url(byte[] value) {
                        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
                    }

                    private static String encode(String value) {
                        return URLEncoder.encode(value, StandardCharsets.UTF_8);
                    }

                    private static String requireEnvironment(String name) {
                        String value = System.getenv(name);
                        if (value == null || value.isBlank()) {
                            throw new IllegalStateException("缺少环境变量: " + name);
                        }
                        return value.trim();
                    }

                    private static String readJsonString(String json, String field) {
                        int name = json.indexOf("\\\"" + field + "\\\"");
                        int colon = name < 0 ? -1 : json.indexOf(':', name);
                        int start = colon < 0 ? -1 : json.indexOf('\\\"', colon);
                        int end = start < 0 ? -1 : json.indexOf('\\\"', start + 1);
                        if (start < 0 || end < 0) {
                            throw new IllegalStateException("Token 响应缺少 " + field);
                        }
                        return json.substring(start + 1, end);
                    }

                    private static void requireSuccess(
                            String stage, HttpResponse<String> response) {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw new IllegalStateException(
                                    stage + "失败，HTTP " + response.statusCode()
                                            + ": " + response.body());
                        }
                    }
                }
                """.formatted(
                client.getId(), client.getClientCode(), client.getUserAssertionKeyId(),
                identityIssuer, baseUrl() + "/oauth2/token", invokeUrl, openapiResource,
                capability.getCapabilityCode(), USER_ASSERTION_TOKEN_TYPE,
                javaStringLiteral(requestExample), userAssertionMaxTtlSeconds,
                TOKEN_EXCHANGE, idempotency);
    }

    private String javaStringLiteral(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n");
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException("Java 调用示例生成失败");
        }
    }

    private String idempotencyHeader(AiCapability capability) {
        return "READ_ONLY".equals(capability.getBehavior()) ? ""
                : "  -H 'Idempotency-Key: <UNIQUE_BUSINESS_REQUEST_KEY>' \\\n";
    }

    private String compactJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value).replace("'", "'\\''");
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException("调用示例生成失败");
        }
    }

    private JsonNode readPolicy(String content) {
        if (StringUtils.isBlank(content)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode policy = objectMapper.readTree(content);
            return policy != null && policy.isObject() ? policy : objectMapper.createObjectNode();
        }
        catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode();
        }
    }

    private Set<String> readTextSet(JsonNode values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null && values.isArray()) {
            values.forEach(item -> {
                String value = item.isTextual() ? StringUtils.trimToNull(item.asText()) : null;
                if (value != null) {
                    result.add(value);
                }
            });
        }
        return result;
    }

    private Set<String> authModes(String value) {
        Set<String> result = new LinkedHashSet<>();
        if (value != null) {
            Arrays.stream(value.split(",")).map(String::trim)
                    .filter(item -> !item.isBlank()).forEach(result::add);
        }
        return result;
    }

    private CapabilityClientActorMode actorMode(String value) {
        try {
            return CapabilityClientActorMode.valueOf(value);
        }
        catch (Exception exception) {
            return null;
        }
    }

    private boolean expired(LocalDateTime expiresAt, LocalDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    private boolean sameMajor(String left, String right) {
        String leftMajor = major(left);
        String rightMajor = major(right);
        return leftMajor != null && leftMajor.equals(rightMajor);
    }

    private String major(String version) {
        if (StringUtils.isBlank(version)) {
            return null;
        }
        int separator = version.indexOf('.');
        if (separator <= 0) {
            return null;
        }
        String major = version.substring(0, separator);
        return major.chars().allMatch(Character::isDigit) ? major : null;
    }

    private boolean requiresTokenExchange(
            AiCapability capability,
            AiCapabilityClient client) {
        if ("USER".equals(capability.getRequiredActorType())) {
            return true;
        }
        CapabilityClientActorMode mode = actorMode(client.getActorMode());
        return "BOTH".equals(capability.getRequiredActorType())
                && mode == CapabilityClientActorMode.USER_DELEGATION;
    }

    private String baseUrl() {
        try {
            URI uri = URI.create(openapiResource);
            return uri.getScheme() + "://" + uri.getRawAuthority();
        }
        catch (RuntimeException exception) {
            throw new BusinessException("开放平台 OpenAPI resource 配置无效");
        }
    }

    private record ExecutionAvailability(boolean available, String message) {
    }
}
