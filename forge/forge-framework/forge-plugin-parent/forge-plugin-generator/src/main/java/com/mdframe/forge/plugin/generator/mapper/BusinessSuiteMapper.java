package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessSuite;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessSuiteQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessSuiteSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessSuiteVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessSuiteMapper extends BaseMapper<AiBusinessSuite> {

    Page<BusinessSuiteVO> selectSuitePage(Page<BusinessSuiteVO> page,
                                          @Param("tenantId") Long tenantId,
                                          @Param("query") BusinessSuiteQueryDTO query);

    List<BusinessSuiteVO> selectSuiteList(@Param("tenantId") Long tenantId,
                                          @Param("query") BusinessSuiteQueryDTO query);

    BusinessSuiteVO selectSuiteDetail(@Param("tenantId") Long tenantId,
                                      @Param("id") Long id);

    AiBusinessSuite selectBySuiteCode(@Param("tenantId") Long tenantId,
                                      @Param("suiteCode") String suiteCode);

    Long countBySuiteCode(@Param("tenantId") Long tenantId,
                          @Param("suiteCode") String suiteCode,
                          @Param("excludeId") Long excludeId);

    Long countObjectsBySuite(@Param("tenantId") Long tenantId,
                             @Param("suiteCode") String suiteCode);

    Long countAppsBySuite(@Param("tenantId") Long tenantId,
                          @Param("suiteCode") String suiteCode);

    List<BusinessSuiteSummaryVO> selectSuiteSummary(@Param("tenantId") Long tenantId);
}
