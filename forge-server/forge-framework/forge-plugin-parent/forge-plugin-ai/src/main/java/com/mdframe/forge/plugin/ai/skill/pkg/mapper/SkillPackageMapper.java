package com.mdframe.forge.plugin.ai.skill.pkg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.skill.pkg.domain.SkillPackage;
import com.mdframe.forge.plugin.ai.skill.pkg.domain.SkillPackageFile;
import com.mdframe.forge.plugin.ai.skill.pkg.domain.SkillPackageVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkillPackageMapper extends BaseMapper<SkillPackage> {

    SkillPackage selectCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    Page<SkillPackageVersion> selectPage(Page<SkillPackageVersion> page,
                                         @Param("tenantId") Long tenantId,
                                         @Param("keyword") String keyword,
                                         @Param("status") String status);

    List<SkillPackageVersion> selectVersions(@Param("tenantId") Long tenantId,
                                             @Param("packageId") Long packageId);

    SkillPackageVersion selectVersion(@Param("tenantId") Long tenantId, @Param("id") Long id);

    SkillPackageVersion selectExact(@Param("tenantId") Long tenantId,
                                    @Param("code") String code,
                                    @Param("version") String version);

    SkillPackageVersion selectExactCurrent(@Param("tenantId") Long tenantId,
                                           @Param("code") String code,
                                           @Param("version") String version);

    List<SkillPackageFile> selectFiles(@Param("tenantId") Long tenantId,
                                       @Param("versionId") Long versionId);

    int changeState(@Param("tenantId") Long tenantId,
                    @Param("id") Long id,
                    @Param("hash") String hash,
                    @Param("expected") String expected,
                    @Param("status") String status,
                    @Param("snapshot") String snapshot,
                    @Param("actor") Long actor);

    List<SkillPackageVersion> selectBoundByAgent(@Param("tenantId") Long tenantId,
                                                 @Param("agentId") Long agentId);

    int softDeleteAgentPackage(@Param("tenantId") Long tenantId,
                               @Param("agentId") Long agentId,
                               @Param("packageId") Long packageId,
                               @Param("actor") Long actor);

    int softDeleteAgentVersion(@Param("tenantId") Long tenantId,
                               @Param("agentId") Long agentId,
                               @Param("versionId") Long versionId,
                               @Param("actor") Long actor);

    int countBoundByAgent(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId);
}
