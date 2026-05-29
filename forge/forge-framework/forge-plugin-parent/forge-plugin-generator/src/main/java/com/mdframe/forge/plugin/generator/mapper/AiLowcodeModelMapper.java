package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiLowcodeModelMapper extends BaseMapper<AiLowcodeModel> {

    Page<AiLowcodeModel> selectModelPage(Page<AiLowcodeModel> page,
                                         @Param("tenantId") Long tenantId,
                                         @Param("domainId") Long domainId,
                                         @Param("keyword") String keyword,
                                         @Param("status") String status,
                                         @Param("masterData") Boolean masterData);

    List<AiLowcodeModel> selectModelList(@Param("tenantId") Long tenantId,
                                         @Param("domainId") Long domainId,
                                         @Param("keyword") String keyword,
                                         @Param("status") String status);

    AiLowcodeModel selectModelById(@Param("tenantId") Long tenantId,
                                   @Param("id") Long id);

    AiLowcodeModel selectByCode(@Param("tenantId") Long tenantId,
                                @Param("domainId") Long domainId,
                                @Param("modelCode") String modelCode);

    AiLowcodeModel selectByModelCode(@Param("tenantId") Long tenantId,
                                     @Param("modelCode") String modelCode);

    Long countByCode(@Param("tenantId") Long tenantId,
                     @Param("domainId") Long domainId,
                     @Param("modelCode") String modelCode,
                     @Param("excludeId") Long excludeId);
}
