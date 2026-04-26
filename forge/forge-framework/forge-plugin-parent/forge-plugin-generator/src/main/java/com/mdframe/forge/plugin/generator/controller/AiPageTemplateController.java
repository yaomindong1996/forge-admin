package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiPageTemplate;
import com.mdframe.forge.plugin.generator.dto.AiPageTemplateDTO;
import com.mdframe.forge.plugin.generator.service.AiPageTemplateService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai/page-template")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class AiPageTemplateController {

    private final AiPageTemplateService pageTemplateService;

    /**
     * 分页查询（管理后台用）
     */
    @GetMapping("/page")
    public RespInfo<Page<AiPageTemplate>> page(PageQuery pageQuery,
                                               @RequestParam(required = false) String templateKey,
                                               @RequestParam(required = false) String templateName) {
        return RespInfo.success(pageTemplateService.listPage(pageQuery, templateKey, templateName));
    }

    /**
     * 查询所有启用模板（前端生成器选择用）
     */
    @GetMapping("/list")
    public RespInfo<List<AiPageTemplate>> listEnabled() {
        return RespInfo.success(pageTemplateService.listEnabled());
    }

    /**
     * 根据 templateKey 查询单个（crud-page 渲染时用）
     */
    @GetMapping("/by-key/{templateKey}")
    public RespInfo<AiPageTemplate> getByTemplateKey(@PathVariable String templateKey) {
        return RespInfo.success(pageTemplateService.getByTemplateKey(templateKey));
    }

    @GetMapping("/{id}")
    public RespInfo<AiPageTemplate> getById(@PathVariable Long id) {
        return RespInfo.success(pageTemplateService.getById(id));
    }

    @PostMapping
    public RespInfo<Void> create(@RequestBody AiPageTemplateDTO dto) {
        pageTemplateService.createTemplate(dto);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> update(@RequestBody AiPageTemplateDTO dto) {
        pageTemplateService.updateTemplate(dto);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        pageTemplateService.deleteTemplate(id);
        return RespInfo.success();
    }
}
