package com.mdframe.forge.plugin.ai.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 模型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelService extends ServiceImpl<AiModelMapper, AiModel> {

    private final AiProviderService providerService;
    private final ObjectMapper objectMapper;

    /**
     * 新增模型 + 双写同步
     */
    @Transactional(rollbackFor = Exception.class)
    public void addModel(AiModel model) {
        // 校验同一供应商下 modelId 唯一
        long count = count(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, model.getProviderId())
                .eq(AiModel::getModelId, model.getModelId()));
        if (count > 0) {
            throw new RuntimeException("同一供应商下模型标识已存在: " + model.getModelId());
        }

        // 如果设为默认模型，先清除该供应商下其他默认
        if ("1".equals(model.getIsDefault())) {
            update(new LambdaUpdateWrapper<AiModel>()
                    .set(AiModel::getIsDefault, "0")
                    .eq(AiModel::getProviderId, model.getProviderId())
                    .eq(AiModel::getIsDefault, "1"));
        }

        save(model);
        syncModelsToProvider(model.getProviderId());
        log.info("[AI模型] 新增模型, providerId={}, modelId={}", model.getProviderId(), model.getModelId());
    }

    /**
     * 修改模型 + 双写同步
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateModel(AiModel model) {
        AiModel existing = getById(model.getId());
        if (existing == null) {
            throw new RuntimeException("模型不存在: " + model.getId());
        }

        // 如果修改了 modelId，校验同一供应商下唯一
        if (model.getModelId() != null && !model.getModelId().equals(existing.getModelId())) {
            long count = count(new LambdaQueryWrapper<AiModel>()
                    .eq(AiModel::getProviderId, existing.getProviderId())
                    .eq(AiModel::getModelId, model.getModelId()));
            if (count > 0) {
                throw new RuntimeException("同一供应商下模型标识已存在: " + model.getModelId());
            }
        }

        // 如果设为默认模型，先清除该供应商下其他默认
        if ("1".equals(model.getIsDefault())) {
            update(new LambdaUpdateWrapper<AiModel>()
                    .set(AiModel::getIsDefault, "0")
                    .eq(AiModel::getProviderId, existing.getProviderId())
                    .eq(AiModel::getIsDefault, "1"));
        }

        updateById(model);
        syncModelsToProvider(existing.getProviderId());
        log.info("[AI模型] 修改模型, id={}", model.getId());
    }

    /**
     * 删除模型 + 双写同步
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        AiModel existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("模型不存在: " + id);
        }

        removeById(id);
        syncModelsToProvider(existing.getProviderId());
        log.info("[AI模型] 删除模型, id={}, providerId={}, modelId={}", id, existing.getProviderId(), existing.getModelId());
    }

    /**
     * 按供应商查询模型列表
     */
    public List<AiModel> listByProviderId(Long providerId) {
        return list(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, providerId)
                .eq(AiModel::getStatus, "0")
                .orderByAsc(AiModel::getSortOrder));
    }

    /**
     * 统计供应商下的模型数量
     */
    public long countByProviderId(Long providerId) {
        return count(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, providerId));
    }

    /**
     * 双写同步：将 ai_model 表数据聚合回写至 ai_provider.models 和 ai_provider.default_model
     */
    private void syncModelsToProvider(Long providerId) {
        AiProvider provider = providerService.getById(providerId);
        if (provider == null) {
            log.warn("[AI模型同步] 供应商不存在, providerId={}", providerId);
            return;
        }

        List<AiModel> models = list(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, providerId)
                .orderByAsc(AiModel::getSortOrder));

        // 聚合 modelId 列表为 JSON 数组
        List<String> modelIdList = models.stream()
                .map(AiModel::getModelId)
                .collect(Collectors.toList());

        String modelsJson;
        try {
            modelsJson = objectMapper.writeValueAsString(modelIdList);
        } catch (JsonProcessingException e) {
            log.error("[AI模型同步] JSON序列化失败, providerId={}", providerId, e);
            modelsJson = "[]";
        }

        // 查找默认模型
        String defaultModel = models.stream()
                .filter(m -> "1".equals(m.getIsDefault()))
                .map(AiModel::getModelId)
                .findFirst()
                .orElse(null);

        // 回写 ai_provider
        providerService.update(new LambdaUpdateWrapper<AiProvider>()
                .set(AiProvider::getModels, modelsJson)
                .set(AiProvider::getDefaultModel, defaultModel)
                .eq(AiProvider::getId, providerId));

        log.info("[AI模型同步] 已同步, providerId={}, modelCount={}, defaultModel={}",
                providerId, modelIdList.size(), defaultModel);
    }
}
