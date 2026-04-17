package com.mdframe.forge.plugin.ai.model.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 模型管理接口
 */
@Slf4j
@RestController
@RequestMapping("/ai/model")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelService modelService;
    private final AiProviderService providerService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询模型列表
     */
    @GetMapping("/page")
    public RespInfo<Page<AiModel>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String modelName) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<AiModel>()
                .eq(providerId != null, AiModel::getProviderId, providerId)
                .eq(modelType != null && !modelType.isEmpty(), AiModel::getModelType, modelType)
                .like(modelName != null && !modelName.isEmpty(), AiModel::getModelName, modelName)
                .orderByAsc(AiModel::getSortOrder)
                .orderByDesc(AiModel::getCreateTime);
        return RespInfo.success(modelService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    /**
     * 按供应商查询所有模型（下拉选择用）
     */
    @GetMapping("/list")
    public RespInfo<List<AiModel>> list(@RequestParam(required = false) Long providerId) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getStatus, AiConstants.STATUS_NORMAL)
                .eq(providerId != null, AiModel::getProviderId, providerId)
                .orderByAsc(AiModel::getSortOrder);
        return RespInfo.success(modelService.list(wrapper));
    }

    /**
     * 查询模型详情
     */
    @GetMapping("/{id}")
    public RespInfo<AiModel> getById(@PathVariable Long id) {
        return RespInfo.success(modelService.getById(id));
    }

    /**
     * 新增模型
     */
    @PostMapping
    public RespInfo<Void> create(@RequestBody AiModel model) {
        modelService.addModel(model);
        syncModelsToProvider(model.getProviderId());
        return RespInfo.success();
    }

    /**
     * 修改模型
     */
    @PutMapping
    public RespInfo<Void> update(@RequestBody AiModel model) {
        AiModel existing = modelService.getById(model.getId());
        modelService.updateModel(model);
        // 使用 existing 的 providerId，因为模型可能切换了供应商
        Long providerId = existing != null ? existing.getProviderId() : model.getProviderId();
        // 如果修改了 providerId，需要同步新旧两个供应商
        if (model.getProviderId() != null && !model.getProviderId().equals(providerId)) {
            syncModelsToProvider(model.getProviderId());
        }
        syncModelsToProvider(providerId);
        return RespInfo.success();
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        AiModel existing = modelService.getById(id);
        modelService.deleteModel(id);
        if (existing != null) {
            syncModelsToProvider(existing.getProviderId());
        }
        return RespInfo.success();
    }

    /**
     * 双写同步：将 ai_model 表数据聚合回写至 ai_provider.models 和 ai_provider.default_model
     */
    private void syncModelsToProvider(Long providerId) {
        List<String> modelIdList = modelService.getModelIdListByProviderId(providerId);
        String defaultModel = modelService.getDefaultModelId(providerId);

        String modelsJson;
        try {
            modelsJson = objectMapper.writeValueAsString(modelIdList);
        } catch (JsonProcessingException e) {
            log.error("[AI模型同步] JSON序列化失败, providerId={}", providerId, e);
            modelsJson = "[]";
        }

        providerService.update(new LambdaUpdateWrapper<AiProvider>()
                .set(AiProvider::getModels, modelsJson)
                .set(AiProvider::getDefaultModel, defaultModel)
                .eq(AiProvider::getId, providerId));

        log.info("[AI模型同步] 已同步, providerId={}, modelCount={}, defaultModel={}",
                providerId, modelIdList.size(), defaultModel);
    }
}
