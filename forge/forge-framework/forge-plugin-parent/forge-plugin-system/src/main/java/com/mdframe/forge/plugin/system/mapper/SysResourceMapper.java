package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资源Mapper接口
 */
@Mapper
public interface SysResourceMapper extends BaseMapper<SysResource> {

    /**
     * 根据角色ID列表和客户端代码查询资源列表
     */
    List<SysResource> selectUserResourcesByRoleIds(@Param("roleIds") List<Long> roleIds, @Param("clientCode") String clientCode);

    /**
     * 查询用户指定客户端下的导航资源列表（目录和菜单）
     */
    List<SysResource> selectUserNavigationResources(@Param("userId") Long userId, @Param("clientCode") String clientCode);

    /**
     * 查询用户指定客户端下的API权限列表
     */
    List<String> selectUserApiPermissions(@Param("userId") Long userId, @Param("clientCode") String clientCode);
}
