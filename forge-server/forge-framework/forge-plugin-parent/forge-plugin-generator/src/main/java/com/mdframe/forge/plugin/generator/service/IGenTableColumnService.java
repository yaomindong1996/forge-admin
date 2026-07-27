package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;

import java.util.List;

/**
 * 代码生成表字段配置Service接口
 */
public interface IGenTableColumnService extends IService<GenTableColumn> {

    /**
     * 查询当前数据库的表字段信息
     */
    List<GenTableColumn> selectDbTableColumnsByName(String tableName);

    /**
     * 查询已导入的表字段配置
     */
    List<GenTableColumn> selectTableColumns(Long tableId);

    /**
     * 批量更新字段配置并重排顺序
     */
    void batchUpdate(List<GenTableColumn> columns);

    /**
     * 重置字段配置
     */
    void resetConfig(Long tableId);
}
