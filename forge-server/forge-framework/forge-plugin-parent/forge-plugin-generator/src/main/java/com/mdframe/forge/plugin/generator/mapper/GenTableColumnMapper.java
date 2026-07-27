package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 代码生成表字段配置Mapper接口
 */
@Mapper
public interface GenTableColumnMapper extends BaseMapper<GenTableColumn> {

    /**
     * 根据表名查询数据库表字段信息
     */
    List<GenTableColumn> selectDbTableColumnsByName(@Param("tableName") String tableName);

    /**
     * 按表配置ID查询字段配置
     */
    List<GenTableColumn> selectByTableId(@Param("tableId") Long tableId);

    /**
     * 删除指定表配置的字段
     */
    int deleteByTableId(@Param("tableId") Long tableId);

    /**
     * 批量删除指定表配置的字段
     */
    int deleteByTableIds(@Param("tableIds") List<Long> tableIds);

    /**
     * 按数据模型字段配置同步表模型字段必填状态。
     */
    int updateRequiredByTableRef(@Param("tableId") Long tableId,
                                 @Param("tableName") String tableName,
                                 @Param("columns") List<Map<String, Object>> columns);
}
