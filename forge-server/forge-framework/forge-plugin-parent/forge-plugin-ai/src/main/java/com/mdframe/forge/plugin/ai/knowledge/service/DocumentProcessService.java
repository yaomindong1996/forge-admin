package com.mdframe.forge.plugin.ai.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.ai.knowledge.chunker.ChunkerRegistry;
import com.mdframe.forge.plugin.ai.knowledge.chunker.dto.ChunkResult;
import com.mdframe.forge.plugin.ai.knowledge.domain.*;
import com.mdframe.forge.plugin.ai.knowledge.enums.AiDocumentProcessStatus;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeDocumentMapper;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeMapper;
import com.mdframe.forge.plugin.ai.knowledge.parser.DocumentParserRegistry;
import com.mdframe.forge.plugin.ai.knowledge.parser.dto.ParsedDocument;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.DocumentProcessEvent;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.DocumentUploadRequest;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreFactory;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreService;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文档处理服务。
 * 负责文档上传、解析、分块、向量化、入库的完整流水线。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessService {

    private final AiKnowledgeMapper knowledgeMapper;
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiModelMapper modelMapper;
    private final AiProviderMapper providerMapper;
    private final AiModelAdapterRegistry modelAdapterRegistry;
    private final AiSecretCrypto aiSecretCrypto;
    private final DocumentParserRegistry parserRegistry;
    private final ChunkerRegistry chunkerRegistry;
    private final VectorStoreFactory vectorStoreFactory;
    private final AiStoreInstanceService storeInstanceService;
    private final FileManager fileManager;

    /**
     * SSE 事件汇：documentId → Sink
     */
    private final ConcurrentHashMap<Long, Sinks.Many<DocumentProcessEvent>> progressSinks = new ConcurrentHashMap<>();

    /**
     * 异步处理线程池
     */
    private final ExecutorService processExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "doc-process");
                t.setDaemon(true);
                return t;
            }
    );

    /**
     * 订阅文档处理进度（SSE）
     */
    public Flux<ServerSentEvent<DocumentProcessEvent>> subscribeProgress(Long documentId) {
        Sinks.Many<DocumentProcessEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
        progressSinks.put(documentId, sink);

        return sink.asFlux()
                .map(event -> ServerSentEvent.<DocumentProcessEvent>builder()
                        .id(String.valueOf(System.nanoTime()))
                        .event("progress")
                        .data(event)
                        .build())
                .doFinally(signalType -> progressSinks.remove(documentId));
    }

    /**
     * 上传文档（第一步：创建文档记录，可选立即处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocument uploadDocument(DocumentUploadRequest request) {
        AiKnowledge knowledge = knowledgeMapper.selectByIdForUpdate(request.getKnowledgeId());
        if (knowledge == null) {
            throw new BusinessException("知识库不存在");
        }

        // 推断文档类型
        String docType = request.getDocType();
        if (docType == null || docType.isBlank()) {
            docType = parserRegistry.inferDocType(request.getDocName());
        }

        // 计算内容哈希（去重）
        String contentHash = null;
        if (request.getFileId() != null) {
            contentHash = computeFileHash(request.getFileId());
        }

        // 去重检查
        if (contentHash != null && !"none".equals(knowledge.getDedupStrategy())) {
            long dupCount = documentMapper.countByContentHash(knowledge.getId(), contentHash, null);
            if (dupCount > 0) {
                handleDedup(knowledge.getDedupStrategy(), knowledge.getDedupAction(), request.getDocName());
            }
        }

        // 创建文档记录
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setKnowledgeId(knowledge.getId());
        document.setFileId(request.getFileId());
        document.setDocName(request.getDocName());
        document.setDocType(docType);
        document.setSourceType(request.getSourceType() != null ? request.getSourceType() : "upload");
        document.setSourceUrl(request.getSourceUrl());
        document.setContentHash(contentHash);
        document.setChunkCount(0);
        document.setProcessStatus(AiDocumentProcessStatus.PENDING.getCode());
        documentMapper.insert(document);

        // 两步上传模式：不自动处理，等待确认
        if ("1".equals(knowledge.getUploadConfirm()) && !Boolean.TRUE.equals(request.getConfirm())) {
            return document;
        }

        // 立即异步处理
        processDocumentAsync(document.getId(), knowledge);
        return document;
    }

    /**
     * 确认并处理文档（两步上传第二步）
     */
    public void confirmAndProcess(Long documentId) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        if (!AiDocumentProcessStatus.PENDING.matches(document.getProcessStatus())) {
            throw new BusinessException("文档不在待处理状态");
        }
        AiKnowledge knowledge = knowledgeMapper.selectById(document.getKnowledgeId());
        if (knowledge == null) {
            throw new BusinessException("知识库不存在");
        }
        processDocumentAsync(documentId, knowledge);
    }

    /**
     * 重新处理失败文档
     */
    public void reprocessDocument(Long documentId) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        if (!AiDocumentProcessStatus.FAILED.matches(document.getProcessStatus())) {
            throw new BusinessException("仅失败状态的文档可重新处理");
        }
        AiKnowledge knowledge = knowledgeMapper.selectById(document.getKnowledgeId());
        if (knowledge == null) {
            throw new BusinessException("知识库不存在");
        }
        // 重置状态为待处理并清空错误信息
        documentMapper.updateProcessStatus(documentId, AiDocumentProcessStatus.PENDING.getCode(), null, null);
        processDocumentAsync(documentId, knowledge);
    }

    /**
     * 查看文档分块列表（按分块序号升序）
     */
    public List<AiKnowledgeChunk> listChunks(Long documentId) {
        return chunkMapper.selectByDocumentId(documentId);
    }

    /**
     * 查看文档原始内容。
     * upload 来源重新解析文件获取全文；manual/url 来源直接返回录入文本。
     */
    public String getDocumentRawContent(Long documentId) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        if ("manual".equals(document.getSourceType()) || "url".equals(document.getSourceType())) {
            return document.getSourceUrl() != null ? document.getSourceUrl() : "";
        }
        ParsedDocument parsed = parseDocument(document);
        return parsed.getContent() != null ? parsed.getContent() : "";
    }

    /**
     * 异步处理文档
     */
    private void processDocumentAsync(Long documentId, AiKnowledge knowledge) {
        processExecutor.submit(() -> {
            try {
                processDocument(documentId, knowledge);
            } catch (Exception e) {
                log.error("[文档处理] 异步处理失败: documentId={}", documentId, e);
                documentMapper.updateProcessStatus(documentId, AiDocumentProcessStatus.FAILED.getCode(), e.getMessage(), null);
                emitProgress(documentId, "", "failed", 0, "处理失败: " + e.getMessage(), AiDocumentProcessStatus.FAILED.getCode());
            }
        });
    }

    /**
     * 同步处理文档（核心流水线）
     */
    private void processDocument(Long documentId, AiKnowledge knowledge) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) return;

        try {
            // 1. 更新状态为处理中
            documentMapper.updateProcessStatus(documentId, AiDocumentProcessStatus.PROCESSING.getCode(), null, null);
            emitProgress(documentId, document.getDocName(), "parsing", 10, "开始解析文档", AiDocumentProcessStatus.PROCESSING.getCode());

            // 2. 解析文档
            ParsedDocument parsed;
            if ("manual".equals(document.getSourceType()) || "url".equals(document.getSourceType())) {
                // 手动输入或 URL，直接使用内容
                parsed = ParsedDocument.of(document.getDocName(), document.getSourceUrl() != null ? document.getSourceUrl() : "");
            } else {
                parsed = parseDocument(document);
            }
            emitProgress(documentId, document.getDocName(), "parsing", 30, "文档解析完成", AiDocumentProcessStatus.PROCESSING.getCode());

            // 3. 分块
            List<ChunkResult> chunks = chunkerRegistry.chunk(
                    parsed.getContent(),
                    knowledge.getChunkStrategy(),
                    knowledge.getChunkConfigJson()
            );
            emitProgress(documentId, document.getDocName(), "chunking", 50,
                    "分块完成，共" + chunks.size() + "个分块", AiDocumentProcessStatus.PROCESSING.getCode());

            // 4. 向量化 + 入库
            embedAndStore(documentId, knowledge, chunks);
            emitProgress(documentId, document.getDocName(), "embedding", 90, "向量化入库完成", AiDocumentProcessStatus.PROCESSING.getCode());

            // 5. 更新文档状态
            documentMapper.updateProcessStatus(documentId, AiDocumentProcessStatus.SUCCESS.getCode(), null, chunks.size());
            emitProgress(documentId, document.getDocName(), "complete", 100, "处理完成", AiDocumentProcessStatus.SUCCESS.getCode());

        } catch (Exception e) {
            log.error("[文档处理] 处理失败: documentId={}", documentId, e);
            documentMapper.updateProcessStatus(documentId, AiDocumentProcessStatus.FAILED.getCode(), e.getMessage(), null);
            emitProgress(documentId, document.getDocName(), "failed", 0, "处理失败: " + e.getMessage(), AiDocumentProcessStatus.FAILED.getCode());
        }
    }

    /**
     * 解析文档
     */
    private ParsedDocument parseDocument(AiKnowledgeDocument document) {
        if (document.getFileId() == null) {
            return ParsedDocument.of(document.getDocName(), "");
        }
        try (InputStream is = getFileInputStream(document.getFileId())) {
            return parserRegistry.parse(is, document.getDocName(), document.getDocType());
        } catch (Exception e) {
            throw new BusinessException("文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 向量化并存储
     */
    private void embedAndStore(Long documentId, AiKnowledge knowledge, List<ChunkResult> chunks) {
        if (chunks.isEmpty()) return;

        // 来源标识：URL来源存 source_url，否则回退文档名（库表导入将来存表/行标识）
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        String sourceId = document != null && document.getSourceUrl() != null && !document.getSourceUrl().isBlank()
                ? document.getSourceUrl() : (document != null ? document.getDocName() : null);

        // 获取 Embedding 模型信息
        AiModel embeddingModel = modelMapper.selectEnabledById(knowledge.getEmbeddingModelId());
        if (embeddingModel == null) {
            throw new BusinessException("Embedding模型不存在或未启用");
        }
        AiProvider provider = providerMapper.selectById(embeddingModel.getProviderId());
        if (provider == null) {
            throw new BusinessException("Embedding模型供应商不存在");
        }

        // 解密 API Key
        String apiKey = aiSecretCrypto.resolvePlainSecret(provider.getApiKey());

        // 获取适配器
        var adapter = modelAdapterRegistry.getEmbedding(embeddingModel.getModelId());

        // 获取向量存储
        VectorStoreService vectorStore = vectorStoreFactory.getService(
                knowledge.getVectorStoreInstanceId() != null
                        ? getStoreInstance(knowledge.getVectorStoreInstanceId()) : null
        );

        // 集合名称
        String collectionName = "knowledge_" + knowledge.getId();

        // 先批量嵌入，再按模型实际输出维度建集合：
        // 知识库未配置向量维度时以模型真实输出为准（避免硬编码 1536 与 text-embedding-v3 实际输出 1024 不符）
        List<String> texts = chunks.stream().map(ChunkResult::getContent).toList();
        List<List<Float>> vectors = adapter.embed(
                provider.getBaseUrl(), apiKey, embeddingModel.getModelId(), texts
        );
        int actualDim = vectors.isEmpty() ? 0 : vectors.get(0).size();
        Integer configuredDim = knowledge.getDimensionOfVectorModel();
        int dimension;
        if (configuredDim != null && configuredDim > 0) {
            if (actualDim > 0 && actualDim != configuredDim) {
                throw new BusinessException("Embedding模型输出维度(" + actualDim + ")与知识库配置维度(" + configuredDim + ")不一致");
            }
            dimension = configuredDim;
        } else {
            if (actualDim <= 0) {
                throw new BusinessException("Embedding模型未返回有效向量");
            }
            dimension = actualDim;
        }

        // 确保集合存在（维度不匹配时 forceRecreate 触发删除重建，见 MilvusVectorStoreService 维度校验）
        String configJson = knowledge.getVectorStoreInstanceId() != null
                ? getStoreInstance(knowledge.getVectorStoreInstanceId()).getConfigJson() : "{}";
        VectorStoreService.CreateCollectionRequest createReq = new VectorStoreService.CreateCollectionRequest();
        createReq.setCollectionName(collectionName);
        createReq.setDimension(dimension);
        createReq.setConfigJson(configJson);
        createReq.setForceRecreate(true);
        vectorStore.createCollectionIfAbsent(createReq);

        // 保存分块到数据库
        List<AiKnowledgeChunk> chunkEntities = new ArrayList<>();
        List<String> vectorIds = new ArrayList<>();
        List<List<Float>> vectorList = new ArrayList<>();
        List<Long> docIds = new ArrayList<>();
        List<Integer> chunkIndices = new ArrayList<>();
        List<String> sourceIds = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        List<String> hideContents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkResult chunk = chunks.get(i);
            AiKnowledgeChunk entity = new AiKnowledgeChunk();
            entity.setKnowledgeId(knowledge.getId());
            entity.setDocumentId(documentId);
            entity.setChunkIndex(chunk.getIndex());
            entity.setContent(chunk.getContent());
            entity.setTitle(chunk.getTitle());
            entity.setTokenCount(chunk.getTokenCount());
            entity.setRefCount(1);
            entity.setContentHash(hashContent(chunk.getContent()));
            entity.setRetrievalCount(0);

            String vectorId = "doc_" + documentId + "_chunk_" + chunk.getIndex();
            entity.setVectorId(vectorId);
            chunkMapper.insert(entity);

            vectorIds.add(vectorId);
            vectorList.add(vectors.get(i));
            docIds.add(documentId);
            chunkIndices.add(chunk.getIndex());
            sourceIds.add(sourceId);
            titles.add(chunk.getTitle());
            // 分块暂无"隐藏内容"概念，预留空串（参考项目存医生介绍等不参与检索的补充详情）
            hideContents.add(null);
            chunkEntities.add(entity);
        }

        // 插入向量存储
        VectorStoreService.InsertRequest insertReq = new VectorStoreService.InsertRequest();
        insertReq.setCollectionName(collectionName);
        insertReq.setIds(vectorIds);
        insertReq.setVectors(vectorList);
        insertReq.setContents(texts);
        insertReq.setDocumentIds(docIds);
        insertReq.setChunkIndices(chunkIndices);
        insertReq.setSourceIds(sourceIds);
        insertReq.setTitles(titles);
        insertReq.setHideContents(hideContents);
        insertReq.setConfigJson(configJson);
        vectorStore.insert(insertReq);
    }

    /**
     * 删除文档及其分块和向量
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) return;

        // 获取分块的向量ID
        List<AiKnowledgeChunk> chunks = chunkMapper.selectByDocumentId(documentId);
        if (!chunks.isEmpty()) {
            // 删除向量
            AiKnowledge knowledge = knowledgeMapper.selectById(document.getKnowledgeId());
            if (knowledge != null && knowledge.getVectorStoreInstanceId() != null) {
                try {
                    VectorStoreService vectorStore = vectorStoreFactory.getService(
                            getStoreInstance(knowledge.getVectorStoreInstanceId()));
                    VectorStoreService.DeleteRequest deleteReq = new VectorStoreService.DeleteRequest();
                    deleteReq.setCollectionName("knowledge_" + knowledge.getId());
                    deleteReq.setIds(chunks.stream().map(AiKnowledgeChunk::getVectorId).toList());
                    deleteReq.setConfigJson(getStoreInstance(knowledge.getVectorStoreInstanceId()).getConfigJson());
                    vectorStore.delete(deleteReq);
                } catch (Exception e) {
                    log.warn("[文档删除] 向量删除失败，继续删除数据库记录: documentId={}", documentId, e);
                }
            }

            // 逻辑删除分块
            chunkMapper.deleteByDocumentId(documentId);
        }

        // 逻辑删除文档
        documentMapper.deleteById(documentId);
    }

    // ===== 辅助方法 =====

    private AiStoreInstance getStoreInstance(Long storeInstanceId) {
        return storeInstanceService.getById(storeInstanceId);
    }

    private String computeFileHash(String fileId) {
        try (InputStream is = getFileInputStream(fileId)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            log.warn("[文件哈希] 计算失败: fileId={}", fileId, e);
            return null;
        }
    }

    private String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void handleDedup(String strategy, String action, String docName) {
        if ("reject".equals(action)) {
            throw new BusinessException("文档内容重复，已拒绝上传: " + docName);
        }
        // skip / overwrite 由后续处理逻辑处理
        log.info("[去重] 文档重复，策略={}: {}", action, docName);
    }

    private void emitProgress(Long documentId, String docName, String stage, int progress, String message, String status) {
        Sinks.Many<DocumentProcessEvent> sink = progressSinks.get(documentId);
        if (sink != null) {
            sink.tryEmitNext(DocumentProcessEvent.of(documentId, docName, stage, progress, message, status));
        }
    }

    /**
     * 通过 FileManager 获取文件输入流。
     * fileId 为 FileManager 上传返回的 UUID 字符串。
     */
    private InputStream getFileInputStream(String fileId) {
        if (fileId == null) {
            throw new BusinessException("文件ID不能为空");
        }
        var metadata = fileManager.getFileMetadata(fileId);
        if (metadata == null) {
            throw new BusinessException("文件不存在: " + fileId);
        }
        FileStorage storage = fileManager.getStorage(metadata.getStorageType());
        if (storage == null) {
            throw new BusinessException("文件存储策略不存在: " + metadata.getStorageType());
        }
        return storage.download(fileId);
    }
}
