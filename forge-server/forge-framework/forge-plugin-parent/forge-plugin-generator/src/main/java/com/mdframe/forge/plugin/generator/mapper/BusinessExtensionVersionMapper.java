package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BusinessExtensionVersionMapper extends BaseMapper<AiBusinessExtensionVersion> {

    List<AiBusinessExtensionVersion> selectVersions(@Param("tenantId") Long tenantId,
                                                     @Param("extensionId") Long extensionId);

    AiBusinessExtensionVersion selectVersion(@Param("tenantId") Long tenantId,
                                              @Param("extensionId") Long extensionId,
                                              @Param("versionNo") Integer versionNo);

    List<AiBusinessExtensionVersion> selectReleaseVersions(
            @Param("tenantId") Long tenantId,
            @Param("releaseVersions") Map<Long, Integer> releaseVersions);

    Integer selectMaxVersionNo(@Param("tenantId") Long tenantId,
                               @Param("extensionId") Long extensionId);

    int updateValidationResult(@Param("tenantId") Long tenantId,
                               @Param("extensionId") Long extensionId,
                               @Param("versionNo") Integer versionNo,
                               @Param("passed") Integer passed,
                               @Param("summary") String summary);

    int updateTestResult(@Param("tenantId") Long tenantId,
                         @Param("extensionId") Long extensionId,
                         @Param("versionNo") Integer versionNo,
                         @Param("passed") Integer passed,
                         @Param("summary") String summary);
}
