package com.mdframe.forge.plugin.ai.resource.service;

import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.knowledge.domain.*;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiStoreInstanceMapper;
import com.mdframe.forge.plugin.ai.model.constant.AiModelType;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.resource.domain.AiKnowledgeResourceProfile;
import com.mdframe.forge.plugin.ai.resource.dto.AiKnowledgeResourceSaveDTO;
import com.mdframe.forge.plugin.ai.resource.mapper.AiKnowledgeResourceProfileMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/**
 * 租户级知识库默认检索资源配置服务（向量模型 / 重排模型 / 向量存储 / 默认维度与分段）。
 */
@Service
@RequiredArgsConstructor
public class AiKnowledgeResourceProfiles {

    private static final String STORE_CATEGORY_VECTOR = "vector_store";
    private static final String STORE_TYPE_PG_VECTOR = "PG_VECTOR";

    private final AiKnowledgeResourceProfileMapper profiles;
    private final AiModelMapper models;
    private final AiProviderMapper providers;
    private final AiStoreInstanceMapper stores;

    public record View(Long id, Integer revision, Long embeddingModelId, String embeddingName, Long rerankModelId,
                       String rerankName, Long vectorStoreInstanceId, String storeName, Integer dimension,
                       Integer chunkSize, boolean configured, boolean available, boolean local, String message) {
    }

    public record Resources(AiModel embedding, AiProvider embeddingProvider, AiModel rerank,
                            AiProvider rerankProvider, AiStoreInstance store) {
    }

    public View current() {
        var row = profiles.find(tenant());
        if (row == null) {
            return new View(null, 0, null, null, null, null, null, null, null, 512,
                    false, false, false, "尚未配置知识库默认检索资源");
        }
        AiKnowledge knowledge = knowledge(row);
        try {
            var resources = validate(knowledge);
            return new View(row.getId(), row.getRevision(), row.getEmbeddingModelId(),
                    resources.embedding().getModelName(),
                    row.getRerankModelId(),
                    resources.rerank() == null ? null : resources.rerank().getModelName(),
                    row.getVectorStoreInstanceId(), resources.store().getInstanceName(),
                    row.getDimension(), row.getChunkSize(),
                    true, true, local(resources), "仅预填新知识库；已建知识库保持当前索引绑定");
        } catch (BusinessException unavailable) {
            return new View(row.getId(), row.getRevision(), row.getEmbeddingModelId(), null,
                    row.getRerankModelId(), null,
                    row.getVectorStoreInstanceId(), null, row.getDimension(), row.getChunkSize(),
                    true, false, false, "默认资源已停用、不可见或配置不匹配，请检查模型和存储");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public View save(AiKnowledgeResourceSaveDTO dto) {
        Long tenant = tenant();
        var row = profiles.locked(tenant);
        if ((row == null && dto.getRevision() != 0)
                || (row != null && !Objects.equals(row.getRevision(), dto.getRevision()))) {
            throw new BusinessException(409, "默认资源已被修改，请刷新后重试");
        }
        var next = new AiKnowledgeResourceProfile();
        next.setTenantId(tenant);
        next.setEmbeddingModelId(dto.getEmbeddingModelId());
        next.setRerankModelId(dto.getRerankModelId());
        next.setVectorStoreInstanceId(dto.getVectorStoreInstanceId());
        next.setDimension(dto.getDimension());
        next.setChunkSize(dto.getChunkSize());
        next.setUpdateBy(SessionHelper.getUserId());
        validate(knowledge(next));
        if (row == null) {
            next.setDelFlag(0L);
            next.setRevision(1);
            next.setCreateBy(SessionHelper.getUserId());
            next.setCreateDept(SessionHelper.getActiveOrgId());
            try {
                profiles.insert(next);
            } catch (DuplicateKeyException conflict) {
                throw new BusinessException(409, "默认资源已被创建，请刷新后重试");
            }
        } else {
            next.setId(row.getId());
            if (profiles.revise(next, dto.getRevision()) != 1) {
                throw new BusinessException(409, "默认资源已被修改，请刷新后重试");
            }
        }
        return current();
    }

    /**
     * 新建知识库时若未显式指定模型与存储，则自动填充租户默认检索资源。
     */
    public void prepareCreation(AiKnowledge knowledge) {
        if (knowledge.getEmbeddingModelId() == null && knowledge.getVectorStoreInstanceId() == null) {
            var row = profiles.find(tenant());
            if (row != null) {
                knowledge.setEmbeddingModelId(row.getEmbeddingModelId());
                knowledge.setVectorStoreInstanceId(row.getVectorStoreInstanceId());
                if (knowledge.getRerankModelId() == null) {
                    knowledge.setRerankModelId(row.getRerankModelId());
                }
                if (knowledge.getDimensionOfVectorModel() == null) {
                    knowledge.setDimensionOfVectorModel(row.getDimension());
                }
                if (knowledge.getChunkStrategy() == null) {
                    knowledge.setChunkStrategy("length");
                }
                if (knowledge.getChunkConfigJson() == null) {
                    knowledge.setChunkConfigJson(JSON.toJSONString(
                            Map.of("max_tokens", row.getChunkSize(), "overlap", 16)));
                }
                if (knowledge.getSearchConfigJson() == null) {
                    knowledge.setSearchConfigJson(JSON.toJSONString(Map.of(
                            "rerank_enable", row.getRerankModelId() != null,
                            "fusion_strategy", "rrf",
                            "topK", 5,
                            "max_context_chars", 12000)));
                }
            }
        }
        validate(knowledge);
    }

    public Resources validate(AiKnowledge knowledge) {
        Long tenant = tenant();
        if (knowledge.getTenantId() != null && !Objects.equals(tenant, knowledge.getTenantId())) {
            throw new BusinessException(403, "知识资源租户不一致");
        }
        AiModel embedding = model(knowledge.getEmbeddingModelId(), AiModelType.EMBEDDING, tenant);
        AiProvider embeddingProvider = provider(embedding, tenant);
        AiModel rerank = knowledge.getRerankModelId() == null
                ? null : model(knowledge.getRerankModelId(), AiModelType.RERANK, tenant);
        AiProvider rerankProvider = rerank == null ? null : provider(rerank, tenant);
        AiStoreInstance store = knowledge.getVectorStoreInstanceId() == null
                ? null : stores.selectById(knowledge.getVectorStoreInstanceId());
        if (store == null || !Objects.equals(tenant, store.getTenantId())
                || !AiConstants.STATUS_NORMAL.equals(store.getStatus())
                || (store.getDelFlag() != null && store.getDelFlag() != 0L)
                || !STORE_CATEGORY_VECTOR.equals(store.getCategory())) {
            throw new BusinessException("向量存储不存在、已停用或不属于当前租户");
        }
        Integer dimension = knowledge.getDimensionOfVectorModel();
        if (dimension != null && (dimension < 1 || dimension > 65536
                || (STORE_TYPE_PG_VECTOR.equals(store.getStoreType()) && dimension > 2000))) {
            throw new BusinessException("向量维度超出当前存储支持范围");
        }
        JSONObject config;
        try {
            config = JSON.parseObject(knowledge.getChunkConfigJson());
        } catch (RuntimeException invalid) {
            throw new BusinessException("分段设置须为有效对象");
        }
        if (config != null) {
            Object raw = config.get("max_tokens");
            int size = raw == null ? 500 : raw instanceof Integer n ? n : -1;
            Object overlap = config.get("overlap");
            int overlapSize = overlap == null ? 16 : overlap instanceof Integer n ? n : -1;
            if (size < 32 || size > 2048 || overlapSize < 0 || overlapSize >= size || overlapSize > 256
                    || (embedding.getContextWindow() != null && size > embedding.getContextWindow())) {
                throw new BusinessException("分段字符上限或重叠长度超出范围；请减小分段，实际模型还会校验token限制");
            }
        }
        return new Resources(embedding, embeddingProvider, rerank, rerankProvider, store);
    }

    private AiModel model(Long id, AiModelType expectedType, Long tenant) {
        var value = id == null ? null : models.selectEnabledById(id);
        if (value == null || !Objects.equals(tenant, value.getTenantId())
                || !expectedType.getCode().equals(value.getModelType())) {
            throw new BusinessException("知识模型不存在、已停用、租户或能力不匹配");
        }
        return value;
    }

    private AiProvider provider(AiModel model, Long tenant) {
        var value = providers.selectEnabledById(model.getProviderId());
        if (value == null || !Objects.equals(tenant, value.getTenantId())) {
            throw new BusinessException("知识模型供应商不存在或已停用");
        }
        return value;
    }

    private boolean local(Resources resources) {
        if (!AiPrivateEndpoint.url(resources.embeddingProvider().getBaseUrl())
                || (resources.rerankProvider() != null
                && !AiPrivateEndpoint.url(resources.rerankProvider().getBaseUrl()))
                || !STORE_TYPE_PG_VECTOR.equals(resources.store().getStoreType())) {
            return false;
        }
        try {
            return AiPrivateEndpoint.host(JSON.parseObject(resources.store().getConfigJson()).getString("host"));
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    private AiKnowledge knowledge(AiKnowledgeResourceProfile row) {
        var value = new AiKnowledge();
        value.setTenantId(row.getTenantId());
        value.setEmbeddingModelId(row.getEmbeddingModelId());
        value.setRerankModelId(row.getRerankModelId());
        value.setVectorStoreInstanceId(row.getVectorStoreInstanceId());
        value.setDimensionOfVectorModel(row.getDimension());
        value.setChunkConfigJson(JSON.toJSONString(Map.of("max_tokens", row.getChunkSize(), "overlap", 16)));
        return value;
    }

    private Long tenant() {
        Long id = TenantContextHolder.getTenantId();
        if (id == null || id < 1) {
            throw new BusinessException(403, "知识资源需要当前租户上下文");
        }
        return id;
    }
}
