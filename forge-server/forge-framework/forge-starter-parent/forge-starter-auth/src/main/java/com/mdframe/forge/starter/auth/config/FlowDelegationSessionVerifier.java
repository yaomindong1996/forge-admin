package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 验证 Flow 服务当前 Sa-Token Session 是否由受信 MCP USER 委托桥签发。
 */
@Component
public class FlowDelegationSessionVerifier {

    static final String MARKER_KEY = "forge:flow:delegation";
    static final String ACTOR_USER_ID_KEY = "forge:flow:actor-user-id";
    static final String TENANT_ID_KEY = "forge:flow:tenant-id";
    static final String ACTIVE_ORG_ID_KEY = "forge:flow:active-org-id";
    static final String CLIENT_ID_KEY = "forge:flow:client-id";

    private final Supplier<SaSession> sessionSupplier;

    public FlowDelegationSessionVerifier() {
        this(StpUtil::getTokenSession);
    }

    FlowDelegationSessionVerifier(Supplier<SaSession> sessionSupplier) {
        this.sessionSupplier = sessionSupplier;
    }

    static void bind(SaSession session, ExecutionIdentity identity) {
        LoginUser user = identity.loginUser();
        session.set(MARKER_KEY, Boolean.TRUE);
        session.set(ACTOR_USER_ID_KEY, identity.actorUserId());
        session.set(TENANT_ID_KEY, user.getTenantId());
        session.set(ACTIVE_ORG_ID_KEY, user.getActiveOrgId());
        session.set(CLIENT_ID_KEY, identity.clientId());
    }

    public void requireTrustedDelegation() {
        LoginUser user;
        SaSession session;
        try {
            user = SessionHelper.getLoginUser();
            session = sessionSupplier.get();
        }
        catch (RuntimeException exception) {
            throw new IllegalArgumentException("FLOW_START_DELEGATION_REQUIRED", exception);
        }
        Long clientId = session == null ? null : longValue(session.get(CLIENT_ID_KEY));
        if (user == null || session == null
                || !Boolean.TRUE.equals(session.get(MARKER_KEY))
                || !Objects.equals(user.getUserId(), longValue(session.get(ACTOR_USER_ID_KEY)))
                || !Objects.equals(user.getTenantId(), longValue(session.get(TENANT_ID_KEY)))
                || !Objects.equals(user.getActiveOrgId(), longValue(session.get(ACTIVE_ORG_ID_KEY)))
                || clientId == null
                || clientId <= 0) {
            throw new IllegalArgumentException("FLOW_START_DELEGATION_REQUIRED");
        }
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }
}
