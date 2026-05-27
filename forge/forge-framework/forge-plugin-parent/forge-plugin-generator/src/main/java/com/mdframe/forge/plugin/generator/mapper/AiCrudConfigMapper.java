package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiCrudConfigMapper extends BaseMapper<AiCrudConfig> {

    AiCrudConfig selectByConfigKey(@Param("tenantId") Long tenantId,
                                   @Param("configKey") String configKey);

    Page<AiCrudConfig> selectLowcodePage(Page<AiCrudConfig> page,
                                         @Param("tenantId") Long tenantId,
                                         @Param("keyword") String keyword,
                                         @Param("publishStatus") String publishStatus,
                                         @Param("domainId") Long domainId,
                                         @Param("domainCode") String domainCode,
                                         @Param("generalDomain") Boolean generalDomain);

    List<AiCrudConfig> selectPublishedLowcodeConfigs(@Param("tenantId") Long tenantId);

}
