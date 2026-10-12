package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.entity.SysExcelColumnConfig;
import com.mdframe.forge.plugin.system.entity.SysExcelExportConfig;
import com.mdframe.forge.plugin.system.mapper.SysExcelColumnConfigMapper;
import com.mdframe.forge.plugin.system.mapper.SysExcelExportConfigMapper;
import com.mdframe.forge.plugin.system.service.ISysExcelColumnConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Excel列配置Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysExcelColumnConfigServiceImpl extends ServiceImpl<SysExcelColumnConfigMapper, SysExcelColumnConfig>
        implements ISysExcelColumnConfigService {

    private static final String CONFIG_TYPE_EXPORT = "EXPORT";
    private static final String CONFIG_TYPE_IMPORT = "IMPORT";
    
    private final SysExcelExportConfigMapper exportConfigMapper;

    @Override
    public List<SysExcelColumnConfig> listByConfigKey(String configKey) {
        return this.baseMapper.selectByConfigKey(configKey);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(String configKey, List<SysExcelColumnConfig> columns) {
        // 先删除原有配置
        deleteByConfigKey(configKey);

        SysExcelExportConfig parentConfig = exportConfigMapper.selectByConfigKey(configKey);
        String configType = parentConfig != null ? StrUtil.blankToDefault(parentConfig.getConfigType(), "BOTH").toUpperCase() : "BOTH";
        boolean exportEnabled = !CONFIG_TYPE_IMPORT.equals(configType);
        boolean importEnabled = !CONFIG_TYPE_EXPORT.equals(configType);
        
        // 批量保存新配置
        if (columns != null && !columns.isEmpty()) {
            for (int index = 0; index < columns.size(); index++) {
                SysExcelColumnConfig column = columns.get(index);
                normalizeColumn(configKey, column, index + 1, exportEnabled, importEnabled);
            }
            this.saveBatch(columns);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByConfigKey(String configKey) {
        this.baseMapper.deleteByConfigKey(configKey);
    }

    private void normalizeColumn(String configKey,
                                 SysExcelColumnConfig column,
                                 int defaultOrderNum,
                                 boolean exportEnabled,
                                 boolean importEnabled) {
        if (column == null) {
            throw new RuntimeException("列配置不能为空");
        }
        if (StrUtil.isBlank(column.getFieldName())) {
            throw new RuntimeException("字段名不能为空");
        }
        if (StrUtil.isBlank(column.getColumnName())) {
            throw new RuntimeException("列名不能为空");
        }

        column.setConfigKey(configKey);
        column.setFieldName(StrUtil.trim(column.getFieldName()));
        column.setColumnName(StrUtil.trim(column.getColumnName()));
        column.setWidth(column.getWidth() == null || column.getWidth() <= 0 ? 20 : column.getWidth());
        column.setOrderNum(column.getOrderNum() == null || column.getOrderNum() < 0 ? defaultOrderNum : column.getOrderNum());
        column.setDateFormat(trimToNull(column.getDateFormat()));
        column.setNumberFormat(trimToNull(column.getNumberFormat()));
        column.setDictType(trimToNull(column.getDictType()));
        column.setExampleValue(trimToNull(column.getExampleValue()));
        column.setValidationRule(trimToNull(column.getValidationRule()));
        column.setValidationMessage(trimToNull(column.getValidationMessage()));

        column.setExport(exportEnabled && !Boolean.FALSE.equals(column.getExport()));
        if (importEnabled) {
            column.setImportable(!Boolean.FALSE.equals(column.getImportable()));
            column.setRequired(Boolean.TRUE.equals(column.getRequired()));
        } else {
            column.setImportable(false);
            column.setRequired(false);
            column.setExampleValue(null);
            column.setValidationRule(null);
            column.setValidationMessage(null);
        }
    }

    private String trimToNull(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }
}
