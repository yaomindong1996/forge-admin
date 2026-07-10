package com.mdframe.forge.plugin.ai.provider.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterCode;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderSaveDTO;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderTestDTO;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.provider.vo.AiProviderVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/ai/provider")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderService providerService;
    private final AiModelService modelService;
    private final ObjectMapper objectMapper;

    /**
     * 内置供应商预设模板列表（纯代码查表，不查数据库）
     */
    @GetMapping("/templates")
    public RespInfo<List<Map<String, String>>> templates() {
        return RespInfo.success(List.of(
                template("alibaba_native", "阿里百炼（原生）", "https://dashscope.aliyuncs.com", "qwen-plus",
                        AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode()),
                template("alibaba", "阿里百炼（兼容模式）", "https://dashscope.aliyuncs.com/compatible-mode",
                        "qwen-plus", AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode()),
                template("openai", "OpenAI", "https://api.openai.com", "gpt-4o-mini",
                        AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode()),
                template("zhipu", "智谱 AI", "https://open.bigmodel.cn/api/paas/v4", "glm-4",
                        AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode()),
                template("moonshot", "Moonshot", "https://api.moonshot.cn/v1", "moonshot-v1-8k",
                        AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode()),
                template("deepseek", "DeepSeek", "https://api.deepseek.com", "deepseek-chat",
                        AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode()),
                template("ollama", "Ollama（本地）", "http://localhost:11434", "llama3",
                        AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode()),
                template("custom", "自定义", "", "", AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode())
        ));
    }

    private Map<String, String> template(String key, String name, String baseUrl,
                                         String defaultModel, String adapterCode) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("templateKey", key);
        m.put("name", name);
        m.put("baseUrl", baseUrl);
        m.put("defaultModel", defaultModel);
        m.put("adapterCode", adapterCode);
        return m;
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
        providerService.updateProvider(request);
        // 双写同步：更新供应商后重新聚合 models
        syncModelsToProvider(request.getId());
        return RespInfo.success();
    }

    /**
     * 删除供应商（校验关联模型）
     */
    @DeleteMapping("/{id}")
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<Void> delete(@PathVariable Long id) {
        long modelCount = modelService.countByProviderId(id);
        if (modelCount > 0) {
            throw new BusinessException("该供应商下存在 " + modelCount + " 个关联模型，请先删除关联模型");
        }
        providerService.deleteProvider(id);
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
        providerService.setDefault(id);
        return RespInfo.success();
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

    private String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
