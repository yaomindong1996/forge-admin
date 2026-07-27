package com.mdframe.forge.plugin.ai.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 模型 Mapper
 */
@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {

    Page<AiModel> selectModelPage(Page<AiModel> page,
                                  @Param("providerId") Long providerId,
                                  @Param("modelType") String modelType,
                                  @Param("modelName") String modelName);

    List<AiModel> selectEnabledModels(@Param("providerId") Long providerId);

    List<AiModel> selectAllByProviderId(@Param("providerId") Long providerId);

    AiModel selectByIdForUpdate(@Param("id") Long id);

    long countByProviderId(@Param("providerId") Long providerId);

    long countByProviderAndModelId(@Param("providerId") Long providerId,
                                   @Param("modelId") String modelId,
                                   @Param("excludeId") Long excludeId);

    int clearDefaultByProviderId(@Param("providerId") Long providerId,
                                 @Param("excludeId") Long excludeId);

    /**
     * 查询供应商启用且标记为默认的模型标识。
     *
     * @param providerId 供应商 ID
     * @return 默认模型标识，不存在时返回 null
     */
    String selectEnabledDefaultModelId(@Param("providerId") Long providerId);

    AiModel selectEnabledByProviderAndModelId(@Param("providerId") Long providerId, @Param("modelId") String modelId);

    AiModel selectEnabledById(@Param("id") Long id);

    List<AiModel> selectEnabledByIds(@Param("ids") List<Long> ids);
}
