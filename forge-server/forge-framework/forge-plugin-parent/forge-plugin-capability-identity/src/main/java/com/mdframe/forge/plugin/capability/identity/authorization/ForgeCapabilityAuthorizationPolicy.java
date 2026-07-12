package com.mdframe.forge.plugin.capability.identity.authorization;

import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityGrantDecision;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityGrantService;
import com.mdframe.forge.plugin.capability.model.CapabilityAuthorizationDecision;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;
import com.mdframe.forge.plugin.capability.spi.CapabilityAuthorizationPolicy;
import com.mdframe.forge.plugin.capability.spi.ScopeBasedCapabilityAuthorizationPolicy;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

/**
 * MCP 能力运行时授权交集：Token scope、客户端 grant、当前用户权限和组织身份必须同时成立。
 */
public final class ForgeCapabilityAuthorizationPolicy implements CapabilityAuthorizationPolicy {

    private static final Logger log = LoggerFactory.getLogger(ForgeCapabilityAuthorizationPolicy.class);
    private static final String PING_CAPABILITY = "capability.ping";

    private final ScopeBasedCapabilityAuthorizationPolicy scopePolicy =
            new ScopeBasedCapabilityAuthorizationPolicy();
    private final CapabilityGrantService grantService;
    private final ForgeCapabilityPermissionMapper permissionMapper;

    public ForgeCapabilityAuthorizationPolicy(
            CapabilityGrantService grantService,
            ForgeCapabilityPermissionMapper permissionMapper) {
        this.grantService = grantService;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public CapabilityAuthorizationDecision evaluateDiscovery(
            CapabilityDefinition definition,
            CapabilityCallerContext caller) {
        CapabilityAuthorizationDecision scopeDecision = scopePolicy.evaluateDiscovery(definition, caller);
        return scopeDecision.allowed()
                ? evaluateGovernance(definition, caller, permissionMapper.discoveryPermission(definition))
                : scopeDecision;
    }

    @Override
    public CapabilityAuthorizationDecision evaluateInvocation(
            CapabilityDefinition definition,
            CapabilityCallerContext caller) {
        CapabilityAuthorizationDecision scopeDecision = scopePolicy.evaluateInvocation(definition, caller);
        return scopeDecision.allowed()
                ? evaluateGovernance(definition, caller, permissionMapper.invocationPermission(definition))
                : scopeDecision;
    }

    private CapabilityAuthorizationDecision evaluateGovernance(
            CapabilityDefinition definition,
            CapabilityCallerContext caller,
            String requiredPermission) {
        ExecutionIdentity identity = ExecutionIdentityContextHolder.current().orElse(null);
        if (!matchesTrustedIdentity(identity, caller)) {
            return CapabilityAuthorizationDecision.deny(CapabilityErrorCode.UNAUTHENTICATED);
        }
        // 阶段 2.0 唯一例外：ping 是所有已认证客户端统一可见、统一可调用的健康能力。
        if (PING_CAPABILITY.equals(definition.capabilityCode())) {
            return CapabilityAuthorizationDecision.allow();
        }
        if (!hasPermission(identity.loginUser(), requiredPermission)) {
            return CapabilityAuthorizationDecision.deny(CapabilityErrorCode.FORBIDDEN);
        }
        try {
            CapabilityGrantDecision decision = grantService.evaluate(
                    caller.tenantId(), caller.activeOrgId(), identity.clientId(),
                    definition.capabilityCode(), definition.version());
            if (decision.allowed()) {
                return CapabilityAuthorizationDecision.allow();
            }
            return CapabilityAuthorizationDecision.deny(mapGrantError(decision.errorCode()));
        }
        catch (RuntimeException exception) {
            log.error("[Capability授权] capabilityCode={}, resultCode=AUTHORIZATION_UNAVAILABLE, exceptionType={}",
                    definition.capabilityCode(), exception.getClass().getSimpleName(), exception);
            return CapabilityAuthorizationDecision.deny(CapabilityErrorCode.INTERNAL_ERROR);
        }
    }

    private boolean matchesTrustedIdentity(
            ExecutionIdentity identity,
            CapabilityCallerContext caller) {
        if (identity == null || caller == null) {
            return false;
        }
        LoginUser user = identity.loginUser();
        return Objects.equals(identity.clientCode(), caller.machineClientId())
                && Objects.equals(identity.actorUserId(), caller.userId())
                && Objects.equals(user.getTenantId(), caller.tenantId())
                && Objects.equals(user.getActiveOrgId(), caller.activeOrgId());
    }

    private boolean hasPermission(LoginUser user, String permission) {
        Set<String> permissions = user == null ? null : user.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
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

    private CapabilityErrorCode mapGrantError(String errorCode) {
        if ("TENANT_SCOPE_VIOLATION".equals(errorCode)) {
            return CapabilityErrorCode.TENANT_SCOPE_VIOLATION;
        }
        if ("ORG_SCOPE_VIOLATION".equals(errorCode)) {
            return CapabilityErrorCode.ORG_SCOPE_VIOLATION;
        }
        return CapabilityErrorCode.FORBIDDEN;
    }
}
