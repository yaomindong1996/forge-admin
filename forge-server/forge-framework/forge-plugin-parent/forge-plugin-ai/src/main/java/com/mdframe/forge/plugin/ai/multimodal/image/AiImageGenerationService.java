package com.mdframe.forge.plugin.ai.multimodal.image;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import com.mdframe.forge.plugin.ai.multimodal.image.enums.AiImageGenerateStatus;
import com.mdframe.forge.plugin.ai.multimodal.image.mapper.AiImageGenerateRecordMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 请求只持久化待处理记录并提交独立工作池，返回记录 ID 后通过 getResult 轮询。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiImageGenerationService {
    private final AiImageGenerateRecordMapper records;
    private final AiImageModelResolver models;
    private final AiImageGenerationWorker worker;
    private final AiImageTaskExecutor executor;

    public Long generate(AiImageGenerateRecord request) {
        var caller = AgentCallerGuard.requireCaller();
        validate(request);
        var binding = models.require(request.getModelId());
        // 内部 Agent 仍沿用实体入口，但所有持久化字段都由服务端重建，不能信任输入身份/主键/状态。
        var record = new AiImageGenerateRecord();
        record.setTenantId(caller.getTenantId());
        record.setUserId(caller.getUserId());
        record.setModelId(binding.model().getId());
        record.setProviderId(binding.provider().getId());
        record.setPrompt(request.getPrompt());
        record.setNegativePrompt(request.getNegativePrompt());
        record.setSize(request.getSize() == null || request.getSize().isBlank() ? "1024x1024" : request.getSize());
        record.setStatus(AiImageGenerateStatus.PENDING.getCode());
        record.setDelFlag(0L);
        if (records.insert(record) != 1 || record.getId() == null) {
            throw new BusinessException("图像任务创建失败，请重试");
        }
        Long recordId = record.getId();
        try {
            executor.execute(() -> worker.run(recordId));
        } catch (RuntimeException ex) {
            log.warn("[AI Image] 任务提交失败, recordId={}, exceptionType={}",
                    recordId, ex.getClass().getSimpleName());
            worker.reject(record);
            throw new BusinessException("图像生成服务繁忙，请稍后重新发起");
        }
        return recordId;
    }

    public Page<AiImageGenerateRecord> page(Integer pageNum, Integer pageSize, Long userId, String status) {
        var caller = AgentCallerGuard.requireCaller();
        return records.selectRecordPage(new Page<>(pageNum, pageSize), caller.getTenantId(), userId, status);
    }

    public AiImageGenerateRecord getResult(Long id) {
        AgentCallerGuard.requireCaller();
        return AiImageGenerationWorker.requireOwned(records.selectById(id));
    }

    private void validate(AiImageGenerateRecord request) {
        if (request == null || request.getModelId() == null || request.getModelId() <= 0) {
            throw new BusinessException("请选择有效的图像模型");
        }
        if (request.getPrompt() == null || request.getPrompt().isBlank() || request.getPrompt().length() > 6000) {
            throw new BusinessException("提示词不能为空且不能超过 6000 字符");
        }
        if (request.getNegativePrompt() != null && request.getNegativePrompt().length() > 3000) {
            throw new BusinessException("负面提示词不能超过 3000 字符");
        }
        String size = request.getSize();
        if (size != null && !size.isBlank() && !size.matches("[1-9][0-9]{1,3}x[1-9][0-9]{1,3}")) {
            throw new BusinessException("图片尺寸格式无效");
        }
        if (size != null && !size.isBlank()) {
            String[] dimensions = size.split("x", 2);
            if (Integer.parseInt(dimensions[0]) > 4096 || Integer.parseInt(dimensions[1]) > 4096) {
                throw new BusinessException("图片尺寸不能超过 4096 像素");
            }
        }
    }
}
