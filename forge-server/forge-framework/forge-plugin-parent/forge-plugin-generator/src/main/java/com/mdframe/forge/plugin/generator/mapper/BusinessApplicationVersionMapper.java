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

    /** 锁定读全部保留快照，避免删除检查受之前一致性读快照影响。 */
    List<AiBusinessApplicationVersion> lockRetainedSnapshots(@Param("tenantId") Long tenantId,
                                                            @Param("applicationId") Long applicationId);

    /** 从当前不可变应用快照发现配置来源，不依赖可能已编辑的应用对象关联。 */
    List<AiBusinessApplicationVersion> selectPublishedPrintSources(@Param("tenantId") Long tenantId,
            @Param("configKey") String configKey, @Param("applicationId") Long applicationId);

    Integer selectMaxVersionNo(@Param("tenantId") Long tenantId,
                               @Param("applicationId") Long applicationId);
}
