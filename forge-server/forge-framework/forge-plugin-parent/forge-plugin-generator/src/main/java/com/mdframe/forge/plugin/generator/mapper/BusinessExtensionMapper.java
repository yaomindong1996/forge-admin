package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BusinessExtensionMapper extends BaseMapper<AiBusinessExtension> {

    Page<BusinessExtensionVO> selectExtensionPage(Page<BusinessExtensionVO> page,
                                                   @Param("tenantId") Long tenantId,
                                                   @Param("query") BusinessExtensionQueryDTO query);

    List<BusinessExtensionVO> selectExtensionList(@Param("tenantId") Long tenantId,
                                                   @Param("query") BusinessExtensionQueryDTO query);

    List<BusinessExtensionVO> selectWorkspaceSummaries(@Param("tenantId") Long tenantId,
                                                        @Param("applicationId") Long applicationId);

    BusinessExtensionVO selectExtensionDetail(@Param("tenantId") Long tenantId,
                                               @Param("id") Long id);

    AiBusinessExtension selectEntityById(@Param("tenantId") Long tenantId,
                                         @Param("id") Long id);

    AiBusinessApplication selectApplicationById(@Param("tenantId") Long tenantId,
                                                 @Param("applicationId") Long applicationId);

    Long countByExtensionCode(@Param("tenantId") Long tenantId,
                              @Param("applicationId") Long applicationId,
                              @Param("extensionCode") String extensionCode,
                              @Param("excludeId") Long excludeId);

    Long countApplicationObject(@Param("tenantId") Long tenantId,
                                @Param("applicationId") Long applicationId,
                                @Param("objectId") Long objectId);

    Long countApplicationEntry(@Param("tenantId") Long tenantId,
                               @Param("applicationId") Long applicationId,
                               @Param("entryId") Long entryId);

    int updateDraftVersion(@Param("tenantId") Long tenantId,
                           @Param("id") Long id,
                           @Param("draftVersion") Integer draftVersion,
                           @Param("status") String status);

    int updateLifecycle(@Param("tenantId") Long tenantId,
                        @Param("id") Long id,
                        @Param("status") String status,
                        @Param("enabledVersion") Integer enabledVersion);

    int tryAcquireLock(@Param("tenantId") Long tenantId,
                       @Param("id") Long id,
                       @Param("userId") Long userId,
                       @Param("username") String username,
                       @Param("lockToken") String lockToken,
                       @Param("now") LocalDateTime now,
                       @Param("expireTime") LocalDateTime expireTime);

    int renewLock(@Param("tenantId") Long tenantId,
                  @Param("id") Long id,
                  @Param("userId") Long userId,
                  @Param("lockToken") String lockToken,
                  @Param("now") LocalDateTime now,
                  @Param("expireTime") LocalDateTime expireTime);

    int releaseLock(@Param("tenantId") Long tenantId,
                    @Param("id") Long id,
                    @Param("userId") Long userId,
                    @Param("lockToken") String lockToken);

    Long countOwnedLock(@Param("tenantId") Long tenantId,
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("lockToken") String lockToken,
                        @Param("now") LocalDateTime now);

    List<AiBusinessExtension> selectEnabledForHook(@Param("tenantId") Long tenantId,
                                                    @Param("applicationId") Long applicationId,
                                                    @Param("objectId") Long objectId,
                                                    @Param("entryId") Long entryId,
                                                    @Param("hookCode") String hookCode);

    List<AiBusinessExtension> selectByApplicationId(@Param("tenantId") Long tenantId,
                                                    @Param("applicationId") Long applicationId);

    int restoreEnabledVersion(@Param("tenantId") Long tenantId,
                              @Param("applicationId") Long applicationId,
                              @Param("extensionId") Long extensionId,
                              @Param("status") String status,
                              @Param("enabledVersion") Integer enabledVersion);
}
