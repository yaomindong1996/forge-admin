package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsConfig;
import com.mdframe.forge.plugin.message.mapper.SysSmsConfigMapper;
import com.mdframe.forge.plugin.message.service.SmsConfigService;
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
        return baseMapper.selectEnabledConfig(tenantId);
    }

    @Override
    public SysSmsConfig getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysSmsConfig config) {
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
            config.setStatus(0);
        }
        return super.save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SysSmsConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        return super.updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        SysSmsConfig config = new SysSmsConfig();
        config.setId(id);
        config.setStatus(status);
        config.setUpdateTime(LocalDateTime.now());
        return super.updateById(config);
    }
}
