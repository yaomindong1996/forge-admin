package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.entity.SysEmailConfig;
import com.mdframe.forge.plugin.message.mapper.SysEmailConfigMapper;
import com.mdframe.forge.plugin.message.service.EmailConfigService;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConfigServiceImpl extends ServiceImpl<SysEmailConfigMapper, SysEmailConfig>
        implements EmailConfigService {

    @Override
    public SysEmailConfig getEnabledConfig(Long tenantId) {
        return baseMapper.selectEnabledConfig(MessageChannelConfigSupport.resolveTenantId(tenantId));
    }

    @Override
    public SysEmailConfig getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysEmailConfig config) {
        config.setTenantId(MessageChannelConfigSupport.resolveTenantId(config.getTenantId()));
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        if (config.getIsSsl() == null) {
            config.setIsSsl(true);
        }
        if (config.getIsAuth() == null) {
            config.setIsAuth(true);
        }
        if (config.getRetryInterval() == null) {
            config.setRetryInterval(5);
        }
        if (config.getMaxRetries() == null) {
            config.setMaxRetries(1);
        }
        if (config.getStatus() == null) {
            config.setStatus(EnableStatus.DISABLED.getCode());
        }
        return super.save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SysEmailConfig config) {
        preserveMaskedPassword(config);
        SysEmailConfig existing = config.getId() == null ? null : getById(config.getId());
        if (existing != null) {
            assertLastEnabledProtected(existing, config.getStatus());
        }
        config.setUpdateTime(LocalDateTime.now());
        return super.updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        SysEmailConfig existing = getById(id);
        if (existing == null) {
            throw new BusinessException("邮件配置不存在");
        }
        assertLastEnabledProtected(existing, status);
        SysEmailConfig config = new SysEmailConfig();
        config.setId(id);
        config.setStatus(status);
        config.setUpdateTime(LocalDateTime.now());
        return super.updateById(config);
    }

    private void preserveMaskedPassword(SysEmailConfig config) {
        if (config == null || config.getId() == null) {
            return;
        }
        if (!SensitiveDataUtil.isMaskedValue(config.getPassword())) {
            return;
        }
        SysEmailConfig existing = getById(config.getId());
        if (existing != null) {
            config.setPassword(existing.getPassword());
        }
    }

    private void assertLastEnabledProtected(SysEmailConfig existing, Integer nextStatus) {
        if (!MessageChannelConfigSupport.isDisabling(existing.getStatus(), nextStatus)) {
            return;
        }
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(existing.getTenantId());
        long otherEnabled = count(new LambdaQueryWrapper<SysEmailConfig>()
                .eq(SysEmailConfig::getTenantId, tenantId)
                .eq(SysEmailConfig::getStatus, EnableStatus.ENABLED.getCode())
                .ne(SysEmailConfig::getId, existing.getId()));
        MessageChannelConfigSupport.assertCanDisable(true, otherEnabled, "邮件");
    }
}
