package com.mdframe.forge.plugin.ai.invocation.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.invocation.dto.AiInvocationPageQuery;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationQueryService;
import com.mdframe.forge.plugin.ai.invocation.vo.AiInvocationLogVO;
import com.mdframe.forge.plugin.ai.invocation.vo.AiInvocationSummaryVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/model-routing/invocation")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class AiModelInvocationController {
    private final AiModelInvocationQueryService service;

    @GetMapping("/page")
    @SaCheckPermission("ai:model-invocation:list")
    public RespInfo<Page<AiInvocationLogVO>> page(AiInvocationPageQuery query) {
        return RespInfo.success(service.page(query));
    }

    @GetMapping("/summary")
    @SaCheckPermission("ai:model-invocation:list")
    public RespInfo<AiInvocationSummaryVO> summary(AiInvocationPageQuery query) {
        return RespInfo.success(service.summarize(query));
    }
}
