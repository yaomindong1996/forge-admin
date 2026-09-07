package com.mdframe.forge.starter.datascope.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据权限配置Mapper
 */
@Mapper
public interface SysDataScopeConfigMapper extends BaseMapper<SysDataScopeConfig> {

    /**
     * 查询未删除规则，包含显式禁用配置，避免被误判为未配置。
     */
    List<SysDataScopeConfig> selectRuntimeConfigs();

    Page<SysDataScopeConfig> selectConfigPage(Page<SysDataScopeConfig> page,
                                             @Param("query") SysDataScopeConfig query);

    List<SysDataScopeConfig> selectConfigList(@Param("query") SysDataScopeConfig query);

    int updateConfigStatus(@Param("id") Long id, @Param("enabled") Integer enabled,
                           @Param("expectedEnabled") Integer expectedEnabled,
                           @Param("updateBy") Long updateBy);

    /**
     * 查询租户下已启用且已归属业务模块的数据权限规则。
     */
    List<SysDataScopeConfig> selectEnabledModuleConfigs(@Param("tenantId") Long tenantId);
}
