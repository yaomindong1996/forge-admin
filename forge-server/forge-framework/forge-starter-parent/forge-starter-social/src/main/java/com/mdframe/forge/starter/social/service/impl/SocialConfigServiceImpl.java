package com.mdframe.forge.starter.social.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.social.domain.dto.SocialPlatformInfo;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import com.mdframe.forge.starter.social.enums.SocialPlatform;
import com.mdframe.forge.starter.social.factory.SocialAuthRequestFactory;
import com.mdframe.forge.starter.social.mapper.SysSocialConfigMapper;
import com.mdframe.forge.starter.social.service.ISocialConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 三方登录配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialConfigServiceImpl extends ServiceImpl<SysSocialConfigMapper, SysSocialConfig>
        implements ISocialConfigService {

    private final SocialAuthRequestFactory authRequestFactory;

    @Autowired(required = false)
    private FileManager fileManager;

    @Override
    public Page<SysSocialConfig> selectConfigPage(Page<SysSocialConfig> page, SysSocialConfig query) {
        LambdaQueryWrapper<SysSocialConfig> wrapper = buildQueryWrapper(query);
        return this.page(page, wrapper);
    }

    @Override
    public List<SysSocialConfig> selectConfigList(SysSocialConfig query) {
        LambdaQueryWrapper<SysSocialConfig> wrapper = buildQueryWrapper(query);
        return this.list(wrapper);
    }

    @Override
    public SysSocialConfig selectConfigById(Long id) {
        return this.getById(id);
    }

    @Override
    public SysSocialConfig selectByPlatformAndTenant(String platform, Long tenantId) {
        List<SysSocialConfig> connections = baseMapper.selectEnabledByPlatform(platform, tenantId);
        if (connections.isEmpty()) {
            return null;
        }
        if (connections.size() > 1) {
            // 兼容期失败关闭：多连接歧义时禁止按平台猜测归属，必须按连接编码访问
            throw new BusinessException(StrUtil.format("平台[{}]存在{}个启用连接，请按连接编码访问", platform, connections.size()));
        }
        return connections.get(0);
    }

    @Override
    public SysSocialConfig selectConnectionByCode(String connectionCode) {
        if (StrUtil.isBlank(connectionCode)) {
            return null;
        }
        return baseMapper.selectConnectionByCode(null, connectionCode);
    }

    @Override
    public SysSocialConfig selectSsoWorkbenchConnection(String platform) {
        if (StrUtil.isBlank(platform)) {
            return null;
        }
        return baseMapper.selectSsoWorkbenchConnection(platform);
    }

    @Override
    public List<SocialPlatformInfo> selectEnabledPlatforms(Long tenantId) {
        List<SysSocialConfig> configs;
        if (tenantId == null) {
            configs = this.lambdaQuery()
                    .eq(SysSocialConfig::getStatus,"1").list();
        } else {
            configs = this.lambdaQuery()
                    .eq(SysSocialConfig::getStatus,"1")
                    .eq(SysSocialConfig::getTenantId,tenantId)
                    .list();
        }

        return configs.stream()
                .map(config -> {
                    String logoBase64 = null;
                    if (StrUtil.isNotBlank(config.getPlatformLogo()) && fileManager != null) {
                        try {
                            logoBase64 = fileManager.getFileContentBase64(config.getPlatformLogo());
                        } catch (Exception e) {
                            log.warn("读取平台Logo失败: platform={}, logo={}", config.getPlatform(), config.getPlatformLogo());
                        }
                    }
                    
                    return SocialPlatformInfo.builder()
                            .platform(config.getPlatform())
                            .connectionCode(config.getConnectionCode())
                            .connectionName(config.getConnectionName())
                            .platformName(config.getPlatformName())
                            .platformLogo(config.getPlatformLogo())
                            .platformLogoBase64(logoBase64)
                            .enabled(EnableStatus.ENABLED.matches(config.getStatus()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean insertConfig(SysSocialConfig config) {
        // platform 非空由 Controller @Valid + 实体注解保证
        sanitizeSecretEcho(config, null);
        if (StrUtil.isBlank(config.getConnectionName())) {
            config.setConnectionName(StrUtil.blankToDefault(config.getPlatformName(), config.getPlatform()));
        }
        boolean success = this.save(config);
        if (success) {
            // 连接编码缺省时用主键生成确定性编码，保证租户内唯一
            if (StrUtil.isBlank(config.getConnectionCode())) {
                SysSocialConfig codePatch = new SysSocialConfig();
                codePatch.setId(config.getId());
                codePatch.setConnectionCode(StrUtil.format("{}-{}", config.getPlatform().toLowerCase(), config.getId()));
                this.updateById(codePatch);
                config.setConnectionCode(codePatch.getConnectionCode());
            }
            authRequestFactory.clearCache(config);
        }
        return success;
    }

    @Override
    public boolean updateConfig(SysSocialConfig config) {
        SysSocialConfig current = config.getId() != null ? this.getById(config.getId()) : null;
        sanitizeSecretEcho(config, current);
        boolean success = this.updateById(config);
        if (success) {
            authRequestFactory.clearCache(config);
        }
        return success;
    }

    @Override
    public boolean deleteConfigById(Long id) {
        SysSocialConfig config = this.getById(id);
        boolean success = this.removeById(id);
        if (success && config != null) {
            authRequestFactory.clearCache(config);
        }
        return success;
    }

    @Override
    public boolean deleteConfigByIds(Long[] ids) {
        for (Long id : ids) {
            SysSocialConfig config = this.getById(id);
            if (config != null) {
                authRequestFactory.clearCache(config);
            }
        }
        return this.removeByIds(List.of(ids));
    }

    @Override
    public void refreshCache() {
        authRequestFactory.clearCache();
    }

    /**
     * 掩码回传保护：前端只拿到固定掩码，回传掩码/空值时保留原 Secret，禁止用掩码覆盖真实凭据
     */
    private void sanitizeSecretEcho(SysSocialConfig config, SysSocialConfig current) {
        String secret = config.getClientSecret();
        boolean maskEcho = StrUtil.isNotBlank(secret) && secret.chars().allMatch(c -> c == '*');
        if (StrUtil.isBlank(secret) || maskEcho) {
            config.setClientSecret(current != null ? current.getClientSecret() : null);
        }
    }

    private LambdaQueryWrapper<SysSocialConfig> buildQueryWrapper(SysSocialConfig query) {
        LambdaQueryWrapper<SysSocialConfig> wrapper = new LambdaQueryWrapper<>();

        if (ObjectUtil.isNotEmpty(query)) {
            if (ObjectUtil.isNotEmpty(query.getPlatform())) {
                wrapper.eq(SysSocialConfig::getPlatform, query.getPlatform());
            }
            if (ObjectUtil.isNotEmpty(query.getConnectionCode())) {
                wrapper.eq(SysSocialConfig::getConnectionCode, query.getConnectionCode());
            }
            if (ObjectUtil.isNotEmpty(query.getConnectionName())) {
                wrapper.like(SysSocialConfig::getConnectionName, query.getConnectionName());
            }
            if (ObjectUtil.isNotEmpty(query.getStatus())) {
                wrapper.eq(SysSocialConfig::getStatus, query.getStatus());
            }
            if (ObjectUtil.isNotEmpty(query.getTenantId())) {
                wrapper.eq(SysSocialConfig::getTenantId, query.getTenantId());
            }
        }

        wrapper.orderByDesc(SysSocialConfig::getCreateTime);
        return wrapper;
    }
}
