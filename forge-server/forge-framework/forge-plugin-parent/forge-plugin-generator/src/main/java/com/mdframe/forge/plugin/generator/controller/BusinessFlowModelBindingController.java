package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowModelBindingsQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowModelBindingQueryService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingSummaryVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 与单模型绑定查询保持同一权限，批量入口不扩大模型可见范围。 */
@RestController
@RequestMapping("/ai/business/flow/model")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessFlowModelBindingController {
    private final BusinessFlowModelBindingQueryService queryService;

    @GetMapping("/business-bindings")
    @SaCheckPermission("ai:businessFlow:config")
    public RespInfo<List<BusinessBindingSummaryVO>> list(
            @Valid @ModelAttribute BusinessFlowModelBindingsQueryDTO query) {
        return RespInfo.success(queryService.list(query));
    }
}
