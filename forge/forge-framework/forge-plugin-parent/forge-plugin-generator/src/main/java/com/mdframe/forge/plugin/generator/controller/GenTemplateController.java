package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.GenTemplate;
import com.mdframe.forge.plugin.generator.service.IGenTemplateService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 代码生成模板管理Controller
 */
@RestController
@RequestMapping("/generator/template")
@RequiredArgsConstructor
public class GenTemplateController {

    private final IGenTemplateService genTemplateService;

    /**
     * 分页查询模板列表
     */
    @GetMapping("/list")
    @OperationLog(module = "模板管理", type = OperationType.QUERY, desc = "分页查询模板列表")
    public RespInfo<Page<GenTemplate>> list(PageQuery pageQuery, String templateName, String templateType, String templateEngine) {
        LambdaQueryWrapper<GenTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(templateName), GenTemplate::getTemplateName, templateName)
                .eq(StringUtils.isNotBlank(templateType), GenTemplate::getTemplateType, templateType)
                .eq(StringUtils.isNotBlank(templateEngine), GenTemplate::getTemplateEngine, templateEngine)
                .orderByAsc(GenTemplate::getSort)
                .orderByDesc(GenTemplate::getCreateTime);
        Page<GenTemplate> page = genTemplateService.page(pageQuery.toPage(), wrapper);
        return RespInfo.success(page);
    }

    /**
     * 查询所有启用的模板
     */
    @GetMapping("/enabled")
    public RespInfo<List<GenTemplate>> enabledList(String templateEngine) {
        LambdaQueryWrapper<GenTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GenTemplate::getIsEnabled, 1)
                .eq(StringUtils.isNotBlank(templateEngine), GenTemplate::getTemplateEngine, templateEngine)
                .orderByAsc(GenTemplate::getSort);
        List<GenTemplate> list = genTemplateService.list(wrapper);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询模板详情
     */
    @GetMapping("/{templateId}")
    public RespInfo<GenTemplate> getInfo(@PathVariable Long templateId) {
        GenTemplate template = genTemplateService.getById(templateId);
        return RespInfo.success(template);
    }

    /**
     * 新增模板
     */
    @PostMapping("/add")
    @OperationLog(module = "模板管理", type = OperationType.ADD, desc = "新增模板")
    public RespInfo<Void> add(@RequestBody GenTemplate template) {
        // 新增模板默认不是系统内置
        template.setIsSystem(0);
        genTemplateService.save(template);
        return RespInfo.success();
    }

    /**
     * 修改模板
     */
    @PostMapping("/edit")
    @OperationLog(module = "模板管理", type = OperationType.UPDATE, desc = "修改模板")
    public RespInfo<Void> edit(@RequestBody GenTemplate template) {
        // 系统内置模板不允许修改核心信息
        GenTemplate existTemplate = genTemplateService.getById(template.getTemplateId());
        if (existTemplate != null && existTemplate.getIsSystem() == 1) {
            // 系统内置只允许修改启用状态、排序和备注
            GenTemplate updateTemplate = new GenTemplate();
            updateTemplate.setTemplateId(template.getTemplateId());
            updateTemplate.setIsEnabled(template.getIsEnabled());
            updateTemplate.setSort(template.getSort());
            updateTemplate.setRemark(template.getRemark());
            genTemplateService.updateById(updateTemplate);
        } else {
            genTemplateService.updateById(template);
        }
        return RespInfo.success();
    }

    /**
     * 删除模板
     */
    @PostMapping("/remove/{templateId}")
    @OperationLog(module = "模板管理", type = OperationType.DELETE, desc = "删除模板")
    public RespInfo<Void> remove(@PathVariable Long templateId) {
        // 系统内置模板不允许删除
        GenTemplate template = genTemplateService.getById(templateId);
        if (template != null && template.getIsSystem() == 1) {
            return RespInfo.error("系统内置模板不允许删除");
        }
        genTemplateService.removeById(templateId);
        return RespInfo.success();
    }

    /**
     * 预览模板渲染结果
     */
    @PostMapping("/preview")
    public RespInfo<String> preview(@RequestBody Map<String, Long> params) {
        Long templateId = params.get("templateId");
        Long tableId = params.get("tableId");
        if (templateId == null || tableId == null) {
            return RespInfo.error("参数不完整");
        }
        String result = genTemplateService.previewTemplate(templateId, tableId);
        return RespInfo.success(result);
    }

    /**
     * 获取模板类型枚举
     */
    @GetMapping("/types")
    public RespInfo<List<String>> getTypes() {
        return RespInfo.success(genTemplateService.listTemplateTypes());
    }
}
