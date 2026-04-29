package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowTemplate;
import com.mdframe.forge.starter.flow.service.FlowTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程模板管理接口
 */
@RestController
@RequestMapping("/api/flow/template")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowTemplateController {

    private final FlowTemplateService flowTemplateService;

    /**
     * 分页查询流程模板
     */
    @GetMapping("/page")
    public RespInfo<IPage<FlowTemplate>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        
        Page<FlowTemplate> page = new Page<>(pageNum, pageSize);
        IPage<FlowTemplate> result = flowTemplateService.pageTemplate(page, templateName, category, status);
        return RespInfo.success(result);
    }

    /**
     * 获取启用的模板列表
     */
    @GetMapping("/enabled")
    public RespInfo<List<FlowTemplate>> getEnabledTemplates(
            @RequestParam(required = false) String category) {
        List<FlowTemplate> templates = flowTemplateService.getEnabledTemplates(category);
        return RespInfo.success(templates);
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    public RespInfo<FlowTemplate> getById(@PathVariable String id) {
        FlowTemplate template = flowTemplateService.getTemplateDetail(id);
        return RespInfo.success(template);
    }

    /**
     * 根据Key获取模板详情
     */
    @GetMapping("/key/{templateKey}")
    public RespInfo<FlowTemplate> getByKey(@PathVariable String templateKey) {
        FlowTemplate template = flowTemplateService.getTemplateByKey(templateKey);
        return RespInfo.success(template);
    }

    /**
     * 创建模板
     */
    @PostMapping
    public RespInfo<FlowTemplate> create(@RequestBody FlowTemplate template) {
        FlowTemplate result = flowTemplateService.createTemplate(template);
        return RespInfo.success("创建成功", result);
    }

    /**
     * 更新模板
     */
    @PutMapping
    public RespInfo<FlowTemplate> update(@RequestBody FlowTemplate template) {
        FlowTemplate result = flowTemplateService.updateTemplate(template);
        return RespInfo.success("更新成功", result);
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable String id) {
        flowTemplateService.deleteTemplate(id);
        return RespInfo.success("删除成功", null);
    }

    /**
     * 启用模板
     */
    @PostMapping("/{id}/enable")
    public RespInfo<Void> enable(@PathVariable String id) {
        flowTemplateService.enableTemplate(id);
        return RespInfo.success("启用成功", null);
    }

    /**
     * 禁用模板
     */
    @PostMapping("/{id}/disable")
    public RespInfo<Void> disable(@PathVariable String id) {
        flowTemplateService.disableTemplate(id);
        return RespInfo.success("禁用成功", null);
    }

    /**
     * 从模板创建流程模型
     */
    @PostMapping("/createModel/{templateKey}")
    public RespInfo<String> createModel(
            @PathVariable String templateKey,
            @RequestParam String modelName,
            @RequestParam(required = false) String modelKey) {
        String modelId = flowTemplateService.createModelFromTemplate(templateKey, modelName, modelKey);
        return RespInfo.success("创建成功", modelId);
    }

    /**
     * 复制模板
     */
    @PostMapping("/copy/{id}")
    public RespInfo<FlowTemplate> copy(
            @PathVariable String id,
            @RequestParam String newName) {
        FlowTemplate template = flowTemplateService.copyTemplate(id, newName);
        return RespInfo.success("复制成功", template);
    }
}
