package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface BusinessApplicationMapper extends BaseMapper<AiBusinessApplication> {

    Page<BusinessApplicationVO> selectApplicationPage(Page<BusinessApplicationVO> page,
                                                       @Param("tenantId") Long tenantId,
                                                       @Param("query") BusinessApplicationQueryDTO query);

    List<BusinessApplicationVO> selectApplicationList(@Param("tenantId") Long tenantId,
                                                       @Param("query") BusinessApplicationQueryDTO query);

    BusinessApplicationVO selectApplicationDetail(@Param("tenantId") Long tenantId,
                                                   @Param("id") Long id);

    BusinessApplicationVO selectApplicationPublishContext(@Param("tenantId") Long tenantId,
                                                           @Param("id") Long id);

    BusinessApplicationVO selectApplicationDetailByCode(@Param("tenantId") Long tenantId,
                                                         @Param("applicationCode") String applicationCode);

    AiBusinessApplication selectEntityById(@Param("tenantId") Long tenantId,
                                           @Param("id") Long id);

    AiBusinessApplication selectEntityByCode(@Param("tenantId") Long tenantId,
                                             @Param("applicationCode") String applicationCode);

    Long countByApplicationCode(@Param("tenantId") Long tenantId,
                                @Param("applicationCode") String applicationCode,
                                @Param("excludeId") Long excludeId);

    Long countBySuiteCode(@Param("tenantId") Long tenantId,
                          @Param("suiteCode") String suiteCode);

    int markChanged(@Param("tenantId") Long tenantId,
                    @Param("applicationId") Long applicationId);

    int markChangedByObjectId(@Param("tenantId") Long tenantId,
                              @Param("objectId") Long objectId);

    int markPublished(@Param("tenantId") Long tenantId,
                      @Param("applicationId") Long applicationId,
                      @Param("versionNo") Integer versionNo,
                      @Param("publishTime") LocalDateTime publishTime);
}
