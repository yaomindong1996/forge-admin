package com.mdframe.forge.starter.datascope.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色相关Mapper（用于数据权限）
 */
@Mapper
public interface DataScopeRoleMapper {
    
    /**
     * 根据用户ID查询角色ID列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
    
    /**
     * 根据角色ID列表查询最小数据权限范围
     * data_scope值：1-全部，2-本人，3-本组织，4-本组织及子组织，5-自定义
     * 返回最小值（权限最大的）
     */
    @Select("<script>" +
            "SELECT MIN(data_scope) FROM sys_role " +
            "WHERE id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach>" +
            " AND role_status = 1" +
            "</script>")
    Integer selectMinDataScope(@Param("roleIds") List<Long> roleIds);
}
