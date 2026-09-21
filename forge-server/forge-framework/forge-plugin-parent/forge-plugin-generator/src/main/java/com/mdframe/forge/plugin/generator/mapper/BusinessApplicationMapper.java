package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface BusinessApplicationMapper extends BaseMapper<AiBusinessApplication> {

    Page<BusinessApplicationVO> selectApplicationPage(Page<BusinessApplicationVO> page,
                                                       @Param("tenantId") Long tenantId,
                                                       @Param("query") BusinessApplicationQueryDTO query);

    List<BusinessApplicationVO> selectApplicationList(@Param("tenantId") Long tenantId,
                                                       @Param("query") BusinessApplicationQueryDTO query);

    List<BusinessApplicationVO> selectApplicationAccessList(@Param("tenantId") Long tenantId);

    /**
     * 查询已有发布版本的工作台候选，返回设计态分发配置和发布态快照供服务层分别判定。
     */
    List<BusinessApplicationVO> selectPublishedWorkbenchApplications(@Param("tenantId") Long tenantId);

    BusinessApplicationVO selectApplicationDetail(@Param("tenantId") Long tenantId,
                                                   @Param("id") Long id);

    BusinessApplicationVO selectApplicationPublishContext(@Param("tenantId") Long tenantId,
                                                           @Param("id") Long id);

    BusinessApplicationVO selectApplicationDetailByCode(@Param("tenantId") Long tenantId,
                                                         @Param("applicationCode") String applicationCode);

    BusinessApplicationVO selectApplicationDetailBySlug(@Param("tenantId") Long tenantId,
                                                         @Param("portalSlug") String portalSlug);

    BusinessApplicationVO selectApplicationDetailByCodeOrSlug(@Param("tenantId") Long tenantId,
                                                               @Param("identifier") String identifier);

    BusinessApplicationVO selectApplicationDetailByPublishedSlug(@Param("tenantId") Long tenantId,
                                                                  @Param("portalSlug") String portalSlug);

    AiBusinessApplication selectEntityById(@Param("tenantId") Long tenantId,
                                           @Param("id") Long id);

    /** 与应用发布提交/打印修改共享的事务行锁。 */
    AiBusinessApplication lockEntityById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    AiBusinessApplication selectEntityByCode(@Param("tenantId") Long tenantId,
                                             @Param("applicationCode") String applicationCode);

    Long countByApplicationCode(@Param("tenantId") Long tenantId,
                                @Param("applicationCode") String applicationCode,
                                @Param("excludeId") Long excludeId);

    Long countByPortalSlug(@Param("tenantId") Long tenantId,
                           @Param("portalSlug") String portalSlug,
                           @Param("excludeId") Long excludeId);

    Long countBySuiteCode(@Param("tenantId") Long tenantId,
                          @Param("suiteCode") String suiteCode);

    /**
     * 统计租户内启用且拥有应用门户基础权限的角色数量。
     */
    Long countActiveDistributionRoles(@Param("tenantId") Long tenantId,
                                      @Param("roleIds") List<Long> roleIds);

    Long countActiveTenantUsers(@Param("tenantId") Long tenantId,
                                @Param("userIds") List<Long> userIds);

    Long countActiveTenantRoles(@Param("tenantId") Long tenantId,
                                @Param("roleIds") List<Long> roleIds);

    Long countActiveTenantOrgs(@Param("tenantId") Long tenantId,
                               @Param("orgIds") List<Long> orgIds);

    int markChanged(@Param("tenantId") Long tenantId,
                    @Param("applicationId") Long applicationId);

    int markChangedByObjectId(@Param("tenantId") Long tenantId,
                              @Param("objectId") Long objectId);

    int markPublished(@Param("tenantId") Long tenantId,
                      @Param("applicationId") Long applicationId,
                      @Param("versionNo") Integer versionNo,
                      @Param("publishTime") LocalDateTime publishTime);
}
