package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessBindingMapper extends BaseMapper<AiBusinessBinding> {

    List<BusinessBindingVO> selectBindingList(@Param("tenantId") Long tenantId,
                                              @Param("query") BusinessBindingQueryDTO query);

    AiBusinessBinding selectBindingById(@Param("tenantId") Long tenantId,
                                        @Param("id") Long id);

    Long countByScope(@Param("tenantId") Long tenantId,
                      @Param("targetType") String targetType,
                      @Param("targetCode") String targetCode,
                      @Param("bindingType") String bindingType,
                      @Param("bindingKey") String bindingKey,
                      @Param("excludeId") Long excludeId);

    void deleteByBatchScope(@Param("tenantId") Long tenantId,
                            @Param("targetType") String targetType,
                            @Param("targetCode") String targetCode,
                            @Param("bindingTypes") List<String> bindingTypes);
}
