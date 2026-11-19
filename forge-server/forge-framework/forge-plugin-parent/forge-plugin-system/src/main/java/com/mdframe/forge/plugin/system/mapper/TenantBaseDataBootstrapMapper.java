package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 新租户基础数据引导：只读查询与跨表 INSERT…SELECT 拷贝。
 * <p>调用方需在忽略租户行拦截的上下文中执行。
 */
@Mapper
public interface TenantBaseDataBootstrapMapper {

    long countActiveDictTypes(@Param("tenantId") Long tenantId);

    long countActiveConfigs(@Param("tenantId") Long tenantId);

    long countActiveOrgs(@Param("tenantId") Long tenantId);

    long countRolesByKey(@Param("tenantId") Long tenantId, @Param("roleKey") String roleKey);

    Long selectTemplateAdminRoleId(@Param("tenantId") Long tenantId);

    List<SysDictType> selectActiveDictTypes(@Param("tenantId") Long tenantId);

    List<SysDictData> selectActiveDictData(@Param("tenantId") Long tenantId);

    List<SysConfig> selectActiveConfigs(@Param("tenantId") Long tenantId);

    int copyRoleResourcesExcludingTenantMgmt(@Param("sourceTenantId") Long sourceTenantId,
                                             @Param("sourceRoleId") Long sourceRoleId,
                                             @Param("targetTenantId") Long targetTenantId,
                                             @Param("targetRoleId") Long targetRoleId);

    int copyMessageTemplates(@Param("sourceTenantId") Long sourceTenantId,
                             @Param("targetTenantId") Long targetTenantId);

    int copyMessageBizTypes(@Param("sourceTenantId") Long sourceTenantId,
                            @Param("targetTenantId") Long targetTenantId);

    int copyFlowCommentPhrases(@Param("sourceTenantId") Long sourceTenantId,
                               @Param("targetTenantId") Long targetTenantId);

    int copyFlowSpelTemplates(@Param("sourceTenantId") Long sourceTenantId,
                              @Param("targetTenantId") Long targetTenantId);

    int copyDataScopeConfigs(@Param("sourceTenantId") Long sourceTenantId,
                             @Param("targetTenantId") Long targetTenantId);
}
