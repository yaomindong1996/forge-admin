package com.mdframe.forge.plugin.ai.routing.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.routing.*;
import com.mdframe.forge.plugin.ai.routing.dto.AiModelRoutePolicySaveDTO;
import com.mdframe.forge.plugin.ai.routing.dto.AiModelRoutePreviewDTO;
import com.mdframe.forge.plugin.ai.routing.service.AiModelRoutePolicyService;
import com.mdframe.forge.plugin.ai.routing.vo.AiModelRoutePolicyVO;
import com.mdframe.forge.plugin.ai.routing.vo.AiModelRoutePreviewVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/model-routing")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class AiModelRoutingController {
    private final AiModelRoutePolicyService policyService;
    private final AiModelRouter router;
    private final AiAgentService agentService;

    @GetMapping("/policy/page")
    @SaCheckPermission("ai:model-routing:list")
    public RespInfo<Page<AiModelRoutePolicyVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return RespInfo.success(policyService.pagePolicy(pageNum, pageSize, keyword, status));
    }

    @GetMapping("/policy/{id}")
    @SaCheckPermission("ai:model-routing:list")
    public RespInfo<AiModelRoutePolicyVO> detail(@PathVariable Long id) {
        return RespInfo.success(policyService.getPolicy(id));
    }

    @PostMapping("/policy")
    @SaCheckPermission("ai:model-routing:edit")
    public RespInfo<Long> create(@RequestBody AiModelRoutePolicySaveDTO dto) {
        return RespInfo.success(policyService.createPolicy(dto));
    }

    @PutMapping("/policy")
    @SaCheckPermission("ai:model-routing:edit")
    public RespInfo<Void> update(@RequestBody AiModelRoutePolicySaveDTO dto) {
        policyService.updatePolicy(dto);
        return RespInfo.success();
    }

    @DeleteMapping("/policy/{id}")
    @SaCheckPermission("ai:model-routing:edit")
    public RespInfo<Void> delete(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return RespInfo.success();
    }

    @PostMapping("/policy/preview")
    @SaCheckPermission("ai:model-routing:preview")
    public RespInfo<AiModelRoutePreviewVO> preview(@RequestBody AiModelRoutePreviewDTO dto) {
        AiAgent agent = agentService.getByCode(dto.getAgentCode());
        if (agent == null) throw new BusinessException("Agent 不存在或已停用");
        RouteDecision d = router.preview(new RouteRequest(SessionHelper.getTenantId(), agent, dto.getProviderId(), dto.getModelName()));
        AiModelRoutePreviewVO vo = new AiModelRoutePreviewVO();
        vo.setProviderId(d.provider().getId());
        vo.setProviderName(d.provider().getProviderName());
        vo.setModelId(d.model().getId());
        vo.setProviderModelId(d.model().getModelId());
        vo.setModelName(d.model().getModelName());
        vo.setSource(d.source().name());
        vo.setReason(d.reason().name());
        vo.setPolicyId(d.policyId());
        vo.setSkippedCandidates(d.skippedCandidates());
        return RespInfo.success(vo);
    }
}
