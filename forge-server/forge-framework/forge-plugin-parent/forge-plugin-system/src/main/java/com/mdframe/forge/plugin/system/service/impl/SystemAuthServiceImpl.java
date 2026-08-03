package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicy;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicyResolver;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.config.config.LoginConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.auth.domain.*;
import com.mdframe.forge.plugin.system.service.ISysOnlineUserService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.auth.service.IAuthService;
import com.mdframe.forge.starter.auth.service.ICaptchaService;
import com.mdframe.forge.starter.auth.strategy.AuthStrategyFactory;
import com.mdframe.forge.starter.auth.strategy.IAuthStrategy;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 系统认证服务实现
 * 使用策略模式实现不同认证方式的灵活扩展
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemAuthServiceImpl implements IAuthService {

    private static final String DEFAULT_USER_CLIENT = "pc";
    private static final String SSO_TICKET_CACHE_KEY = "auth:sso:ticket:";
    private static final String CLIENT_AUTHENTICATION_FAILED = "客户端认证失败";
    private static final long SSO_TICKET_EXPIRE_SECONDS = 60L;

    private final SysUserMapper userMapper;
    private final ICaptchaService captchaService;
    private final AuthStrategyFactory authStrategyFactory;
    private final IUserLoadService userLoadService;  // 委托给用户加载服务
    private final ISysOnlineUserService onlineUserService;
    private final AuthProperties authProperties;
    private final ConfigManagerService configManagerService;
    private final IClientService clientService;
    private final ICacheService cacheService;
    private final LoginCaptchaPolicyResolver captchaPolicyResolver;
    private final SysTenantMapper tenantMapper;

    // ==================== 核心认证方法 ====================

    @Override
    public LoginResult login(LoginRequest request) {
        if (StrUtil.isBlank(request.getAuthType())) {
            request.setAuthType("password");
        }
        
        if (StrUtil.isBlank(request.getUserClient())) {
            request.setUserClient(DEFAULT_USER_CLIENT);
        }
        
        SysClient client = validateAndLoadClient(
            request.getUserClient(),
            request.getAppId(),
            request.getAppSecret()
        );

        IAuthStrategy strategy = authStrategyFactory.getStrategy(
                request.getAuthType(),
                request.getUserClient()
        );

        log.info("开始认证: authType={}, userClient={}, strategy={}",
                request.getAuthType(),
                request.getUserClient(),
                strategy.getClass().getSimpleName());

        LoginUser loginUser = strategy.authenticate(request);

        return issueTokenForUser(loginUser, client, request.getUserClient());
    }

    /**
     * 处理同一账号登录策略
     *
     * @param userId 用户ID
     * @param client 客户端配置
     * @param userClient 客户端编码
     */
    private void handleSameAccountLogin(Long userId, SysClient client, String userClient) {
        if (!authProperties.getEnableOnlineUserManagement()) {
            return;
        }

        if (Boolean.TRUE.equals(client.getConcurrentLogin())) {
            log.debug("客户端允许并发登录: userId={}, client={}", userId, client.getClientCode());
            return;
        }

        List<String> sameClientTokens = getUserTokensByClient(userId, userClient);
        String strategy = authProperties.getSameAccountLoginStrategy();
        
        switch (strategy) {
            case "allow_concurrent":
                log.debug("允许同一账号并发登录: userId={}", userId);
                break;
                
            case "replace_old":
                try {
                    kickoutUserTokens(sameClientTokens);
                    log.info("同一账号新登录踢出旧登录: userId={}, client={}", userId, userClient);
                } catch (Exception e) {
                    log.error("踢出旧会话失败: userId={}", userId, e);
                }
                break;
                
            case "reject_new":
                if (CollUtil.isNotEmpty(sameClientTokens)) {
                    throw new RuntimeException("该账号已在当前客户端登录,请先退出后再登录");
                }
                log.info("同一账号拒绝新登录: userId={}, client={}", userId, userClient);
                break;
                
            default:
                log.warn("未知的同一账号登录策略: {}", strategy);
        }
    }

    @Override
    public SsoTicketResult createSsoTicket(SsoTicketRequest request) {
        LoginUser currentUser = SessionHelper.getLoginUser();
        if (currentUser == null) {
            throw new RuntimeException("未登录");
        }

        if (request == null || StrUtil.isBlank(request.getTargetClient())) {
            throw new RuntimeException("目标客户端不能为空");
        }

        SysClient targetClient = validateSsoTargetClient(request.getTargetClient());
        String redirectPath = normalizeRedirectPath(request.getRedirectPath());
        String ticket = IdUtil.fastSimpleUUID();

        SsoTicketPayload payload = new SsoTicketPayload();
        payload.setTicket(ticket);
        payload.setSourceToken(StpUtil.getTokenValue());
        payload.setSourceClient(StrUtil.blankToDefault(currentUser.getUserClient(), DEFAULT_USER_CLIENT));
        payload.setTargetClient(targetClient.getClientCode());
        payload.setUserId(currentUser.getUserId());
        payload.setUsername(currentUser.getUsername());
        payload.setTenantId(currentUser.getTenantId());
        payload.setRedirectPath(redirectPath);
        payload.setIssuedAt(System.currentTimeMillis());

        cacheService.set(buildSsoTicketCacheKey(ticket), payload, SSO_TICKET_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("生成SSO票据成功: userId={}, sourceClient={}, targetClient={}, redirectPath={}",
                currentUser.getUserId(), payload.getSourceClient(), targetClient.getClientCode(), redirectPath);

        return SsoTicketResult.builder()
                .ticket(ticket)
                .expiresIn(SSO_TICKET_EXPIRE_SECONDS)
                .targetClient(targetClient.getClientCode())
                .redirectPath(redirectPath)
                .build();
    }

    @Override
    public LoginResult exchangeSsoTicket(SsoExchangeRequest request) {
        if (request == null || StrUtil.isBlank(request.getTicket())) {
            throw new RuntimeException("SSO票据不能为空");
        }

        String cacheKey = buildSsoTicketCacheKey(request.getTicket().trim());
        SsoTicketPayload payload = cacheService.get(cacheKey, SsoTicketPayload.class);
        if (payload == null) {
            throw new RuntimeException("SSO票据不存在或已过期");
        }
        if (!cacheService.delete(cacheKey)) {
            throw new RuntimeException("SSO票据已失效或已使用");
        }

        validateSsoSourceSession(payload);

        SysClient targetClient = validateSsoTargetClient(payload.getTargetClient());
        LoginUser loginUser = rebuildSsoLoginUser(payload);

        log.info("开始SSO票据交换: userId={}, sourceClient={}, targetClient={}, redirectPath={}",
                payload.getUserId(), payload.getSourceClient(), payload.getTargetClient(), payload.getRedirectPath());

        return issueTokenForUser(loginUser, targetClient, payload.getTargetClient());
    }

    SysClient validateAndLoadClient(String userClient, String appId, String appSecret) {
        SysClient client = loadEnabledClient(userClient);

        boolean requiresSecret = requiresClientSecret(client, userClient);

        if (StrUtil.isBlank(appId)) {
            throw new RuntimeException(CLIENT_AUTHENTICATION_FAILED);
        }

        if (!Objects.equals(client.getAppId(), appId)) {
            log.warn("客户端AppId不匹配: client={}", userClient);
            throw new RuntimeException(CLIENT_AUTHENTICATION_FAILED);
        }

        if (requiresSecret) {
            if (StrUtil.isBlank(appSecret)) {
                throw new RuntimeException(CLIENT_AUTHENTICATION_FAILED);
            }

            if (!clientService.validateAppSecret(client, appSecret)) {
                log.warn("客户端AppSecret不匹配: client={}", userClient);
                throw new RuntimeException(CLIENT_AUTHENTICATION_FAILED);
            }
        }

        log.info("客户端验证通过: client={}", userClient);
        return client;
    }

    private SysClient loadEnabledClient(String userClient) {
        String clientCode = StrUtil.blankToDefault(userClient, DEFAULT_USER_CLIENT);
        SysClient client = clientService.getByCode(clientCode);
        if (client == null) {
            log.warn("客户端不存在或不可用: client={}", clientCode);
            throw new RuntimeException(CLIENT_AUTHENTICATION_FAILED);
        }
        if (client.getStatus() == null || client.getStatus() == 0) {
            log.warn("客户端不存在或不可用: client={}", clientCode);
            throw new RuntimeException(CLIENT_AUTHENTICATION_FAILED);
        }
        return client;
    }

    SysClient validateSsoTargetClient(String clientCode) {
        SysClient client = loadEnabledClient(clientCode);
        if (requiresClientSecret(client, clientCode)) {
            log.warn("SSO目标客户端不允许使用机密认证方式: client={}", clientCode);
            throw new RuntimeException("目标客户端不支持SSO");
        }
        return client;
    }

    private boolean requiresClientSecret(SysClient client, String clientCode) {
        try {
            return clientService.requiresAppSecret(client);
        } catch (IllegalArgumentException exception) {
            log.error("客户端认证方式配置无效: client={}", clientCode);
            throw new RuntimeException(CLIENT_AUTHENTICATION_FAILED);
        }
    }
    
    private void applyClientTokenConfig(SysClient client) {
        cn.dev33.satoken.config.SaTokenConfig config = cn.dev33.satoken.SaManager.getConfig();
        config.setTimeout(client.getTokenTimeout());
        config.setActiveTimeout(client.getTokenActivityTimeout());
        config.setIsConcurrent(client.getConcurrentLogin());
        config.setIsShare(client.getShareToken());
        SaManager.setConfig(config);
        log.debug("应用客户端Token配置: client={}, timeout={}s, concurrent={}",
            client.getClientCode(), client.getTokenTimeout(), client.getConcurrentLogin());
    }

    private LoginResult issueTokenForUser(LoginUser loginUser, SysClient client, String userClient) {
        String resolvedClient = StrUtil.blankToDefault(userClient, DEFAULT_USER_CLIENT);
        if (loginUser.getTenantId() == null) {
            throw new IllegalStateException("登录用户缺少租户信息");
        }
        executeWithRequiredTenant(loginUser.getTenantId(),
                () -> handleSameAccountLogin(loginUser.getUserId(), client, resolvedClient));
        applyClientTokenConfig(client);

        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setUserClient(resolvedClient);

        StpUtil.login(loginUser.getUserId(), new SaLoginModel().setDevice(resolvedClient));
        SessionHelper.setLoginUser(loginUser);
        try {
            onlineUserService.addOnlineUser(StpUtil.getTokenValue(), loginUser.getUserId());
        } catch (Exception e) {
            log.error("记录在线用户失败: userId={}, client={}", loginUser.getUserId(), resolvedClient, e);
        }

        log.info("用户登录成功: username={}, userId={}, client={}, tokenTimeout={}s",
                loginUser.getUsername(),
                loginUser.getUserId(),
                resolvedClient,
                client.getTokenTimeout());

        return buildLoginResult(loginUser);
    }

    @Override
    public void logout() {
        // Sa-Token logout 会触发登出监听器并清理 token session。
        // 不要提前清空 Session，否则登录日志监听器无法读取 LoginUser。
        StpUtil.logout();
    }

    // ==================== 用户信息加载（委托给UserLoadService） ====================

    @Override
    public LoginUser loadUserByUsername(String username, Long tenantId) {
        return userLoadService.loadUserByUsername(username, tenantId);
    }

    @Override
    public LoginUser loadUserByUsername(String username, Long tenantId, Long preferredActiveOrgId) {
        return userLoadService.loadUserByUsername(username, tenantId, preferredActiveOrgId);
    }

    @Override
    public LoginUser loadUserByPhone(String phone, Long tenantId) {
        return userLoadService.loadUserByPhone(phone, tenantId);
    }

    @Override
    public LoginUser loadUserByEmail(String email, Long tenantId) {
        return userLoadService.loadUserByEmail(email, tenantId);
    }

    @Override
    public LoginUser loadUserByUserId(Long userId, Long tenantId) {
        return userLoadService.loadUserByUserId(userId, tenantId);
    }

    @Override
    public LoginUser loadUserByUserId(Long userId, Long tenantId, Long preferredActiveOrgId) {
        return userLoadService.loadUserByUserId(userId, tenantId, preferredActiveOrgId);
    }

    @Override
    public boolean matchPassword(String rawPassword, String encodedPassword) {
        return userLoadService.matchPassword(rawPassword, encodedPassword);
    }

    @Override
    public boolean validateCode(String codeKey, String code) {
        return userLoadService.validateCode(codeKey, code);
    }

    @Override
    public boolean validatePhoneCode(String phone, String code) {
        return userLoadService.validatePhoneCode(phone, code);
    }

    // ==================== 用户注册相关 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginUser register(RegisterRequest request) {
        // 1. 参数校验
        validateRegisterRequest(request);
        
        // 2. 验证验证码
        if (!captchaService.validateAndDelete(request.getCodeKey(), request.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }
        
        // 3. 检查用户名是否已存在
        if (checkUsernameExists(request.getUsername(), request.getTenantId())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 4. 加密密码
        String encodedPassword = PasswordUtil.encrypt(request.getPassword());
        
        // 5. 保存用户信息
        LoginUser loginUser = saveUser(request, encodedPassword);
        
        log.info("用户注册成功: username={}", request.getUsername());
        return loginUser;
    }

    // ==================== 密码管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(String oldPassword, String newPassword) {
        // 1. 获取当前用户
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("未登录");
        }
        
        // 2. 验证旧密码
        String currentPassword = userLoadService.getUserPassword(loginUser.getUserId());
        if (!matchPassword(oldPassword, currentPassword)) {
            throw new RuntimeException("旧密码错误");
        }
        
        // 3. 加密新密码
        String encodedPassword = PasswordUtil.encrypt(newPassword);
        
        // 4. 更新密码
        boolean success = updateUserPassword(loginUser.getUserId(), encodedPassword);
        
        if (success) {
            loginUser.setForcePasswordChange(false);
            SessionHelper.setLoginUser(loginUser);
            log.info("用户修改密码成功: userId={}", loginUser.getUserId());
        }
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String username, String newPassword, String code, String codeKey) {
        // 1. 验证验证码
        if (!captchaService.validateAndDelete(codeKey, code)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        
        // 2. 查询用户
        LoginUser loginUser = loadUserByUsername(username, null);
        if (loginUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 3. 加密新密码
        String encodedPassword = PasswordUtil.encrypt(newPassword);
        
        // 4. 更新密码
        boolean success = updateUserPassword(loginUser.getUserId(), encodedPassword);
        
        if (success) {
            log.info("用户重置密码成功: username={}", username);
        }
        
        return success;
    }

    // ==================== 验证码和Token管理 ====================

    @Override
    public CaptchaResult getCaptcha() {
        // 生成图形验证码
        return captchaService.generateGraphicCaptcha();
    }

    @Override
    public SliderCaptchaResult getSliderCaptcha() {
        // 生成滑块验证码
        return captchaService.generateSliderCaptcha();
    }

    @Override
    public SmsCaptchaResult sendSmsCaptcha(String phone) {
        // 发送短信验证码
        return captchaService.sendSmsCaptcha(phone);
    }

    @Override
    public LoginConfigResult getLoginConfig() {
        return getLoginConfig(LoginCaptchaPolicyResolver.DEFAULT_USER_CLIENT);
    }

    @Override
    public LoginConfigResult getLoginConfig(String userClient) {
        return getLoginConfig(userClient, null);
    }

    @Override
    public LoginConfigResult getLoginConfig(String userClient, Long tenantId) {
        // 从配置中心获取全局登录配置，并叠加客户端覆盖项
        LoginConfig config = configManagerService.getLoginConfig();
        if (config == null) {
            config = new LoginConfig();
        }

        LoginCaptchaPolicy captchaPolicy = captchaPolicyResolver.resolve(userClient);
        SysTenant tenant = selectLoginTenantConfig(tenantId);

        return LoginConfigResult.builder()
                .enableCaptcha(captchaPolicy.getEnableCaptcha())
                .captchaType(captchaPolicy.getCaptchaType())
                .userClient(captchaPolicy.getUserClient())
                .captchaTypeSource(captchaPolicy.getCaptchaTypeSource())
                .globalCaptchaType(captchaPolicy.getGlobalCaptchaType())
                .clientCaptchaType(captchaPolicy.getClientCaptchaType())
                .enableRememberMe(config.getEnableRememberMe())
                .enableLoginLog(config.getEnableLoginLog())
                .enableIpLimit(config.getEnableIpLimit())
                .tenantId(tenant == null ? null : tenant.getId())
                .tenantName(tenant == null ? null : tenant.getTenantName())
                .browserIcon(tenant == null ? null : tenant.getBrowserIcon())
                .browserTitle(tenant == null ? null : tenant.getBrowserTitle())
                .systemName(tenant == null ? null : tenant.getSystemName())
                .systemLogo(tenant == null ? null : tenant.getSystemLogo())
                .systemIntro(tenant == null ? null : tenant.getSystemIntro())
                .copyrightInfo(tenant == null ? null : tenant.getCopyrightInfo())
                .systemLayout(tenant == null ? null : tenant.getSystemLayout())
                .systemTheme(tenant == null ? null : tenant.getSystemTheme())
                .themeConfig(tenant == null ? null : tenant.getThemeConfig())
                .build();
    }

    @Override
    public List<LoginTenantOption> listLoginTenantOptions() {
        return TenantContextHolder.executeIgnore(() ->
                tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                                .eq(SysTenant::getTenantStatus, 1)
                                .orderByAsc(SysTenant::getId)
                                .select(SysTenant::getId, SysTenant::getTenantName,
                                        SysTenant::getSystemName, SysTenant::getBrowserTitle))
                        .stream()
                        .map(tenant -> LoginTenantOption.builder()
                                .tenantId(tenant.getId())
                                .tenantName(tenant.getTenantName())
                                .systemName(tenant.getSystemName())
                                .browserTitle(tenant.getBrowserTitle())
                                .build())
                        .toList());
    }

    @Override
    public LoginResult refreshToken() {
        // 1. 检查是否登录
        if (!StpUtil.isLogin()) {
            throw new RuntimeException("未登录");
        }
        
        // 2. 获取当前用户
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户信息不存在");
        }
        
        // 3. 刷新Token（Sa-Token会自动延长过期时间）
        StpUtil.renewTimeout(StpUtil.getTokenTimeout());
        
        // 4. 构建返回结果
        return buildLoginResult(loginUser);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验注册请求参数
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (StrUtil.isBlank(request.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        if (StrUtil.isBlank(request.getPassword())) {
            throw new RuntimeException("密码不能为空");
        }
        if (StrUtil.isBlank(request.getCode()) || StrUtil.isBlank(request.getCodeKey())) {
            throw new RuntimeException("验证码不能为空");
        }
    }

    /**
     * 检查用户名是否已存在
     */
    private boolean checkUsernameExists(String username, Long tenantId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        if (tenantId != null) {
            wrapper.eq(SysUser::getTenantId, tenantId);
        }
        return userMapper.selectCount(wrapper) > 0;
    }

    /**
     * 保存用户
     */
    @Transactional(rollbackFor = Exception.class)
    protected LoginUser saveUser(RegisterRequest request, String encodedPassword) {
        // 1. 构建用户实体
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(encodedPassword);
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setTenantId(request.getTenantId());
        user.setUserType(2); // 普通用户
        user.setUserStatus(1); // 正常
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        // 2. 保存用户
        userMapper.insert(user);
        
        // 3. 构建返回结果
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setPhone(user.getPhone());
        loginUser.setEmail(user.getEmail());
        loginUser.setTenantId(user.getTenantId());
        loginUser.setUserType(user.getUserType());
        loginUser.setUserStatus(user.getUserStatus());
        
        return loginUser;
    }

    /**
     * 更新用户密码
     */
    @Transactional(rollbackFor = Exception.class)
    protected boolean updateUserPassword(Long userId, String encodedPassword) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getPassword, encodedPassword)
                .set(SysUser::getForcePasswordChange, false)
                .set(SysUser::getUpdateTime, LocalDateTime.now());
        return userMapper.update(null, wrapper) > 0;
    }

    /**
     * 构建登录结果
     */
    private LoginResult buildLoginResult(LoginUser loginUser) {
        String token = StpUtil.getTokenValue();
        long tokenTimeout = StpUtil.getTokenTimeout();
        
        return LoginResult.builder()
                .accessToken(token)
                .expiresIn(tokenTimeout)
                .tokenType("Bearer")
                .userInfo(loginUser)
                .forcePasswordChange(Boolean.TRUE.equals(loginUser.getForcePasswordChange()))
                .build();
    }

    private String buildSsoTicketCacheKey(String ticket) {
        return SSO_TICKET_CACHE_KEY + ticket;
    }

    private String normalizeRedirectPath(String redirectPath) {
        if (StrUtil.isBlank(redirectPath)) {
            return "/";
        }

        String normalized = redirectPath.trim();
        if (normalized.contains("://") || normalized.startsWith("//")) {
            throw new RuntimeException("跳转路径不合法");
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private void validateSsoSourceSession(SsoTicketPayload payload) {
        if (StrUtil.isBlank(payload.getSourceToken())) {
            throw new RuntimeException("SSO票据来源会话无效");
        }

        try {
            SaSession sourceSession = StpUtil.getTokenSessionByToken(payload.getSourceToken());
            Object loginUserObj = sourceSession == null ? null : sourceSession.get("loginUser");
            if (!(loginUserObj instanceof LoginUser sourceUser)
                    || !Objects.equals(sourceUser.getUserId(), payload.getUserId())) {
                throw new RuntimeException("SSO票据来源会话已失效");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("SSO票据来源会话已失效", e);
        }
    }

    private LoginUser rebuildSsoLoginUser(SsoTicketPayload payload) {
        SysUser user = userMapper.selectById(payload.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getUserStatus() == null || user.getUserStatus() != 1) {
            throw new RuntimeException("用户已被禁用或锁定");
        }

        LoginUser loginUser = loadUserByUsername(user.getUsername(), payload.getTenantId());
        loginUser.setUserClient(payload.getTargetClient());
        return loginUser;
    }

    private List<String> getUserTokensByClient(Long userId, String userClient) {
        String resolvedClient = StrUtil.blankToDefault(userClient, DEFAULT_USER_CLIENT);
        return onlineUserService.getUserTokens(userId).stream()
                .filter(token -> isTokenBelongsToClient(token, resolvedClient))
                .toList();
    }

    private SysTenant selectLoginTenantConfig(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return TenantContextHolder.executeIgnore(() ->
                tenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getId, tenantId)
                        .eq(SysTenant::getTenantStatus, 1)
                        .last("LIMIT 1")));
    }

    private boolean isTokenBelongsToClient(String token, String userClient) {
        try {
            SaSession tokenSession = StpUtil.getTokenSessionByToken(token);
            Object loginUserObj = tokenSession == null ? null : tokenSession.get("loginUser");
            if (!(loginUserObj instanceof LoginUser loginUser)) {
                return false;
            }
            return resolvedClientEquals(loginUser.getUserClient(), userClient);
        } catch (Exception e) {
            log.warn("过滤客户端会话失败: client={}", userClient, e);
            return false;
        }
    }

    private boolean resolvedClientEquals(String actualClient, String expectedClient) {
        return StrUtil.blankToDefault(actualClient, DEFAULT_USER_CLIENT)
                .equals(StrUtil.blankToDefault(expectedClient, DEFAULT_USER_CLIENT));
    }

    private void kickoutUserTokens(List<String> tokenValues) {
        if (CollUtil.isEmpty(tokenValues)) {
            return;
        }
        tokenValues.forEach(onlineUserService::kickoutUser);
    }

    private void executeWithRequiredTenant(Long tenantId, Runnable action) {
        Boolean previousIgnore = TenantContextHolder.getIgnoreValue();
        try {
            TenantContextHolder.setIgnore(false);
            TenantContextHolder.executeWithTenant(tenantId, action);
        } finally {
            if (previousIgnore == null) {
                TenantContextHolder.clearIgnore();
            } else {
                TenantContextHolder.setIgnore(previousIgnore);
            }
        }
    }

    @Override
    public LoginUser refreshLoginUser(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            return loginUser;
        }
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user != null) {
            loginUser.setUsername(user.getUsername());
            loginUser.setRealName(user.getRealName());
            loginUser.setPhone(user.getPhone());
            loginUser.setEmail(user.getEmail());
            loginUser.setAvatar(user.getAvatar());
            loginUser.setCreateTime(user.getCreateTime());
        }
        return loginUser;
    }
}
