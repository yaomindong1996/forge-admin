package com.mdframe.forge.starter.social.community;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.config.config.LoginConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import com.mdframe.forge.starter.social.enums.SocialPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Gitee 社区体验登录开关，读取统一配置中心 login 分组，保存后立即生效。
 */
@Component
@RequiredArgsConstructor
public class GiteeCommunityLoginSupport {

    private static final long DEFAULT_TENANT_ID = 9001L;
    private static final String DEFAULT_ROLE_KEY = "gitee_community";
    private static final String DEFAULT_OWNER = "ForgeLab";
    private static final String DEFAULT_REPO = "forge-admin";
    private static final String DEFAULT_REPO_URL = "https://gitee.com/ForgeLab/forge-admin";
    /** 单页 Star 列表请求超时；过短易在 Gitee 慢响应时误判失败 */
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final ConfigManagerService configManagerService;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(loginConfig().getGiteeCommunityEnabled());
    }

    public boolean appliesTo(SysSocialConfig connection) {
        if (!isEnabled() || connection == null) {
            return false;
        }
        return SocialPlatform.GITEE.getCode().equalsIgnoreCase(connection.getPlatform());
    }

    public boolean requireStar() {
        return isEnabled() && !Boolean.FALSE.equals(loginConfig().getGiteeCommunityRequireStar());
    }

    public Long communityTenantId() {
        Long tenantId = loginConfig().getGiteeCommunityTenantId();
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    public String roleKey() {
        return StrUtil.blankToDefault(loginConfig().getGiteeCommunityRoleKey(), DEFAULT_ROLE_KEY);
    }

    public GiteeCommunitySettings config() {
        LoginConfig loginConfig = loginConfig();
        return new GiteeCommunitySettings(
                isEnabled(),
                requireStar(),
                StrUtil.blankToDefault(loginConfig.getGiteeCommunityOwner(), DEFAULT_OWNER),
                StrUtil.blankToDefault(loginConfig.getGiteeCommunityRepo(), DEFAULT_REPO),
                StrUtil.blankToDefault(loginConfig.getGiteeCommunityRepoUrl(), DEFAULT_REPO_URL),
                communityTenantId(),
                roleKey(),
                loginConfig.getGiteeCommunityTimeoutMs() == null
                        ? DEFAULT_TIMEOUT_MS
                        : loginConfig.getGiteeCommunityTimeoutMs());
    }

    private LoginConfig loginConfig() {
        LoginConfig config = configManagerService.getLoginConfig();
        return config == null ? new LoginConfig() : config;
    }

    public record GiteeCommunitySettings(
            boolean enabled,
            boolean requireStar,
            String owner,
            String repo,
            String repoUrl,
            Long tenantId,
            String roleKey,
            int timeoutMs
    ) {
        public boolean isRequireStar() {
            return requireStar;
        }

        public String getOwner() {
            return owner;
        }

        public String getRepo() {
            return repo;
        }

        public String getRepoUrl() {
            return repoUrl;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }
    }
}
