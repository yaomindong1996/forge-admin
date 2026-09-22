package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessObjectMapper extends BaseMapper<AiBusinessObject> {

    Page<BusinessObjectVO> selectObjectPage(Page<BusinessObjectVO> page,
                                            @Param("tenantId") Long tenantId,
                                            @Param("query") BusinessObjectQueryDTO query);

    List<BusinessObjectVO> selectObjectList(@Param("tenantId") Long tenantId,
                                            @Param("query") BusinessObjectQueryDTO query);

    BusinessObjectVO selectObjectDetail(@Param("tenantId") Long tenantId,
                                        @Param("id") Long id);

    BusinessObjectVO selectObjectDetailByCode(@Param("tenantId") Long tenantId,
                                              @Param("suiteCode") String suiteCode,
                                              @Param("objectCode") String objectCode);

    AiBusinessObject selectByObjectCode(@Param("tenantId") Long tenantId,
                                        @Param("suiteCode") String suiteCode,
                                        @Param("objectCode") String objectCode);

    AiBusinessObject selectFirstByObjectCode(@Param("tenantId") Long tenantId,
                                             @Param("objectCode") String objectCode);

    AiBusinessObject selectByConfigKey(@Param("tenantId") Long tenantId,
                                       @Param("configKey") String configKey);

    AiBusinessObject selectByModelId(@Param("tenantId") Long tenantId,
                                     @Param("modelId") Long modelId);

    AiBusinessObject selectBySuiteAndModelCode(@Param("tenantId") Long tenantId,
                                               @Param("suiteCode") String suiteCode,
                                               @Param("modelCode") String modelCode);

    /**
     * 按租户读取未删除业务对象，避免只使用 MyBatis-Plus 的 selectById 绕过租户和逻辑删除条件。
     */
    AiBusinessObject selectByIdForTenant(@Param("tenantId") Long tenantId,
                                         @Param("id") Long id);

    /**
     * 对象编码是流程业务 Key 的全局身份，同一租户内不能跨业务套件重复。
     */
    Long countActiveByObjectCode(@Param("tenantId") Long tenantId,
                                 @Param("objectCode") String objectCode,
                                 @Param("excludeId") Long excludeId);

    int bindConfigKey(@Param("tenantId") Long tenantId,
                      @Param("objectId") Long objectId,
                      @Param("configKey") String configKey);

    Long countByObjectCode(@Param("tenantId") Long tenantId,
                           @Param("suiteCode") String suiteCode,
                           @Param("objectCode") String objectCode,
                           @Param("excludeId") Long excludeId);

    Long countRelationsByObject(@Param("tenantId") Long tenantId,
                                @Param("suiteCode") String suiteCode,
                                @Param("objectCode") String objectCode);

    Long countAppsByObject(@Param("tenantId") Long tenantId,
                           @Param("suiteCode") String suiteCode,
                           @Param("objectCode") String objectCode);

    Long countBindingsByObject(@Param("tenantId") Long tenantId,
                               @Param("suiteCode") String suiteCode,
                               @Param("objectCode") String objectCode);

    int updateAppSuiteByObject(@Param("tenantId") Long tenantId,
                               @Param("oldSuiteCode") String oldSuiteCode,
                               @Param("newSuiteCode") String newSuiteCode,
                               @Param("objectCode") String objectCode);

    int updateDesignVersionSuiteByObject(@Param("tenantId") Long tenantId,
                                         @Param("objectId") Long objectId,
                                         @Param("oldSuiteCode") String oldSuiteCode,
                                         @Param("newSuiteCode") String newSuiteCode);

    int updateTriggerSuiteByObject(@Param("tenantId") Long tenantId,
                                   @Param("oldSuiteCode") String oldSuiteCode,
                                   @Param("newSuiteCode") String newSuiteCode,
                                   @Param("objectCode") String objectCode);

    int updateDocumentConfigSuiteByObject(@Param("tenantId") Long tenantId,
                                          @Param("objectId") Long objectId,
                                          @Param("oldSuiteCode") String oldSuiteCode,
                                          @Param("newSuiteCode") String newSuiteCode,
                                          @Param("objectCode") String objectCode);

    List<AiBusinessObject> selectBySuiteCode(@Param("tenantId") Long tenantId,
                                             @Param("suiteCode") String suiteCode);

    /**
     * 查询已发布且启用的业务对象，供低代码查询源目录使用。
     */
    List<AiBusinessObject> selectPublishedObjects(@Param("tenantId") Long tenantId);

    /**
     * 软删页面表单托管对象独占的数据模型，释放 model_code 以便同编码重建。
     */
    int logicDeleteModelById(@Param("tenantId") Long tenantId,
                             @Param("modelId") Long modelId);
}
