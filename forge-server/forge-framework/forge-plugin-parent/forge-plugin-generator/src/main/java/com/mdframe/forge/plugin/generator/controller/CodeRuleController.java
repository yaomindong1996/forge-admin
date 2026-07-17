package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRule;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleGenerateDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRulePreviewDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.CodeRuleService;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRulePreviewVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleTokenVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 通用编码规则接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/code-rule")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class CodeRuleController {

    private final CodeRuleService codeRuleService;

    @GetMapping("/page")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "编码规则", type = OperationType.QUERY, desc = "分页查询编码规则")
    public RespInfo<Page<AiCodeRule>> page(PageQuery pageQuery,
                                           @RequestParam(required = false) String ruleCode,
                                           @RequestParam(required = false) String ruleName,
                                           @RequestParam(required = false) String scene,
                                           @RequestParam(required = false) Integer status) {
        return RespInfo.success(codeRuleService.page(
                pageQuery.getPageNum(), pageQuery.getPageSize(), ruleCode, ruleName, scene, status));
    }

    @GetMapping("/list")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "编码规则", type = OperationType.QUERY, desc = "查询可用编码规则")
    public RespInfo<List<AiCodeRule>> list(
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String objectCode) {
        return RespInfo.success(codeRuleService.listEnabled(scene, objectCode));
    }

    @GetMapping("/tokens")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "编码规则", type = OperationType.QUERY, desc = "查询编码规则变量")
    public RespInfo<List<CodeRuleTokenVO>> tokens() {
        return RespInfo.success(codeRuleService.listTokens());
    }

    @PostMapping("/preview")
    @SaCheckPermission("ai:businessObject:design")
    public RespInfo<CodeRulePreviewVO> preview(@Valid @RequestBody CodeRulePreviewDTO dto) {
        return RespInfo.success(codeRuleService.preview(dto));
    }

    @PostMapping("/generate")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "编码规则", type = OperationType.OTHER, desc = "生成编码样例")
    public RespInfo<Map<String, String>> generate(@Valid @RequestBody CodeRuleGenerateDTO dto) {
        CodeRuleGenerateDTO request = dto == null ? new CodeRuleGenerateDTO() : dto;
        Map<String, Object> fields = request.getFields() == null ? request.getContext() : request.getFields();
        return RespInfo.success(Map.of("code", codeRuleService.generate(request.getRuleCode(), fields)));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "编码规则", type = OperationType.QUERY, desc = "查询编码规则详情")
    public RespInfo<AiCodeRule> detail(@PathVariable Long id) {
        return RespInfo.success(codeRuleService.detail(id));
    }

    @PostMapping
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "编码规则", type = OperationType.ADD, desc = "新增编码规则")
    public RespInfo<Void> create(@RequestBody AiCodeRule rule) {
        codeRuleService.create(rule);
        return RespInfo.success();
    }

    @PutMapping
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "编码规则", type = OperationType.UPDATE, desc = "修改编码规则")
    public RespInfo<Void> update(@RequestBody AiCodeRule rule) {
        codeRuleService.update(rule);
        return RespInfo.success();
    }

    @PutMapping("/{id}/status/{status}")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "编码规则", type = OperationType.UPDATE, desc = "启停编码规则")
    public RespInfo<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        codeRuleService.updateStatus(id, status);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "编码规则", type = OperationType.DELETE, desc = "删除编码规则")
    public RespInfo<Void> delete(@PathVariable Long id) {
        codeRuleService.delete(id);
        return RespInfo.success();
    }
}
