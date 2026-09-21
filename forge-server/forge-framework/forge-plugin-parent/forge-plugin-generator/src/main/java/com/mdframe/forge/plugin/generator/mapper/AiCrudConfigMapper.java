package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeAppDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiCrudConfigMapper extends BaseMapper<AiCrudConfig> {

    /** 当前启停/删除守卫，只返回数量；历史打印配置仍来自不可变版本。 */
    long countActiveRuntimeConfig(@Param("tenantId") Long tenantId, @Param("configId") Long configId,
                                  @Param("objectCode") String objectCode);

    AiCrudConfig selectByConfigKey(@Param("tenantId") Long tenantId,
                                   @Param("configKey") String configKey);

    AiCrudConfig selectPublishedByObjectCode(@Param("tenantId") Long tenantId,
                                             @Param("objectCode") String objectCode);

    AiCrudConfig selectPublishedByObjectCodeOrConfigKey(@Param("tenantId") Long tenantId,
                                                        @Param("objectCodeOrConfigKey") String objectCodeOrConfigKey);

    AiCrudConfig selectRuntimeByObjectCodeOrConfigKey(@Param("tenantId") Long tenantId,
                                                      @Param("objectCodeOrConfigKey") String objectCodeOrConfigKey);

    Page<LowcodeAppDetailVO> selectLowcodePage(Page<LowcodeAppDetailVO> page,
                                               @Param("tenantId") Long tenantId,
                                               @Param("keyword") String keyword,
                                               @Param("publishStatus") String publishStatus,
                                               @Param("domainIds") List<Long> domainIds,
                                               @Param("domainCode") String domainCode,
                                               @Param("generalDomain") Boolean generalDomain);

    List<AiCrudConfig> selectPublishedLowcodeConfigs(@Param("tenantId") Long tenantId);

    List<AiCrudConfig> selectEncryptConfigCandidates(@Param("tenantId") Long tenantId,
                                                     @Param("configKeys") List<String> configKeys);

}
