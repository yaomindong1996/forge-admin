package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessObjectRelationMapper extends BaseMapper<AiBusinessObjectRelation> {

    List<BusinessObjectRelationVO> selectRelationsByObject(@Param("tenantId") Long tenantId,
                                                           @Param("suiteCode") String suiteCode,
                                                           @Param("objectCode") String objectCode);

    List<AiBusinessObjectRelation> selectRuntimeRelationsBySource(@Param("tenantId") Long tenantId,
                                                                   @Param("suiteCode") String suiteCode,
                                                                   @Param("sourceObjectCode") String sourceObjectCode);

    AiBusinessObjectRelation selectRelationById(@Param("tenantId") Long tenantId,
                                                @Param("id") Long id);

    Long countByScope(@Param("tenantId") Long tenantId,
                      @Param("suiteCode") String suiteCode,
                      @Param("sourceObjectCode") String sourceObjectCode,
                      @Param("targetObjectCode") String targetObjectCode,
                      @Param("relationType") String relationType,
                      @Param("relationName") String relationName,
                      @Param("excludeId") Long excludeId);

    void deleteMissingRelations(@Param("tenantId") Long tenantId,
                                @Param("suiteCode") String suiteCode,
                                @Param("sourceObjectCode") String sourceObjectCode,
                                @Param("relationIds") List<Long> relationIds);
}
