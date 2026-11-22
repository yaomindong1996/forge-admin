package com.mdframe.forge.plugin.ai.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledge;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeChunk;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiStoreInstance;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeMapper;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchRequest;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchResult;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreFactory;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreService;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库向量检索服务。
 * 支持纯向量检索、阈值过滤、Nearby 上下文扩展、Lost-in-Middle 重排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSearchService {

    private final AiKnowledgeMapper knowledgeMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiModelMapper modelMapper;
    private final AiProviderMapper providerMapper;
    private final AiModelAdapterRegistry modelAdapterRegistry;
    private final AiSecretCrypto aiSecretCrypto;
    private final VectorStoreFactory vectorStoreFactory;
    private final AiStoreInstanceService storeInstanceService;

    /**
     * 检索知识库（独立检索入口：含 Rerank/Nearby/Lost-in-Middle 后处理）
     */
    public List<KnowledgeSearchResult> search(KnowledgeSearchRequest request) {
        AiKnowledge knowledge = knowledgeMapper.selectById(request.getKnowledgeId());
        if (knowledge == null) {
            throw new BusinessException("知识库不存在");
        }

        JSONObject searchConfig = parseSearchConfig(knowledge.getSearchConfigJson());
        boolean rerankEnable = request.getRerankEnable() != null ? request.getRerankEnable()
                : searchConfig.getBooleanValue("rerank_enable", false);
        boolean lostInMiddle = request.getLostInMiddle() != null ? request.getLostInMiddle()
                : searchConfig.getBooleanValue("lost_in_middle", false);
        int nearbyCount = searchConfig.getIntValue("nearby_count", 0);

        List<KnowledgeSearchResult> results = searchVectorOnly(knowledge, request);

        // 独立检索入口的后处理（RAG 管线内由 RerankHandler/FinalizeHandler 承担，避免重复执行）
        if (rerankEnable && knowledge.getRerankModelId() != null) {
            results = rerank(knowledge, request.getQuery(), results);
        }
        if (nearbyCount > 0) {
            results = expandNearby(results, nearbyCount);
        }
        if (lostInMiddle) {
            results = lostInMiddleRerank(results);
        }

        incrementRetrievalCount(results);
        return results;
    }

    /**
     * 纯向量检索（不做任何后处理），供 RAG 管线内作为向量检索阶段使用，
     * 避免与管线的 RerankHandler/FinalizeHandler 重复执行 Rerank/Nearby/Lost-in-Middle。
     */
    public List<KnowledgeSearchResult> searchVectorOnly(KnowledgeSearchRequest request) {
        AiKnowledge knowledge = knowledgeMapper.selectById(request.getKnowledgeId());
        if (knowledge == null) {
            throw new BusinessException("知识库不存在");
        }
        return searchVectorOnly(knowledge, request);
    }

    /**
     * 纯向量检索核心：Embedding → 向量检索 → 结果映射
     */
    private List<KnowledgeSearchResult> searchVectorOnly(AiKnowledge knowledge, KnowledgeSearchRequest request) {
        JSONObject searchConfig = parseSearchConfig(knowledge.getSearchConfigJson());
        int topK = request.getTopK() != null ? request.getTopK() : searchConfig.getIntValue("topK", 5);
        double threshold = request.getThreshold() != null ? request.getThreshold()
                : (searchConfig.getDoubleValue("threshold") > 0 ? searchConfig.getDoubleValue("threshold") : 0.5);

        List<Float> queryVector = embedQuery(knowledge, request.getQuery());
        List<VectorStoreService.SearchResult> vectorResults = vectorSearch(knowledge, queryVector, topK, threshold, request);
        if (vectorResults.isEmpty()) {
            return List.of();
        }
        return vectorResults.stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * Milvus 原生混合检索（稠密向量 + 标题BM25 + 正文BM25 单次调用融合，对齐参考项目 54doctor_ai）。
     * 管线 searchType=hybrid 时调用。
     */
    public List<KnowledgeSearchResult> hybridSearch(KnowledgeSearchRequest request) {
        AiKnowledge knowledge = knowledgeMapper.selectById(request.getKnowledgeId());
        if (knowledge == null) {
            throw new BusinessException("知识库不存在");
        }
        JSONObject searchConfig = parseSearchConfig(knowledge.getSearchConfigJson());
        int topK = request.getTopK() != null ? request.getTopK() : searchConfig.getIntValue("topK", 5);

        List<Float> queryVector = embedQuery(knowledge, request.getQuery());
        VectorStoreService vectorStore = resolveVectorStore(knowledge);
        String configJson = resolveConfigJson(knowledge);

        VectorStoreService.SearchRequest searchReq = new VectorStoreService.SearchRequest();
        searchReq.setCollectionName("knowledge_" + knowledge.getId());
        searchReq.setVector(queryVector);
        searchReq.setQuery(request.getQuery());
        searchReq.setTopK(topK);
        searchReq.setFilterExpr(request.getFilterExpr());
        // 权重/融合类型：知识库检索配置可配 rerank_type(rrf|weighted)/vector_weight/bm25_weight/rrf_k
        searchReq.setRerankType(searchConfig.getString("rerank_type"));
        if (searchConfig.getDouble("vector_weight") != null) {
            searchReq.setVectorWeight(searchConfig.getDouble("vector_weight"));
        }
        if (searchConfig.getDouble("bm25_weight") != null) {
            searchReq.setBm25Weight(searchConfig.getDouble("bm25_weight"));
        }
        searchReq.setRrfK(searchConfig.getInteger("rrf_k"));
        searchReq.setConfigJson(configJson);

        List<VectorStoreService.SearchResult> vectorResults = vectorStore.hybridSearch(searchReq);
        return vectorResults.stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * 更新检索计数
     */
    private void incrementRetrievalCount(List<KnowledgeSearchResult> results) {
        results.forEach(r -> {
            try {
                chunkMapper.incrementRetrievalCount(Long.parseLong(r.getChunkId()));
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Embedding 查询文本
     */
    private List<Float> embedQuery(AiKnowledge knowledge, String query) {
        AiModel embeddingModel = modelMapper.selectEnabledById(knowledge.getEmbeddingModelId());
        if (embeddingModel == null) {
            throw new BusinessException("Embedding模型不存在或未启用");
        }
        AiProvider provider = providerMapper.selectById(embeddingModel.getProviderId());
        if (provider == null) {
            throw new BusinessException("Embedding模型供应商不存在");
        }

        String apiKey = aiSecretCrypto.resolvePlainSecret(provider.getApiKey());

        var adapter = modelAdapterRegistry.getEmbedding(embeddingModel.getModelId());
        List<List<Float>> vectors = adapter.embed(provider.getBaseUrl(), apiKey, embeddingModel.getModelId(), List.of(query));
        return vectors.get(0);
    }

    /**
     * 向量检索
     */
    private List<VectorStoreService.SearchResult> vectorSearch(AiKnowledge knowledge, List<Float> queryVector, int topK, double threshold, KnowledgeSearchRequest request) {
        VectorStoreService vectorStore = resolveVectorStore(knowledge);
        String configJson = resolveConfigJson(knowledge);

        VectorStoreService.SearchRequest searchReq = new VectorStoreService.SearchRequest();
        searchReq.setCollectionName("knowledge_" + knowledge.getId());
        searchReq.setVector(queryVector);
        searchReq.setTopK(topK);
        searchReq.setThreshold(threshold);
        searchReq.setFilterExpr(request.getFilterExpr());
        searchReq.setConfigJson(configJson);

        return vectorStore.search(searchReq);
    }

    /**
     * Rerank
     */
    private List<KnowledgeSearchResult> rerank(AiKnowledge knowledge, String query, List<KnowledgeSearchResult> results) {
        try {
            AiModel rerankModel = modelMapper.selectEnabledById(knowledge.getRerankModelId());
            if (rerankModel == null) {
                log.warn("[Rerank] Rerank模型不存在，跳过");
                return results;
            }
            AiProvider provider = providerMapper.selectById(rerankModel.getProviderId());
            if (provider == null) {
                log.warn("[Rerank] Rerank模型供应商不存在，跳过");
                return results;
            }

            String apiKey = aiSecretCrypto.resolvePlainSecret(provider.getApiKey());

            var adapter = modelAdapterRegistry.getRerank(rerankModel.getModelId());
            List<String> passages = results.stream().map(KnowledgeSearchResult::getContent).toList();
            List<Float> scores = adapter.rerank(provider.getBaseUrl(), apiKey, rerankModel.getModelId(), query, passages);

            for (int i = 0; i < results.size() && i < scores.size(); i++) {
                results.get(i).setRerankScore(scores.get(i));
            }

            // 按 rerank 分数重新排序
            results.sort((a, b) -> Double.compare(b.getRerankScore(), a.getRerankScore()));
            return results;
        } catch (Exception e) {
            log.warn("[Rerank] Rerank失败，使用原始排序", e);
            return results;
        }
    }

    /**
     * Nearby 上下文扩展：为每个命中的分块追加前后相邻分块（按文档缓存，避免重复查库）
     */
    private List<KnowledgeSearchResult> expandNearby(List<KnowledgeSearchResult> results, int nearbyCount) {
        Set<String> seen = new HashSet<>();
        List<KnowledgeSearchResult> expanded = new ArrayList<>();
        Map<Long, List<AiKnowledgeChunk>> chunksByDoc = new HashMap<>();

        for (KnowledgeSearchResult result : results) {
            if (seen.add(result.getChunkId())) {
                expanded.add(result);
            }

            // 查找前后相邻分块（同一文档只查一次）
            Long documentId = result.getDocumentId();
            if (documentId == null) continue;
            List<AiKnowledgeChunk> nearbyChunks = chunksByDoc.computeIfAbsent(documentId, id -> {
                List<AiKnowledgeChunk> list = chunkMapper.selectByDocumentId(id);
                return list != null ? list : List.of();
            });
            int chunkIndex = result.getChunkIndex();

            for (int offset = -nearbyCount; offset <= nearbyCount; offset++) {
                if (offset == 0) continue;
                int targetIndex = chunkIndex + offset;
                if (targetIndex < 0) continue;

                for (AiKnowledgeChunk chunk : nearbyChunks) {
                    if (chunk.getChunkIndex() == targetIndex) {
                        String key = String.valueOf(chunk.getId());
                        if (seen.add(key)) {
                            KnowledgeSearchResult nearby = new KnowledgeSearchResult();
                            nearby.setChunkId(key);
                            nearby.setDocumentId(chunk.getDocumentId());
                            nearby.setChunkIndex(chunk.getChunkIndex());
                            nearby.setContent(chunk.getContent());
                            nearby.setTitle(chunk.getTitle());
                            nearby.setScore(result.getScore() * 0.8); // 降权
                            expanded.add(nearby);
                        }
                    }
                }
            }
        }

        return expanded;
    }

    /**
     * Lost-in-Middle 重排：将最相关和最不相关的放在开头和结尾，中等相关的放在中间。
     * 研究表明 LLM 对开头和结尾的信息更敏感。
     */
    private List<KnowledgeSearchResult> lostInMiddleRerank(List<KnowledgeSearchResult> results) {
        if (results.size() <= 2) return results;

        List<KnowledgeSearchResult> reranked = new ArrayList<>();
        int left = 0;
        int right = results.size() - 1;
        boolean takeLeft = true;

        while (left <= right) {
            if (takeLeft) {
                reranked.add(results.get(left++));
            } else {
                reranked.add(results.get(right--));
            }
            takeLeft = !takeLeft;
        }

        return reranked;
    }

    private KnowledgeSearchResult toSearchResult(VectorStoreService.SearchResult sr) {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setChunkId(sr.getId());
        result.setDocumentId(sr.getDocumentId());
        result.setChunkIndex(sr.getChunkIndex());
        result.setContent(sr.getContent());
        result.setTitle(sr.getTitle());
        result.setHideContent(sr.getHideContent());
        result.setSourceId(sr.getSourceId());
        result.setScore(sr.getScore());
        return result;
    }

    private VectorStoreService resolveVectorStore(AiKnowledge knowledge) {
        if (knowledge.getVectorStoreInstanceId() == null) {
            throw new BusinessException("知识库未配置向量存储实例，请先在知识库配置中绑定向量存储后再检索");
        }
        AiStoreInstance storeInstance = storeInstanceService.getById(knowledge.getVectorStoreInstanceId());
        return vectorStoreFactory.getService(storeInstance);
    }

    private String resolveConfigJson(AiKnowledge knowledge) {
        if (knowledge.getVectorStoreInstanceId() == null) {
            throw new BusinessException("知识库未配置向量存储实例，请先在知识库配置中绑定向量存储后再检索");
        }
        AiStoreInstance storeInstance = storeInstanceService.getById(knowledge.getVectorStoreInstanceId());
        return storeInstance != null ? storeInstance.getConfigJson() : "{}";
    }

    private JSONObject parseSearchConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(configJson);
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
