package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.DataScopeConfigStatusDTO;
import com.mdframe.forge.plugin.system.service.ISysDataScopeConfigService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.List;

/**
 * 数据权限配置服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysDataScopeConfigServiceImpl implements ISysDataScopeConfigService {

    private final SysDataScopeConfigMapper dataScopeConfigMapper;
    private final IDataScopeService dataScopeService;

    @Override
    public Page<SysDataScopeConfig> selectConfigPage(PageQuery pageQuery, SysDataScopeConfig query) {
        return dataScopeConfigMapper.selectConfigPage(pageQuery.toPage(), query);
    }

    @Override
    public List<SysDataScopeConfig> selectConfigList(SysDataScopeConfig query) {
        return dataScopeConfigMapper.selectConfigList(query);
    }

    @Override
    public SysDataScopeConfig selectConfigById(Long id) {
        return dataScopeConfigMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertConfig(SysDataScopeConfig config) {
        config.setId(null);
        config.setEnabled(EnableStatus.ENABLED.getCode());
        int result = dataScopeConfigMapper.insert(config);
        if (result > 0) {
            // 刷新数据权限配置缓存
            refreshAfterCommit();
        }
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(SysDataScopeConfig config) {
        if (config.getId() == null) {
            throw new BusinessException("规则 ID 不能为空");
        }
        config.setEnabled(null);
        int result = dataScopeConfigMapper.updateById(config);
        if (result > 0) {
            // 刷新数据权限配置缓存
            refreshAfterCommit();
        }
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfigById(Long id) {
        int result = dataScopeConfigMapper.deleteById(id);
        if (result > 0) {
            // 刷新数据权限配置缓存
            refreshAfterCommit();
        }
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfigByIds(Long[] ids) {
        int result = dataScopeConfigMapper.deleteBatchIds(Arrays.asList(ids));
        if (result > 0) {
            // 刷新数据权限配置缓存
            refreshAfterCommit();
        }
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigStatus(DataScopeConfigStatusDTO dto) {
        EnableStatus target = requireStatus(dto.getEnabled());
        EnableStatus expected = requireStatus(dto.getExpectedEnabled());
        if (dto.getId() == null || target == expected) {
            throw new BusinessException("请选择有效的规则状态变更");
        }
        int changed = dataScopeConfigMapper.updateConfigStatus(dto.getId(), target.getCode(),
                expected.getCode(), SessionHelper.getUserId());
        if (changed == 0) {
            throw new BusinessException("规则已被修改或删除，请刷新列表后重试");
        }
        refreshAfterCommit();
    }

    private EnableStatus requireStatus(Integer value) {
        for (EnableStatus status : EnableStatus.values()) {
            if (status.matches(value)) {
                return status;
            }
        }
        throw new BusinessException("不支持的规则状态");
    }

    private void refreshAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refreshCommittedConfig();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refreshCommittedConfig();
            }
        });
    }

    private void refreshCommittedConfig() {
        try {
            dataScopeService.refreshDataScopeCache();
        } catch (Exception exception) {
            log.error("配置已保存，但数据权限刷新失败", exception);
            throw new BusinessException("配置已保存，但数据权限刷新失败，请点击「刷新数据权限」重试", exception);
        }
    }
}
