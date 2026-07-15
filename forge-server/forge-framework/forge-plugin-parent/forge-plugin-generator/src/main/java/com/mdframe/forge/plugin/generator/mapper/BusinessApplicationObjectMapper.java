package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationObject;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessApplicationObjectMapper extends BaseMapper<AiBusinessApplicationObject> {

    List<BusinessApplicationObjectVO> selectByApplicationId(@Param("tenantId") Long tenantId,
                                                             @Param("applicationId") Long applicationId);

    Long countByApplicationId(@Param("tenantId") Long tenantId,
                              @Param("applicationId") Long applicationId);

    Long countByObjectId(@Param("tenantId") Long tenantId,
                         @Param("objectId") Long objectId);

    Long countByApplicationAndObjectCode(@Param("tenantId") Long tenantId,
                                         @Param("applicationId") Long applicationId,
                                         @Param("suiteCode") String suiteCode,
                                         @Param("objectCode") String objectCode);

    List<Long> selectApplicationIdsByObjectId(@Param("tenantId") Long tenantId,
                                              @Param("objectId") Long objectId);

    int logicDeleteByApplicationId(@Param("tenantId") Long tenantId,
                                   @Param("applicationId") Long applicationId);

    int insertBatch(@Param("items") List<AiBusinessApplicationObject> items);
}
