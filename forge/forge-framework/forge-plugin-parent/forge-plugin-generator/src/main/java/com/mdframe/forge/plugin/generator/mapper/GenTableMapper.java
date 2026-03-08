package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 代码生成表配置Mapper接口
 */
@Mapper
public interface GenTableMapper extends BaseMapper<GenTable> {

    /**
     * 查询数据库表列表
     */
    @Select("SELECT table_name, table_comment, create_time, update_time " +
            "FROM information_schema.tables " +
            "WHERE table_schema = (SELECT DATABASE()) " +
            "AND table_name NOT LIKE 'qrtz_%' AND table_name NOT LIKE 'gen_%' " +
            "ORDER BY create_time DESC")
    List<GenTable> selectDbTableList();

    /**
     * 根据表名查询数据库表信息
     */
    @Select("SELECT table_name, table_comment, create_time, update_time " +
            "FROM information_schema.tables " +
            "WHERE table_schema = (SELECT DATABASE()) " +
            "AND table_name = #{tableName}")
    GenTable selectDbTableByName(@Param("tableName") String tableName);
}
