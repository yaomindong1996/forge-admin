package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.flow.client.FlowTokenAcquisitionException;
import com.mdframe.forge.flow.client.FlowTokenProvider;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 基于 Sa-Token 的流程客户端 Token 供应商
 * <p>
 * 在引入 {@code forge-starter-auth} 的服务中自动注册。
 * 每次 {@link com.mdframe.forge.flow.client.FlowClient} 发起 HTTP 请求前，
 * 自动从当前请求上下文中取出 Sa-Token，透传到 Authorization 请求头，
 * 解决服务内调用流程服务时出现"未登录"的问题。
 *
 * @author forge
 */
@Slf4j
@Component
@ConditionalOnClass(name = "com.mdframe.forge.flow.client.FlowClient")
public class SaTokenFlowTokenProvider implements FlowTokenProvider {

    private static final String LOGIN_USER_KEY = "loginUser";
    private static final long DELEGATED_TOKEN_TIMEOUT_SECONDS = 60L;

    private final Supplier<StpLogic> stpLogicSupplier;

    public SaTokenFlowTokenProvider() {
        this(StpUtil::getStpLogic);
    }

    SaTokenFlowTokenProvider(Supplier<StpLogic> stpLogicSupplier) {
        this.stpLogicSupplier = stpLogicSupplier;
    }

    @Override
    public String getToken() {
        ExecutionIdentity identity = ExecutionIdentityContextHolder.current().orElse(null);
        if (identity != null) {
            return createDelegatedFlowToken(identity);
        }
        try {
            StpLogic stpLogic = stpLogicSupplier.get();
            if (stpLogic.isLogin()) {
                return stpLogic.getTokenValue();
            }
        } catch (Exception e) {
            log.debug("[FlowTokenProvider] 获取 Sa-Token 失败（当前线程无登录上下文）: {}", e.getMessage());
        }
        return null;
    }

    private String createDelegatedFlowToken(ExecutionIdentity identity) {
        try {
            LoginUser loginUser = identity.loginUser();
            if (!"USER".equals(identity.actorType())
                    || loginUser == null
                    || identity.actorUserId() == null
                    || !Objects.equals(identity.actorUserId(), loginUser.getUserId())
                    || identity.clientId() == null
                    || identity.clientId() <= 0
                    || loginUser.getTenantId() == null
                    || loginUser.getTenantId() <= 0
                    || loginUser.getActiveOrgId() == null
                    || loginUser.getActiveOrgId() <= 0) {
                throw new IllegalStateException("MCP_FLOW_DELEGATION_IDENTITY_INVALID");
            }
            StpLogic stpLogic = stpLogicSupplier.get();
            String delegationSessionId = UUID.randomUUID().toString();
            SaLoginModel loginModel = SaLoginModel.create()
                    .setDevice("mcp-flow:" + identity.clientId() + ":"
                            + loginUser.getActiveOrgId() + ":" + delegationSessionId)
                    .setTimeout(DELEGATED_TOKEN_TIMEOUT_SECONDS)
                    .setActiveTimeout(DELEGATED_TOKEN_TIMEOUT_SECONDS)
                    .setIsLastingCookie(false)
                    .setIsWriteHeader(false);
            String token = stpLogic.createLoginSession(identity.actorUserId(), loginModel);
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("MCP_FLOW_DELEGATION_TOKEN_EMPTY");
            }
            SaSession tokenSession = stpLogic.getTokenSessionByToken(token, true);
            if (tokenSession == null) {
                throw new IllegalStateException("MCP_FLOW_DELEGATION_SESSION_UNAVAILABLE");
            }
            tokenSession.set(LOGIN_USER_KEY, loginUser);
            FlowDelegationSessionVerifier.bind(tokenSession, identity);
            return token;
        }
        catch (Exception exception) {
            throw new FlowTokenAcquisitionException("MCP_FLOW_DELEGATION_TOKEN_UNAVAILABLE", exception);
        }
    }
}
