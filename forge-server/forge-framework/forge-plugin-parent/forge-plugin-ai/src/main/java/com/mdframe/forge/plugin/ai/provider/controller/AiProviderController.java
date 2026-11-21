package com.mdframe.forge.plugin.ai.provider.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.coordination.AiModelProviderManager;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.dto.AiModelImportItem;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderSaveDTO;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderTestDTO;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderTemplateCatalog;
import com.mdframe.forge.plugin.ai.provider.support.ProviderModelFetcher;
import com.mdframe.forge.plugin.ai.provider.vo.AiProviderTemplateVO;
import com.mdframe.forge.plugin.ai.provider.vo.AiProviderVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai/provider")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderService providerService;
    private final AiModelService modelService;
    private final AiModelProviderManager modelProviderManager;

    /**
     * 内置供应商预设与当前租户连接计数，不返回连接密钥。
     */
    @GetMapping("/templates")
    public RespInfo<List<AiProviderTemplateVO>> templates() {
        return RespInfo.success(AiProviderTemplateCatalog.withCounts(providerService.countProviderTypes()));
    }

    /**
     * 分页查询供应商列表
     */
    @GetMapping("/page")
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<Page<AiProviderVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String providerName,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String status) {
        Page<AiProvider> page = providerService.pageProviders(
                pageNum, pageSize, providerName, providerType, status);
        List<AiProviderVO> records = page.getRecords().stream()
                .map(provider -> {
                    fillModelsFromAiModel(provider);
                    return providerService.toSafeView(provider);
                })
                .toList();
        Page<AiProviderVO> safePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        safePage.setRecords(records);
        return RespInfo.success(safePage);
    }

    /**
     * 查询供应商详情
     */
    @GetMapping("/{id}")
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<AiProviderVO> getById(@PathVariable Long id) {
        AiProvider provider = providerService.getById(id);
        if (provider != null) {
            fillModelsFromAiModel(provider);
        }
        return RespInfo.success(providerService.toSafeView(provider));
    }

    /**
     * 创建供应商
     */
    @PostMapping
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<Void> create(@RequestBody AiProviderSaveDTO request) {
        providerService.createProvider(request);
        return RespInfo.success();
    }

    /**
     * 更新供应商
     */
    @PutMapping
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<Void> update(@RequestBody AiProviderSaveDTO request) {
        modelProviderManager.updateProvider(request);
        return RespInfo.success();
    }

    /**
     * 删除供应商（校验关联模型）
     */
    @DeleteMapping("/{id}")
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<Void> delete(@PathVariable Long id) {
        modelProviderManager.deleteProvider(id);
        return RespInfo.success();
    }

    /**
     * 测试供应商连接
     */
    @PostMapping("/test")
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<String> test(@RequestBody AiProviderTestDTO request) {
        return RespInfo.success(providerService.testConnection(request));
    }

    /**
     * 设为默认供应商
     */
    @PutMapping("/{id}/default")
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<Void> setDefault(@PathVariable Long id) {
        modelProviderManager.setDefaultProvider(id);
        return RespInfo.success();
    }

    /**
     * 拉取供应商可用模型列表（调 OpenAI 兼容的 /v1/models 端点）
     */
    @PostMapping("/{id}/fetch-models")
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<List<ProviderModelFetcher.FetchedModel>> fetchModels(@PathVariable Long id) {
        return RespInfo.success(providerService.fetchModels(id));
    }

    /**
     * 批量导入模型到供应商
     */
    @PostMapping("/{id}/models/batch")
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<Integer> batchImportModels(@PathVariable Long id,
                                                @RequestBody List<AiModelImportItem> items) {
        return RespInfo.success(providerService.batchImportModels(id, items));
    }

    /**
     * 从 ai_model 表聚合填充供应商的 models 和 defaultModel 字段
     * 确保旧接口响应格式兼容
     */
    private void fillModelsFromAiModel(AiProvider provider) {
        List<AiModel> models = modelService.listByProviderId(provider.getId());
        List<String> modelIdList = models.stream()
                .map(AiModel::getModelId)
                .collect(Collectors.toList());
        provider.setModels(modelIdList.isEmpty() ? "[]" : toJsonArray(modelIdList));
        String defaultModel = models.stream()
                .filter(m -> AiConstants.IS_DEFAULT_YES.equals(m.getIsDefault()))
                .map(AiModel::getModelId)
                .findFirst()
                .orElse(provider.getDefaultModel());
        provider.setDefaultModel(defaultModel);
    }

    private String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
