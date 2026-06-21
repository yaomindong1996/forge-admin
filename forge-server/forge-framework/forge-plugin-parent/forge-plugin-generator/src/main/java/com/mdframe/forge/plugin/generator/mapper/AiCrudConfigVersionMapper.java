package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiCrudConfigVersionMapper extends BaseMapper<AiCrudConfigVersion> {

    List<AiCrudConfigVersion> selectByConfigId(@Param("tenantId") Long tenantId,
                                               @Param("configId") Long configId);

    AiCrudConfigVersion selectVersionById(@Param("tenantId") Long tenantId,
                                          @Param("configId") Long configId,
                                          @Param("id") Long id);

    AiCrudConfigVersion selectVersionByNo(@Param("tenantId") Long tenantId,
                                          @Param("configId") Long configId,
                                          @Param("versionNo") Integer versionNo);

    Integer selectMaxVersionNo(@Param("tenantId") Long tenantId,
                               @Param("configId") Long configId);
}
