package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.entity.SysExcelColumnConfig;
import com.mdframe.forge.plugin.system.entity.SysExcelExportConfig;
import com.mdframe.forge.plugin.system.mapper.SysExcelColumnConfigMapper;
import com.mdframe.forge.plugin.system.mapper.SysExcelExportConfigMapper;
import com.mdframe.forge.plugin.system.service.ISysExcelExportConfigService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * Excel导出配置Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysExcelExportConfigServiceImpl extends ServiceImpl<SysExcelExportConfigMapper, SysExcelExportConfig>
        implements ISysExcelExportConfigService {

    private static final String CONFIG_TYPE_EXPORT = "EXPORT";
    private static final String CONFIG_TYPE_IMPORT = "IMPORT";
    private static final String CONFIG_TYPE_BOTH = "BOTH";
    
    private final SysExcelColumnConfigMapper columnConfigMapper;
    
    @Override
    public Page<SysExcelExportConfig> page(PageQuery query, SysExcelExportConfig condition) {
        Page<SysExcelExportConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        return this.baseMapper.selectConfigPage(page, condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(SysExcelExportConfig config) {
        normalizeAndValidate(config);
        if (this.baseMapper.countByConfigKey(config.getConfigKey()) > 0) {
            throw new RuntimeException("配置键已存在: " + config.getConfigKey());
        }
        this.save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(SysExcelExportConfig config) {
        if (config == null || config.getId() == null) {
            throw new RuntimeException("配置ID不能为空");
        }
        normalizeAndValidate(config);
        if (this.baseMapper.countByConfigKeyExcludeId(config.getConfigKey(), config.getId()) > 0) {
            throw new RuntimeException("配置键已存在: " + config.getConfigKey());
        }
        this.updateById(config);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysExcelExportConfig config = new SysExcelExportConfig();
        config.setId(id);
        config.setStatus(status);
        this.updateById(config);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysExcelExportConfig copyConfig(Long id, String newConfigKey) {
        if (StrUtil.isBlank(newConfigKey)) {
            throw new RuntimeException("新配置键不能为空");
        }
        // 查询原配置
        SysExcelExportConfig source = this.getById(id);
        if (source == null) {
            throw new RuntimeException("配置不存在");
        }
        
        // 检查新配置键是否已存在
        long count = this.baseMapper.countByConfigKey(newConfigKey);
        if (count > 0) {
            throw new RuntimeException("配置键已存在: " + newConfigKey);
        }
        
        // 复制主配置
        SysExcelExportConfig newConfig = new SysExcelExportConfig();
        BeanUtils.copyProperties(source, newConfig);
        newConfig.setId(null);
        newConfig.setConfigKey(newConfigKey);
        newConfig.setExportName(source.getExportName() + "_副本");
        newConfig.setStatus(EnableStatus.DISABLED.getCode()); // 默认禁用
        this.save(newConfig);
        
        // 复制列配置
        List<SysExcelColumnConfig> columnConfigs = columnConfigMapper.selectByConfigKey(source.getConfigKey());
        
        for (SysExcelColumnConfig columnConfig : columnConfigs) {
            SysExcelColumnConfig newColumn = new SysExcelColumnConfig();
            BeanUtils.copyProperties(columnConfig, newColumn);
            newColumn.setId(null);
            newColumn.setConfigKey(newConfigKey);
            columnConfigMapper.insert(newColumn);
        }
        
        return newConfig;
    }

    private void normalizeAndValidate(SysExcelExportConfig config) {
        if (config == null) {
            throw new RuntimeException("配置不能为空");
        }
        if (StrUtil.isBlank(config.getConfigKey())) {
            throw new RuntimeException("配置键不能为空");
        }
        if (StrUtil.isBlank(config.getExportName())) {
            throw new RuntimeException("配置名称不能为空");
        }
        config.setConfigKey(StrUtil.trim(config.getConfigKey()));
        config.setExportName(StrUtil.trim(config.getExportName()));
        config.setSheetName(trimToNull(config.getSheetName()));
        config.setFileNameTemplate(trimToNull(config.getFileNameTemplate()));
        config.setDataSourceBean(trimToNull(config.getDataSourceBean()));
        config.setQueryMethod(trimToNull(config.getQueryMethod()));
        config.setSortField(trimToNull(config.getSortField()));
        config.setSortOrder(trimToNull(config.getSortOrder()));
        config.setRemark(trimToNull(config.getRemark()));

        String configType = StrUtil.blankToDefault(config.getConfigType(), CONFIG_TYPE_BOTH).toUpperCase();
        if (!CONFIG_TYPE_EXPORT.equals(configType)
                && !CONFIG_TYPE_IMPORT.equals(configType)
                && !CONFIG_TYPE_BOTH.equals(configType)) {
            throw new RuntimeException("配置类型不正确: " + configType);
        }
        config.setConfigType(configType);

        boolean exportEnabled = CONFIG_TYPE_EXPORT.equals(configType) || CONFIG_TYPE_BOTH.equals(configType);
        boolean importEnabled = CONFIG_TYPE_IMPORT.equals(configType) || CONFIG_TYPE_BOTH.equals(configType);

        config.setAllowImport(importEnabled);
        if (config.getStatus() == null) {
            config.setStatus(EnableStatus.ENABLED.getCode());
        }
        if (config.getAutoTrans() == null) {
            config.setAutoTrans(true);
        }
        if (config.getPageable() == null) {
            config.setPageable(false);
        }
        if (config.getIncludeSample() == null) {
            config.setIncludeSample(false);
        }

        if (exportEnabled) {
            if (StrUtil.isBlank(config.getDataSourceBean())) {
                throw new RuntimeException("导出配置必须填写数据源Bean");
            }
            if (StrUtil.isBlank(config.getQueryMethod())) {
                throw new RuntimeException("导出配置必须填写查询方法");
            }
            if (config.getMaxRows() == null || config.getMaxRows() <= 0) {
                config.setMaxRows(10000);
            }
        } else {
            config.setDataSourceBean(null);
            config.setQueryMethod(null);
            config.setPageable(false);
            config.setMaxRows(null);
            config.setSortField(null);
            config.setSortOrder(null);
        }
    }

    private String trimToNull(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }
}
