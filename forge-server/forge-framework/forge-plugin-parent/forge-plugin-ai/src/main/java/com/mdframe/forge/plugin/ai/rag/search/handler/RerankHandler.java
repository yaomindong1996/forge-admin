package com.mdframe.forge.plugin.ai.rag.search.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledge;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeMapper;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchResult;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.plugin.ai.rag.search.RagSearchContext;
import com.mdframe.forge.plugin.ai.rag.search.RagSearchHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rerank 处理器。
 * 使用 AiModelAdapterRegistry.getRerank() 对融合结果进行重排序。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RerankHandler implements RagSearchHandler {

    private final AiKnowledgeMapper knowledgeMapper;
    private final AiModelMapper modelMapper;
    private final AiProviderMapper providerMapper;
    private final AiModelAdapterRegistry modelAdapterRegistry;
    private final AiSecretCrypto aiSecretCrypto;

    @Override
    public String getName() {
        return "rerank";
    }

    @Override
    public void handle(RagSearchContext context) {
        // 请求字段优先，缺失时回退知识库检索配置 rerank_enable（对齐独立入口 KnowledgeSearchService）
        Boolean rerankEnabled = context.getRequest().getRerankEnable();
        Long knowledgeId = context.getRequest().getKnowledgeId();
        AiKnowledge knowledge = null;
        if (rerankEnabled == null && knowledgeId != null) {
            knowledge = knowledgeMapper.selectById(knowledgeId);
            if (knowledge != null) {
                Boolean configEnabled = readConfigRerankEnable(knowledge.getSearchConfigJson());
                if (configEnabled != null) {
                    rerankEnabled = configEnabled;
                }
            }
        }
        if (rerankEnabled == null || !rerankEnabled) {
            // 未启用 rerank，直接透传
            if (context.getFusedResults() != null) {
                context.setFinalResults(context.getFusedResults());
            } else if (context.getVectorResults() != null) {
                context.setFinalResults(context.getVectorResults());
            }
            return;
        }

        List<KnowledgeSearchResult> results = context.getFusedResults();
        if (results == null || results.isEmpty()) {
            context.setFinalResults(results);
            return;
        }

        if (knowledgeId == null) {
            log.warn("[RerankHandler] 未指定知识库ID，跳过Rerank");
            context.setFinalResults(results);
            return;
        }

        try {
            if (knowledge == null) {
                knowledge = knowledgeMapper.selectById(knowledgeId);
            }
            if (knowledge == null || knowledge.getRerankModelId() == null) {
                log.warn("[RerankHandler] 知识库未配置Rerank模型，跳过");
                context.setFinalResults(results);
                return;
            }

            AiModel rerankModel = modelMapper.selectEnabledById(knowledge.getRerankModelId());
            if (rerankModel == null) {
                log.warn("[RerankHandler] Rerank模型不存在或未启用，跳过");
                context.setFinalResults(results);
                return;
            }

            AiProvider provider = providerMapper.selectById(rerankModel.getProviderId());
            if (provider == null) {
                log.warn("[RerankHandler] Rerank模型供应商不存在，跳过");
                context.setFinalResults(results);
                return;
            }

            String apiKey = aiSecretCrypto.resolvePlainSecret(provider.getApiKey());

            var adapter = modelAdapterRegistry.getRerank(rerankModel.getModelId());
            List<String> passages = results.stream().map(KnowledgeSearchResult::getContent).toList();
            List<Float> scores = adapter.rerank(provider.getBaseUrl(), apiKey, rerankModel.getModelId(),
                    context.getRequest().getQuery(), passages);

            for (int i = 0; i < results.size() && i < scores.size(); i++) {
                results.get(i).setRerankScore(scores.get(i));
            }

            // 按 rerank 分数重新排序
            results.sort((a, b) -> Double.compare(b.getRerankScore(), a.getRerankScore()));
            context.setFinalResults(results);

            log.debug("[RerankHandler] Rerank完成, results={}", results.size());
        } catch (Exception e) {
            log.warn("[RerankHandler] Rerank失败，使用原始排序", e);
            context.setFinalResults(results);
        }
    }

    /**
     * 从知识库检索配置读取 rerank_enable（null 表示未配置，交由请求字段决定）
     */
    private Boolean readConfigRerankEnable(String searchConfigJson) {
        if (searchConfigJson == null || searchConfigJson.isBlank()) {
            return null;
        }
        try {
            JSONObject cfg = JSON.parseObject(searchConfigJson);
            return cfg.containsKey("rerank_enable") ? cfg.getBoolean("rerank_enable") : null;
        } catch (Exception e) {
            log.warn("[RerankHandler] 检索配置解析失败，使用请求字段: {}", e.getMessage());
            return null;
        }
    }
}
