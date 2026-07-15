package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessApplicationVersionMapper extends BaseMapper<AiBusinessApplicationVersion> {

    List<AiBusinessApplicationVersion> selectVersions(@Param("tenantId") Long tenantId,
                                                       @Param("applicationId") Long applicationId);

    AiBusinessApplicationVersion selectVersion(@Param("tenantId") Long tenantId,
                                                @Param("applicationId") Long applicationId,
                                                @Param("versionNo") Integer versionNo);

    AiBusinessApplicationVersion selectVersionById(@Param("tenantId") Long tenantId,
                                                    @Param("applicationId") Long applicationId,
                                                    @Param("versionId") Long versionId);

    Integer selectMaxVersionNo(@Param("tenantId") Long tenantId,
                               @Param("applicationId") Long applicationId);
}
