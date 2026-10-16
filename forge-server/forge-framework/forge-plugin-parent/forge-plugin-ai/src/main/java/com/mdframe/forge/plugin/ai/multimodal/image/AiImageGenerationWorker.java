package com.mdframe.forge.plugin.ai.multimodal.image;

import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import com.mdframe.forge.plugin.ai.multimodal.image.enums.AiImageGenerateStatus;
import com.mdframe.forge.plugin.ai.multimodal.image.mapper.AiImageGenerateRecordMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 远程 I/O 不在数据库事务内；记录条件更新维持 pending/generating/终态的唯一方向。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiImageGenerationWorker {
    private final AiImageGenerateRecordMapper records;
    private final AiImageModelResolver models;
    private final AiModelAdapterRegistry adapters;
    private final AiSecretCrypto crypto;
    private final AiGeneratedImageStore store;

    public void run(Long recordId) {
        AgentCallerGuard.requireCaller();
        var record = requireOwned(records.selectById(recordId));
        if (!transition(record, AiImageGenerateStatus.PENDING, AiImageGenerateStatus.GENERATING)) {
            return;
        }
        try {
            var binding = models.require(record.getModelId());
            if (!Objects.equals(binding.provider().getId(), record.getProviderId())) {
                throw new BusinessException("图像模型供应商配置已变更，请重新发起");
            }
            String key = binding.provider().getApiKey();
            if (AiSecretCrypto.isEncrypted(key)) {
                key = crypto.decrypt(key);
            }
            if (key == null || key.isBlank()) {
                throw new BusinessException("图像供应商密钥不可用");
            }
            var model = binding.model();
            String image = adapters.getImage(model.getModelId()).generate(binding.provider().getBaseUrl(),
                    key, model.getModelId(), record.getPrompt(), record.getNegativePrompt(), record.getSize());
            record.setResultFileId(store.store(image));
            transition(record, AiImageGenerateStatus.GENERATING, AiImageGenerateStatus.SUCCESS);
            log.info("[AI Image] 生成任务完成, recordId={}", recordId);
        } catch (Exception ex) {
            // SDK/存储异常可能包含密钥、签名 URL 或响应正文，只记录类型和受控文案。
            log.error("[AI Image] 生成任务失败, recordId={}, exceptionType={}",
                    recordId, ex.getClass().getSimpleName());
            record.setResultFileId(null);
            record.setErrorMsg("图片生成或存储失败，请检查模型与出站配置后重试");
            transition(record, AiImageGenerateStatus.GENERATING, AiImageGenerateStatus.FAILED);
        }
    }

    public void reject(AiImageGenerateRecord record) {
        requireOwned(record);
        record.setErrorMsg("图像生成服务繁忙，请稍后重新发起");
        transition(record, AiImageGenerateStatus.PENDING, AiImageGenerateStatus.FAILED);
    }

    static AiImageGenerateRecord requireOwned(AiImageGenerateRecord record) {
        var caller = AgentCallerGuard.requireCaller();
        if (record == null || !Objects.equals(record.getTenantId(), caller.getTenantId())
                || !Objects.equals(record.getUserId(), caller.getUserId())
                || (record.getDelFlag() != null && record.getDelFlag() != 0L)) {
            throw new BusinessException(403, "图像生成记录不存在或不可访问");
        }
        return record;
    }

    private boolean transition(AiImageGenerateRecord record, AiImageGenerateStatus expected,
                               AiImageGenerateStatus next) {
        if (!expected.canTransitionTo(next)) {
            throw new IllegalArgumentException("图像任务状态转换无效");
        }
        record.setStatus(next.getCode());
        return records.transition(record, expected.getCode()) == 1;
    }
}
