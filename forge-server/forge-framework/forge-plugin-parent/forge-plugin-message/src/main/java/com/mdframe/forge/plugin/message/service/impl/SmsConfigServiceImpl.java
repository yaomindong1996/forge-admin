package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsConfig;
import com.mdframe.forge.plugin.message.mapper.SysSmsConfigMapper;
import com.mdframe.forge.plugin.message.service.SmsConfigService;
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
public class SmsConfigServiceImpl extends ServiceImpl<SysSmsConfigMapper, SysSmsConfig>
        implements SmsConfigService {

    @Override
    public SysSmsConfig getEnabledConfig(Long tenantId) {
        return baseMapper.selectEnabledConfig(MessageChannelConfigSupport.resolveTenantId(tenantId));
    }

    @Override
    public SysSmsConfig getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysSmsConfig config) {
        config.setTenantId(MessageChannelConfigSupport.resolveTenantId(config.getTenantId()));
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        if (config.getWeight() == null) {
            config.setWeight(1);
        }
        if (config.getRetryInterval() == null) {
            config.setRetryInterval(5);
        }
        if (config.getMaxRetries() == null) {
            config.setMaxRetries(0);
        }
        if (config.getStatus() == null) {
            config.setStatus(EnableStatus.DISABLED.getCode());
        }
        return super.save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SysSmsConfig config) {
        preserveMaskedSecret(config);
        SysSmsConfig existing = config.getId() == null ? null : getById(config.getId());
        if (existing != null) {
            assertLastEnabledProtected(existing, config.getStatus());
        }
        config.setUpdateTime(LocalDateTime.now());
        return super.updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        SysSmsConfig existing = getById(id);
        if (existing == null) {
            throw new BusinessException("短信配置不存在");
        }
        assertLastEnabledProtected(existing, status);
        SysSmsConfig config = new SysSmsConfig();
        config.setId(id);
        config.setStatus(status);
        config.setUpdateTime(LocalDateTime.now());
        return super.updateById(config);
    }

    private void preserveMaskedSecret(SysSmsConfig config) {
        if (config == null || config.getId() == null) {
            return;
        }
        if (!SensitiveDataUtil.isMaskedValue(config.getAccessKeySecret())) {
            return;
        }
        SysSmsConfig existing = getById(config.getId());
        if (existing != null) {
            config.setAccessKeySecret(existing.getAccessKeySecret());
        }
    }

    private void assertLastEnabledProtected(SysSmsConfig existing, Integer nextStatus) {
        if (!MessageChannelConfigSupport.isDisabling(existing.getStatus(), nextStatus)) {
            return;
        }
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(existing.getTenantId());
        long otherEnabled = count(new LambdaQueryWrapper<SysSmsConfig>()
                .eq(SysSmsConfig::getTenantId, tenantId)
                .eq(SysSmsConfig::getStatus, EnableStatus.ENABLED.getCode())
                .ne(SysSmsConfig::getId, existing.getId()));
        MessageChannelConfigSupport.assertCanDisable(true, otherEnabled, "短信");
    }
}
