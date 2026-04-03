package com.mdframe.forge.starter.social.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
        return this.lambdaQuery()
                .eq(SysSocialConfig::getPlatform,platform)
                .eq(SysSocialConfig::getStatus,"1")
                .eq(tenantId != null,SysSocialConfig::getTenantId,tenantId).last("limit 1").one();
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
                            .platformName(config.getPlatformName())
                            .platformLogo(config.getPlatformLogo())
                            .platformLogoBase64(logoBase64)
                            .enabled(config.getStatus() == 1)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean insertConfig(SysSocialConfig config) {
        boolean success = this.save(config);
        if (success) {
            authRequestFactory.clearCache(config);
        }
        return success;
    }

    @Override
    public boolean updateConfig(SysSocialConfig config) {
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

    private LambdaQueryWrapper<SysSocialConfig> buildQueryWrapper(SysSocialConfig query) {
        LambdaQueryWrapper<SysSocialConfig> wrapper = new LambdaQueryWrapper<>();

        if (ObjectUtil.isNotEmpty(query)) {
            if (ObjectUtil.isNotEmpty(query.getPlatform())) {
                wrapper.eq(SysSocialConfig::getPlatform, query.getPlatform());
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
