package com.mdframe.forge.plugin.generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.service.IGenTableColumnService;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 代码生成表字段配置Service实现类
 */
@Service
public class GenTableColumnServiceImpl
        extends ServiceImpl<GenTableColumnMapper, GenTableColumn>
        implements IGenTableColumnService {

    @Override
    public List<GenTableColumn> selectDbTableColumnsByName(String tableName) {
        return baseMapper.selectDbTableColumnsByName(tableName);
    }

    @Override
    public List<GenTableColumn> selectTableColumns(Long tableId) {
        return baseMapper.selectByTableId(tableId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<GenTableColumn> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new BusinessException("字段列表不能为空");
        }
        for (int i = 0; i < columns.size(); i++) {
            GenTableColumn column = columns.get(i);
            column.setSort(i);
            baseMapper.updateById(column);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetConfig(Long tableId) {
        for (GenTableColumn column : baseMapper.selectByTableId(tableId)) {
            GenUtils.initColumnField(column);
            baseMapper.updateById(column);
        }
    }
}
