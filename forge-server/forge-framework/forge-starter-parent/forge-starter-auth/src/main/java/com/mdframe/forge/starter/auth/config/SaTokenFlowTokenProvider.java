package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.flow.client.FlowTokenAcquisitionException;
import com.mdframe.forge.flow.client.FlowTokenProvider;
import com.mdframe.forge.starter.core.constant.FlowDelegationConstants;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 基于 Sa-Token 的流程客户端 Token 供应商
 * <p>
 * 在引入 {@code forge-starter-auth} 的服务中自动注册。
 * 每次 {@link com.mdframe.forge.flow.client.FlowClient} 发起 HTTP 请求前，
 * 自动为当前调用取出可用的 Sa-Token，透传到 Authorization 请求头。
 * <p>
 * 取 Token 策略分两条路径：
 * <ul>
 *     <li>浏览器在线会话（绝大多数调用）：直接透传现有 token，
 *     flow 服务与其共享 Redis 会话，标准登录校验即可识别，
 *     不为常规调用制造任何新会话；</li>
 *     <li>MCP / 开放网关 / 高危回调等无浏览器会话的程序化调用：
 *     签发短时效委托 Token（设备名 {@code mcp-flow:} 前缀，60s TTL），
 *     按身份（用户+租户+组织+客户端）在进程内短窗口复用，
 *     避免每次调用都新建会话挤占账号会话池上限。</li>
 * </ul>
 * 曾把浏览器路径也改为签发委托会话，结果常规页面操作持续新增临时会话，
 * 与开发期频繁重登（is-share=false、timeout=1800s 内旧会话仍占位）叠加后
 * 触发 logoutByMaxLoginCount 把最早的浏览器会话挤下线，故回归透传设计。
 *
 * @author forge
 */
@Slf4j
@Component
@ConditionalOnClass(name = "com.mdframe.forge.flow.client.FlowClient")
public class SaTokenFlowTokenProvider implements FlowTokenProvider {

    private static final String LOGIN_USER_KEY = "loginUser";
    private static final long DELEGATED_TOKEN_TIMEOUT_SECONDS = 60L;
    /** 委托 Token 剩余有效期低于该值时换发新 Token，避免把临近过期的 Token 交给调用方 */
    private static final long DELEGATED_TOKEN_REFRESH_MARGIN_MS = 10_000L;

    private final Supplier<StpLogic> stpLogicSupplier;
    private final LongSupplier clock;

    /** 委托 Token 进程内缓存：key=userId:tenantId:orgId:clientId，过期条目在下次访问时被惰性换发 */
    private final ConcurrentHashMap<String, CachedDelegationToken> delegatedTokenCache = new ConcurrentHashMap<>();
    /** 签发串行化锁：按身份键持有，条目随身份数量有界，无需清理 */
    private final ConcurrentHashMap<String, Object> delegationCreationLocks = new ConcurrentHashMap<>();

    public SaTokenFlowTokenProvider() {
        this(StpUtil::getStpLogic, System::currentTimeMillis);
    }

    SaTokenFlowTokenProvider(Supplier<StpLogic> stpLogicSupplier) {
        this(stpLogicSupplier, System::currentTimeMillis);
    }

    SaTokenFlowTokenProvider(Supplier<StpLogic> stpLogicSupplier, LongSupplier clock) {
        this.stpLogicSupplier = stpLogicSupplier;
        this.clock = clock;
    }

    @Override
    public String getToken() {
        // 程序化委托身份（MCP / 开放网关 / 高危回调 / 异步身份装饰器）：签发短时效委托 Token
        ExecutionIdentity identity = ExecutionIdentityContextHolder.current().orElse(null);
        if (identity != null) {
            return createDelegatedFlowToken(identity);
        }
        // 浏览器在线会话：直接透传现有 token，零新增会话。
        // 不得在此路径签发委托会话：常规页面操作高频发生，任何签发都会持续挤占
        // 账号会话池（max-login-count 达到上限时 logoutByMaxLoginCount 注销最早会话，
        // 表现为用户"未点击退出却自动被注销"）
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
            return reuseOrIssueDelegatedToken(identity, loginUser);
        }
        catch (Exception exception) {
            throw new FlowTokenAcquisitionException("MCP_FLOW_DELEGATION_TOKEN_UNAVAILABLE", exception);
        }
    }

    /**
     * 同一委托身份在有效期内复用同一枚临时 Token，避免每次流程调用都 createLoginSession。
     * <p>
     * Sa-Token 1.38 默认 max-login-count=12，委托会话与浏览器会话共用同一账号会话池；
     * 若每次调用都用随机设备名新建会话（isShare 复用永远不命中），一次页面加载的
     * 并发流程请求即可在 60s TTL 内堆积超过上限，触发 logoutByMaxLoginCount
     * 把最早的浏览器会话挤下线（表现为用户"未点击退出却自动被注销"）。
     * 复用后每个进程同一身份同时最多存活约 2 枚委托会话（换发窗口内的新旧重叠），远离上限。
     */
    private String reuseOrIssueDelegatedToken(ExecutionIdentity identity, LoginUser loginUser) {
        String cacheKey = identity.actorUserId() + ":" + loginUser.getTenantId() + ":"
                + loginUser.getActiveOrgId() + ":" + identity.clientId();
        CachedDelegationToken cached = delegatedTokenCache.get(cacheKey);
        if (isReusable(cached)) {
            return cached.token();
        }
        // 并发流程调用（如同一次页面加载的多个请求）可能同时未命中缓存，
        // 必须按身份串行化签发，否则突发调用依旧会堆积会话触发挤下线
        Object lock = delegationCreationLocks.computeIfAbsent(cacheKey, key -> new Object());
        synchronized (lock) {
            cached = delegatedTokenCache.get(cacheKey);
            if (isReusable(cached)) {
                return cached.token();
            }
            String token = issueDelegatedToken(identity, loginUser);
            delegatedTokenCache.put(cacheKey, new CachedDelegationToken(
                    token, clock.getAsLong() + DELEGATED_TOKEN_TIMEOUT_SECONDS * 1000L));
            return token;
        }
    }

    private boolean isReusable(CachedDelegationToken cached) {
        return cached != null && cached.expiresAt() - clock.getAsLong() > DELEGATED_TOKEN_REFRESH_MARGIN_MS;
    }

    private String issueDelegatedToken(ExecutionIdentity identity, LoginUser loginUser) {
        StpLogic stpLogic = stpLogicSupplier.get();
        String delegationSessionId = UUID.randomUUID().toString();
        SaLoginParameter loginModel = SaLoginParameter.create()
                .setDevice(FlowDelegationConstants.FLOW_DELEGATION_DEVICE_PREFIX + identity.clientId() + ":"
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

    private record CachedDelegationToken(String token, long expiresAt) {
    }
}
