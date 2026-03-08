package com.mdframe.forge.starter.datascope.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 组织相关Mapper（用于数据权限）
 */
@Mapper
public interface DataScopeOrgMapper {
    
    /**
     * 根据组织ID列表查询所有子孙组织ID
     * 通过ancestors字段实现（例如ancestors='1,2,3'表示祖级组织链）
     */
    @Select("<script>" +
            "SELECT id FROM sys_org " +
            "WHERE " +
            "<foreach collection='orgIds' item='orgId' open='(' separator=' OR ' close=')'>" +
            "FIND_IN_SET(#{orgId}, ancestors) > 0 OR id = #{orgId}" +
            "</foreach>" +
            "</script>")
    Set<Long> selectChildOrgIds(@Param("orgIds") List<Long> orgIds);
}
