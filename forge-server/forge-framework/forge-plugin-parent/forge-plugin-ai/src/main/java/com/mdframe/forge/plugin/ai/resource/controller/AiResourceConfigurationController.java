package com.mdframe.forge.plugin.ai.resource.controller;

import cn.dev33.satoken.annotation.*;
import com.mdframe.forge.plugin.ai.resource.dto.AiKnowledgeResourceSaveDTO;
import com.mdframe.forge.plugin.ai.resource.service.AiKnowledgeResourceProfiles;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 统一 AI 资源配置控制器（社区版保留知识检索默认资源配置，外部文档解析服务配置留在企业版）。
 */
@RestController
@RequestMapping("/ai/resources")
@RequiredArgsConstructor
public class AiResourceConfigurationController {

    private final AiKnowledgeResourceProfiles retrieval;

    @GetMapping("/retrieval/defaults")
    @SaCheckPermission(value = {"ai:store:list", "ai:knowledge:list"}, mode = SaMode.OR)
    public RespInfo<AiKnowledgeResourceProfiles.View> retrievalDefaults() {
        return RespInfo.success(retrieval.current());
    }

    @PostMapping("/retrieval/defaults/save")
    @SaCheckPermission("ai:store:edit")
    @OperationLog(module = "AI知识检索资源", desc = "保存新知识库默认资源", saveRequestParams = false)
    public RespInfo<AiKnowledgeResourceProfiles.View> saveRetrievalDefaults(
            @Valid @RequestBody AiKnowledgeResourceSaveDTO dto) {
        return RespInfo.success(retrieval.save(dto));
    }
}
