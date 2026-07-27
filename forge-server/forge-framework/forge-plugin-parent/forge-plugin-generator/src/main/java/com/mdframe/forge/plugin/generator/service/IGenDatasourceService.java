package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.starter.core.domain.PageQuery;

import java.util.List;

/**
 * 代码生成器数据源配置Service接口
 */
public interface IGenDatasourceService extends IService<GenDatasource> {

    /**
     * 分页查询数据源配置
     */
    Page<GenDatasource> selectDatasourcePage(PageQuery pageQuery, String datasourceName, String usageScope);

    /**
     * 查询启用的数据源配置
     */
    List<GenDatasource> selectEnabledDatasources(String usageScope);

    /**
     * 测试数据源连接
     */
    boolean testConnection(Long datasourceId);

    /**
     * 查询指定数据源的表列表
     */
    List<GenTable> selectDbTableList(Long datasourceId);

    /**
     * 根据表名查询指定数据源的表信息
     */
    GenTable selectDbTableByName(Long datasourceId, String tableName);

    /**
     * 查询指定数据源表的字段信息
     */
    List<com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn> selectDbTableColumnsByName(Long datasourceId, String tableName);

    /**
     * 获取默认数据源
     */
    GenDatasource getDefaultDatasource();
}
