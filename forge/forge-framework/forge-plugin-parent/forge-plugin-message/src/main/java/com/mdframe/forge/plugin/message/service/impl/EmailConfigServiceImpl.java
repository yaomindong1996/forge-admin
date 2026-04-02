package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.entity.SysEmailConfig;
import com.mdframe.forge.plugin.message.mapper.SysEmailConfigMapper;
import com.mdframe.forge.plugin.message.service.EmailConfigService;
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
        return baseMapper.selectEnabledConfig(tenantId);
    }

    @Override
    public SysEmailConfig getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysEmailConfig config) {
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
            config.setStatus(0);
        }
        return super.save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SysEmailConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        return super.updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        SysEmailConfig config = new SysEmailConfig();
        config.setId(id);
        config.setStatus(status);
        config.setUpdateTime(LocalDateTime.now());
        return super.updateById(config);
    }
}
