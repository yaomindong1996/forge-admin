package com.mdframe.forge.plugin.capability.secureaction.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.spi.ScopeBasedCapabilityAuthorizationPolicy;
import com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class SecureActionCatalogService {

    private static final String PLATFORM_INVOKE_PERMISSION =
            "ai:capability:business-action:invoke";
    private static final String FLOW_INVOKE_PERMISSION =
            "ai:capability:flow-action:invoke";

    private final SecureActionCatalogMapper catalogMapper;
    private final ObjectMapper objectMapper;

    public SecureActionSearchResult search(String keyword, Integer requestedLimit) {
        ExecutionIdentity identity = requireIdentity();
        int limit = requestedLimit == null ? 20 : requestedLimit;
        if (limit < 1 || limit > 50) {
            throw new BusinessException("limit 必须在 1 到 50 之间");
        }
        List<SecureActionDescriptor> authorized = new ArrayList<>();
        String afterCode = null;
        Long afterId = null;
        int batchSize = Math.max(20, limit + 1);
        while (authorized.size() <= limit) {
            List<SecureActionCatalogRow> rows;
            try {
                rows = catalogMapper.selectGrantedActions(
                        identity.loginUser().getTenantId(), identity.clientId(),
                        StringUtils.trimToNull(keyword), afterCode, afterId, batchSize);
            }
            catch (RuntimeException exception) {
                throw unavailable("CATALOG_UNAVAILABLE", exception);
            }
            if (rows.isEmpty()) {
                break;
            }
            for (SecureActionCatalogRow row : rows) {
                SecureActionDescriptor descriptor = toDescriptor(row);
                if (hasDiscoveryScope(identity, descriptor.capabilityCode())
                        && hasPermission(identity.loginUser(), platformPermission(descriptor))
                        && hasPermission(identity.loginUser(), descriptor.permission())) {
                    authorized.add(descriptor);
                }
                if (authorized.size() > limit) {
                    break;
                }
            }
            SecureActionCatalogRow last = rows.get(rows.size() - 1);
            afterCode = last.getCapabilityCode();
            afterId = last.getCapabilityId();
            if (authorized.size() > limit || rows.size() < batchSize) {
                break;
            }
        }
        boolean hasMore = authorized.size() > limit;
        List<SecureActionDescriptor> items = hasMore
                ? authorized.subList(0, limit) : authorized;
        return new SecureActionSearchResult(List.copyOf(items), hasMore);
    }

    public SecureActionDescriptor requireAuthorized(String capabilityCode) {
        ExecutionIdentity identity = requireIdentity();
        SecureActionCatalogRow row;
        try {
            row = catalogMapper.selectGrantedAction(
                    identity.loginUser().getTenantId(), identity.clientId(), capabilityCode);
        }
        catch (RuntimeException exception) {
            throw unavailable("AUTHORIZATION_UNAVAILABLE", exception);
        }
        if (row == null) {
            throw new BusinessException(403, "能力不存在或未授权");
        }
        SecureActionDescriptor descriptor = toDescriptor(row);
        if (!hasPermission(identity.loginUser(), platformPermission(descriptor))
                || !hasPermission(identity.loginUser(), descriptor.permission())) {
            throw new BusinessException(403, "当前用户无权执行该业务动作");
        }
        return descriptor;
    }

    public ExecutionIdentity requireIdentity() {
        ExecutionIdentity identity = ExecutionIdentityContextHolder.current()
                .orElseThrow(() -> new BusinessException(401, "缺少可信 MCP 执行身份"));
        LoginUser user = identity.loginUser();
        if (user.getTenantId() == null || user.getTenantId() <= 0
                || user.getActiveOrgId() == null || user.getActiveOrgId() <= 0) {
            throw new BusinessException(401, "可信 MCP 执行身份缺少租户或当前组织");
        }
        return identity;
    }

    public void requireDiscoveryScope(ExecutionIdentity identity, String capabilityCode) {
        if (!hasDiscoveryScope(identity, capabilityCode)) {
            throw new BusinessException(403, "INSUFFICIENT_SCOPE");
        }
    }

    public void requireInvocationScope(ExecutionIdentity identity, String capabilityCode) {
        if (!hasScope(identity, ScopeBasedCapabilityAuthorizationPolicy.INVOKE_SCOPE,
                "capability:invoke:" + capabilityCode)) {
            throw new BusinessException(403, "INSUFFICIENT_SCOPE");
        }
    }

    private SecureActionDescriptor toDescriptor(SecureActionCatalogRow row) {
        try {
            JsonNode versionPolicy = objectMapper.readTree(row.getPolicySnapshot());
            JsonNode grantPolicy = objectMapper.readTree(row.getFieldPolicy());
            String sourceType = StringUtils.defaultIfBlank(row.getSourceType(), "BUSINESS_ACTION");
            String behavior = StringUtils.defaultIfBlank(row.getBehavior(), "ACTION");
            if (!"MCP_ELICITATION".equals(versionPolicy.path("confirmationMode").asText())) {
                throw unavailable("CATALOG_UNAVAILABLE", null);
            }
            String[] source = row.getSourceKey().split("/", -1);
            if (source.length != 3) {
                throw unavailable("CATALOG_UNAVAILABLE", null);
            }
            int publishedVersion = Integer.parseInt(row.getSourceVersion());
            if (publishedVersion <= 0
                    || versionPolicy.path("publishedObjectVersion").asInt() != publishedVersion) {
                throw unavailable("CATALOG_UNAVAILABLE", null);
            }
            Set<String> effectiveFields = new LinkedHashSet<>();
            Set<String> required = new LinkedHashSet<>();
            JsonNode sourceInputSchema = objectMapper.readTree(row.getInputSchema());
            JsonNode effectiveInputSchema;
            if ("BUSINESS_ACTION".equals(sourceType)
                    && "ACTION".equals(behavior)) {
                effectiveFields.addAll(fields(versionPolicy.path("allowedFields")));
                effectiveFields.retainAll(fields(grantPolicy.path("allowedFields")));
                if (effectiveFields.isEmpty()) {
                    throw unavailable("CATALOG_UNAVAILABLE", null);
                }
                required.addAll(fields(versionPolicy.path("requiredFields")));
                required.retainAll(effectiveFields);
                effectiveInputSchema = effectiveInputSchema(
                        sourceInputSchema, effectiveFields, required);
            }
            else if ("FLOW_ACTION".equals(sourceType)
                    && "FLOW".equals(behavior)) {
                Set<String> operations = fields(versionPolicy.path("allowedOperations"));
                operations.retainAll(fields(grantPolicy.path("allowedOperations")));
                if (!operations.contains(source[2])
                        || !source[2].equals(versionPolicy.path("operation").asText())) {
                    throw unavailable("CATALOG_UNAVAILABLE", null);
                }
                effectiveInputSchema = sourceInputSchema;
            }
            else {
                throw unavailable("CATALOG_UNAVAILABLE", null);
            }
            return new SecureActionDescriptor(
                    row.getCapabilityId(), row.getCapabilityCode(), row.getCapabilityName(),
                    row.getDescription(), row.getVersion(), sourceType, row.getSourceKey(),
                    row.getSourceVersion(), behavior, row.getRiskLevel(), source[0], source[1], source[2],
                    publishedVersion, versionPolicy.path("permission").asText(),
                    Set.copyOf(effectiveFields), Set.copyOf(required), versionPolicy,
                    effectiveInputSchema,
                    objectMapper.readTree(row.getOutputSchema()));
        }
        catch (SecureActionUnavailableException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw unavailable("CATALOG_UNAVAILABLE", exception);
        }
    }

    private JsonNode effectiveInputSchema(
            JsonNode sourceSchema,
            Set<String> allowedFields,
            Set<String> requiredFields) {
        if (!(sourceSchema instanceof ObjectNode root)
                || !(root.path("properties") instanceof ObjectNode rootProperties)
                || !(rootProperties.path("arguments") instanceof ObjectNode arguments)
                || !(arguments.path("properties") instanceof ObjectNode argumentProperties)) {
            throw unavailable("CATALOG_UNAVAILABLE", null);
        }
        Set<String> schemaFields = new LinkedHashSet<>();
        argumentProperties.fieldNames().forEachRemaining(schemaFields::add);
        if (!schemaFields.containsAll(allowedFields)) {
            throw unavailable("CATALOG_UNAVAILABLE", null);
        }
        schemaFields.stream()
                .filter(field -> !allowedFields.contains(field))
                .forEach(argumentProperties::remove);
        ArrayNode required = objectMapper.createArrayNode();
        requiredFields.stream().sorted().forEach(required::add);
        arguments.set("required", required);
        arguments.put("additionalProperties", false);
        return root;
    }

    private SecureActionUnavailableException unavailable(String errorCode, Throwable cause) {
        return new SecureActionUnavailableException(errorCode, cause);
    }

    private Set<String> fields(JsonNode node) {
        Set<String> result = new LinkedHashSet<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (item.isTextual() && !item.asText().isBlank()) {
                    result.add(item.asText());
                }
            });
        }
        return result;
    }

    private boolean hasPermission(LoginUser user, String permission) {
        if (StringUtils.isBlank(permission) || user.getPermissions() == null) {
            return false;
        }
        Set<String> permissions = user.getPermissions();
        if (permissions.contains("*:*:*") || permissions.contains(permission)) {
            return true;
        }
        int splitIndex = permission.lastIndexOf(':');
        while (splitIndex > 0) {
            if (permissions.contains(permission.substring(0, splitIndex) + ":*")) {
                return true;
            }
            splitIndex = permission.lastIndexOf(':', splitIndex - 1);
        }
        return false;
    }

    private boolean hasDiscoveryScope(ExecutionIdentity identity, String capabilityCode) {
        return hasScope(identity, ScopeBasedCapabilityAuthorizationPolicy.DISCOVER_SCOPE,
                "capability:discover:" + capabilityCode);
    }

    private String platformPermission(SecureActionDescriptor descriptor) {
        return "FLOW_ACTION".equals(descriptor.sourceType())
                ? FLOW_INVOKE_PERMISSION : PLATFORM_INVOKE_PERMISSION;
    }

    private boolean hasScope(ExecutionIdentity identity, String commonScope, String specificScope) {
        Set<String> scopes = identity == null ? null : identity.scopes();
        return scopes != null && (scopes.contains(ScopeBasedCapabilityAuthorizationPolicy.ALL_SCOPE)
                || scopes.contains(commonScope) || scopes.contains(specificScope));
    }
}
