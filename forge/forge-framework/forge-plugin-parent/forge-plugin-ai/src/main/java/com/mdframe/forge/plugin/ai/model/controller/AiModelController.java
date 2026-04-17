package com.mdframe.forge.plugin.ai.model.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
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
                .eq(AiModel::getStatus, "0")
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
        return RespInfo.success();
    }

    /**
     * 修改模型
     */
    @PutMapping
    public RespInfo<Void> update(@RequestBody AiModel model) {
        modelService.updateModel(model);
        return RespInfo.success();
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        modelService.deleteModel(id);
        return RespInfo.success();
    }
}
