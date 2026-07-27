package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代码生成表配置Mapper接口
 */
@Mapper
public interface GenTableMapper extends BaseMapper<GenTable> {

    /**
     * 查询数据库表列表
     */
    List<GenTable> selectDbTableList();

    /**
     * 根据表名查询数据库表信息
     */
    GenTable selectDbTableByName(@Param("tableName") String tableName);

    /**
     * 分页查询已导入的表配置
     */
    Page<GenTable> selectGenTablePage(Page<GenTable> page,
                                      @Param("tableName") String tableName,
                                      @Param("tableComment") String tableComment);

    /**
     * 按数据源和表名查询已导入配置
     */
    GenTable selectByDatasourceAndTableName(@Param("datasourceId") Long datasourceId,
                                            @Param("tableName") String tableName);

    /**
     * 按表名查询已导入配置
     */
    GenTable selectByTableName(@Param("tableName") String tableName);
}
