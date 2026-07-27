package com.mdframe.forge.plugin.ai.model.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.coordination.AiModelProviderManager;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.dto.AiModelSaveDTO;
import com.mdframe.forge.plugin.ai.model.vo.AiModelVO;
import com.mdframe.forge.plugin.ai.health.AiModelConnectionTestService;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 模型管理接口
 */
@RestController
@RequestMapping("/ai/model")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class AiModelController {

    private final AiModelService modelService;
    private final AiModelProviderManager modelProviderManager;
    private final AiModelConnectionTestService connectionTestService;

    /**
     * 分页查询模型列表
     */
    @GetMapping("/page")
    public RespInfo<Page<AiModelVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String modelName) {
        Page<AiModel> modelPage = modelService.selectModelPage(
                pageNum, pageSize, providerId, modelType, modelName);
        Page<AiModelVO> result = new Page<>(modelPage.getCurrent(), modelPage.getSize(), modelPage.getTotal());
        result.setRecords(modelService.toViews(modelPage.getRecords()));
        return RespInfo.success(result);
    }

    /**
     * 按供应商查询所有模型（下拉选择用）
     */
    @GetMapping("/list")
    public RespInfo<List<AiModelVO>> list(@RequestParam(required = false) Long providerId) {
        return RespInfo.success(modelService.toViews(modelService.listEnabledModels(providerId)));
    }

    /**
     * 查询模型详情
     */
    @GetMapping("/{id}")
    public RespInfo<AiModelVO> getById(@PathVariable Long id) {
        AiModel model = modelService.getById(id);
        return RespInfo.success(model == null ? null : modelService.toView(model));
    }

    /**
     * 新增模型
     */
    @PostMapping
    public RespInfo<Void> create(@RequestBody AiModelSaveDTO model) {
        modelProviderManager.createModel(model);
        return RespInfo.success();
    }

    /**
     * 修改模型
     */
    @PutMapping
    public RespInfo<Void> update(@RequestBody AiModelSaveDTO model) {
        modelProviderManager.updateModel(model);
        return RespInfo.success();
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        modelProviderManager.deleteModel(id);
        return RespInfo.success();
    }

    @PostMapping("/{id}/test")
    @SaCheckPermission("ai:model:test")
    public RespInfo<String> test(@PathVariable Long id) {
        return RespInfo.success(connectionTestService.test(id));
    }

}
