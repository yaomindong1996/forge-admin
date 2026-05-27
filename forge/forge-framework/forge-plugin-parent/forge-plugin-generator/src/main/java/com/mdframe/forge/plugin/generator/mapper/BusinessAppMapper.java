package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessAppMapper extends BaseMapper<AiBusinessApp> {

    Page<BusinessAppVO> selectAppPage(Page<BusinessAppVO> page,
                                      @Param("tenantId") Long tenantId,
                                      @Param("query") BusinessAppQueryDTO query);

    List<BusinessAppVO> selectAppList(@Param("tenantId") Long tenantId,
                                      @Param("query") BusinessAppQueryDTO query);

    BusinessAppVO selectAppDetail(@Param("tenantId") Long tenantId,
                                  @Param("id") Long id);

    AiBusinessApp selectEntityById(@Param("tenantId") Long tenantId,
                                   @Param("id") Long id);

    AiBusinessApp selectByAppCode(@Param("tenantId") Long tenantId,
                                  @Param("appCode") String appCode);

    AiBusinessApp selectByConfigKey(@Param("tenantId") Long tenantId,
                                    @Param("configKey") String configKey);

    AiBusinessApp selectRuntimeAppByObject(@Param("tenantId") Long tenantId,
                                           @Param("suiteCode") String suiteCode,
                                           @Param("objectCode") String objectCode);

    Long countByAppCode(@Param("tenantId") Long tenantId,
                        @Param("appCode") String appCode,
                        @Param("excludeId") Long excludeId);
}
