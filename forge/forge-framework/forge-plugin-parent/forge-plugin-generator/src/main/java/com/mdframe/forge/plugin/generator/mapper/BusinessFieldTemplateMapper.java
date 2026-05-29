package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFieldTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessFieldTemplateMapper extends BaseMapper<AiBusinessFieldTemplate> {

    List<AiBusinessFieldTemplate> selectEnabledTemplates(@Param("tenantId") Long tenantId,
                                                         @Param("suiteCode") String suiteCode);

    AiBusinessFieldTemplate selectByTemplateCode(@Param("tenantId") Long tenantId,
                                                 @Param("templateCode") String templateCode);
}
