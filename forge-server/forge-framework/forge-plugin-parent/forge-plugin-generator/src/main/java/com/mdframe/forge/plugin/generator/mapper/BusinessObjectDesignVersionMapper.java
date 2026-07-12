package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessObjectDesignVersionMapper extends BaseMapper<AiBusinessObjectDesignVersion> {

    List<AiBusinessObjectDesignVersion> selectByObjectId(@Param("tenantId") Long tenantId,
                                                         @Param("objectId") Long objectId);

    AiBusinessObjectDesignVersion selectVersionById(@Param("tenantId") Long tenantId,
                                                    @Param("objectId") Long objectId,
                                                    @Param("id") Long id);

    Integer selectMaxVersionNo(@Param("tenantId") Long tenantId,
                               @Param("objectId") Long objectId);

    AiBusinessObjectDesignVersion selectPublishedVersion(@Param("tenantId") Long tenantId,
                                                         @Param("objectId") Long objectId,
                                                         @Param("publishVersion") Integer publishVersion);
}
