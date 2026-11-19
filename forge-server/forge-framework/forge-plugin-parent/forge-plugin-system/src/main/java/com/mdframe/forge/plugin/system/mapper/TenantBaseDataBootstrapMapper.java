package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 新租户基础数据（字典/配置）从默认租户拷贝时的只读查询。
 * <p>SQL 显式带 {@code tenant_id}，调用方需在忽略租户行拦截的上下文中执行。
 */
@Mapper
public interface TenantBaseDataBootstrapMapper {

    long countActiveDictTypes(@Param("tenantId") Long tenantId);

    long countActiveConfigs(@Param("tenantId") Long tenantId);

    List<SysDictType> selectActiveDictTypes(@Param("tenantId") Long tenantId);

    List<SysDictData> selectActiveDictData(@Param("tenantId") Long tenantId);

    List<SysConfig> selectActiveConfigs(@Param("tenantId") Long tenantId);
}
