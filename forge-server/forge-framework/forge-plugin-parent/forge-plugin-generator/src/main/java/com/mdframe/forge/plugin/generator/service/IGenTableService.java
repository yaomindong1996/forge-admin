package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.starter.core.domain.PageQuery;

import java.util.List;
import java.util.Map;

/**
 * 代码生成Service接口
 */
public interface IGenTableService extends IService<GenTable> {

    /**
     * 查询数据库表列表
     */
    List<GenTable> selectDbTableList();

    /**
     * 分页查询已导入的表配置
     */
    Page<GenTable> selectGenTablePage(PageQuery pageQuery, String tableName, String tableComment);

    /**
     * 导入表结构
     */
    void importGenTable(List<String> tableNames);

    /**
     * 从指定数据源导入表结构
     */
    void importGenTable(Long datasourceId, List<String> tableNames);

    /**
     * 生成代码（返回字节流用于下载）
     */
    byte[] generatorCode(String tableName);

    /**
     * 批量生成代码
     */
    byte[] batchGeneratorCode(String[] tableNames);

    /**
     * 预览生成代码
     */
    Map<String, String> previewCode(String tableName);

    /**
     * 修改表配置
     */
    void updateGenTable(GenTable genTable);

    /**
     * 删除表配置
     */
    void deleteGenTableByIds(Long[] tableIds);
}
