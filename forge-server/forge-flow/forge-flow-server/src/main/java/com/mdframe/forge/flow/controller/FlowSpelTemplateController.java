package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.flow.dto.FlowSpelTemplateCreateDTO;
import com.mdframe.forge.flow.dto.FlowSpelTemplateUpdateDTO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import com.mdframe.forge.starter.flow.service.FlowSpelTemplateService;
import com.mdframe.forge.starter.flow.vo.FlowSpelTemplatePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SPEL表达式模板控制器
 *
 * @author forge
 */
@RestController
@RequestMapping("/api/flow/spelTemplate")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowSpelTemplateController {

    private final FlowSpelTemplateService spelTemplateService;

    /**
     * 分页查询模板
     */
    @GetMapping("/page")
    public RespInfo<FlowSpelTemplatePageVO> getPage(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<FlowSpelTemplate> pageResult = spelTemplateService.getPage(templateName, category, status, pageNum, pageSize);

        FlowSpelTemplatePageVO result = new FlowSpelTemplatePageVO();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPage(pageResult.getCurrent());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());

        return RespInfo.success(result);
    }

    /**
     * 获取启用状态的模板列表（节点配置下拉用）
     */
    @GetMapping("/list")
    public RespInfo getEnabledList() {
        List<FlowSpelTemplate> list = spelTemplateService.getEnabledList();
        return RespInfo.success(list);
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    public RespInfo getById(@PathVariable Long id) {
        return RespInfo.success(spelTemplateService.getByIdForCurrentTenant(id));
    }

    /**
     * 创建模板
     */
    @PostMapping
    public RespInfo create(@RequestBody FlowSpelTemplateCreateDTO request) {
        FlowSpelTemplate template = new FlowSpelTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setTemplateCode(request.getTemplateCode());
        template.setExpression(request.getExpression());
        template.setDescription(request.getDescription());
        template.setCategory(request.getCategory());
        template.setExampleParams(request.getExampleParams());
        template.setStatus(request.getStatus());
        template.setSort(request.getSort());
        template.setRemark(request.getRemark());
        spelTemplateService.createTemplate(template);
        return RespInfo.success();
    }

    /**
     * 更新模板
     */
    @PutMapping
    public RespInfo update(@RequestBody FlowSpelTemplateUpdateDTO request) {
        FlowSpelTemplate template = new FlowSpelTemplate();
        template.setId(request.getId());
        template.setTemplateName(request.getTemplateName());
        template.setExpression(request.getExpression());
        template.setDescription(request.getDescription());
        template.setCategory(request.getCategory());
        template.setExampleParams(request.getExampleParams());
        template.setStatus(request.getStatus());
        template.setSort(request.getSort());
        template.setRemark(request.getRemark());
        spelTemplateService.updateTemplate(template);
        return RespInfo.success();
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public RespInfo delete(@PathVariable Long id) {
        spelTemplateService.deleteTemplate(id);
        return RespInfo.success();
    }

    /**
     * 启用模板
     */
    @PostMapping("/{id}/enable")
    public RespInfo enable(@PathVariable Long id) {
        spelTemplateService.enableTemplate(id);
        return RespInfo.success();
    }

    /**
     * 禁用模板
     */
    @PostMapping("/{id}/disable")
    public RespInfo disable(@PathVariable Long id) {
        spelTemplateService.disableTemplate(id);
        return RespInfo.success();
    }
}
