package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessDocumentConfigMapper extends BaseMapper<AiBusinessDocumentConfig> {

    AiBusinessDocumentConfig selectByObjectCode(@Param("tenantId") Long tenantId,
                                                @Param("objectCode") String objectCode);

    AiBusinessDocumentConfig selectByObjectId(@Param("tenantId") Long tenantId,
                                              @Param("objectId") Long objectId);
}
