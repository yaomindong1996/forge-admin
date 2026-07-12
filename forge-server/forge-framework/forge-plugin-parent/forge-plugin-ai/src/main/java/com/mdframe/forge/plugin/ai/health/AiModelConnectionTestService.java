package com.mdframe.forge.plugin.ai.health;

import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderFailureDiagnostics;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class AiModelConnectionTestService {
    private final AiModelMapper modelMapper;
    private final AiProviderService providerService;
    private final AiProviderAdapterRegistry adapterRegistry;
    private final AiModelHealthRegistry healthRegistry;
    private final AiModelFailureClassifier failureClassifier;

    public String test(Long modelPk) {
        AiModel model = modelMapper.selectEnabledById(modelPk);
        if (model == null) throw new BusinessException("模型不存在或已停用");
        AiProvider provider = providerService.getById(model.getProviderId());
        if (provider == null || !"0".equals(provider.getStatus())) throw new BusinessException("模型供应商不存在或已停用");
        Long tenantId = model.getTenantId() != null ? model.getTenantId() : SessionHelper.getTenantId();
        if (tenantId == null) throw new BusinessException("无法确定当前模型所属租户");
        AiModelHealthKey key = new AiModelHealthKey(tenantId, provider.getId(), model.getId());
        AiModelHealthLease lease = healthRegistry.acquireManualProbe(key);
        try {
            adapterRegistry.createChatModel(provider, new AiModelRuntimeOptions(model.getModelId(), 0D, 32))
                    .call(new Prompt(List.of(new UserMessage("请只回复 OK"))));
            lease.success();
            log.info("[AI模型测试] 连接成功, providerId={}, modelId={}", provider.getId(), model.getId());
            return "连接成功";
        } catch (Exception e) {
            AiModelFailureCategory category = failureClassifier.classify(e);
            if (category == AiModelFailureCategory.VALIDATION
                    || category == AiModelFailureCategory.CONTENT_POLICY
                    || category == AiModelFailureCategory.CANCELLED) {
                lease.cancel();
            } else {
                lease.failure(category);
            }
            AiProviderFailureDiagnostics d = AiProviderFailureDiagnostics.from(e);
            log.warn("[AI模型测试] 连接失败, providerId={}, modelId={}, category={}, httpStatus={}, errorCode={}, exceptionType={}",
                    provider.getId(), model.getId(), category, d.httpStatus(), d.errorCode(), e.getClass().getSimpleName());
            throw new BusinessException("模型连接失败，请检查供应商配置和模型标识");
        } finally {
            lease.close();
        }
    }
}
