package com.mdframe.forge.starter.datascope.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.datascope.entity.SysRoleDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 角色-自定义数据权限Mapper
 */
@Mapper
public interface SysRoleDataScopeMapper extends BaseMapper<SysRoleDataScope> {
    
    /**
     * 根据角色ID列表查询自定义数据权限的组织ID集合
     */
    @Select("<script>" +
            "SELECT DISTINCT org_id FROM sys_role_data_scope " +
            "WHERE role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach>" +
            "</script>")
    Set<Long> selectOrgIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
