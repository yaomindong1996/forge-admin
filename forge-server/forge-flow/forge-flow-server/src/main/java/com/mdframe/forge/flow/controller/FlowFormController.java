package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.dto.FormFieldCatalogItemDTO;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import com.mdframe.forge.starter.flow.entity.FlowFormVersion;
import com.mdframe.forge.starter.flow.service.FlowFormService;
import com.mdframe.forge.starter.flow.vo.FlowFormPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程表单定义控制器
 * 
 * @author forge
 */
@RestController
@RequestMapping("/api/flow/form")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowFormController {

    private final FlowFormService flowFormService;

    /**
     * 获取表单定义分页列表
     */
    @GetMapping("/page")
    public RespInfo<FlowFormPageVO> getPage(
            @RequestParam(required = false) String formName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Integer currentPage = page != null ? page : pageNum;
        Page<FlowForm> pageResult = flowFormService.getPage(formName, status, currentPage, pageSize);
        
        FlowFormPageVO result = new FlowFormPageVO();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPage(pageResult.getCurrent());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        
        return RespInfo.success(result);
    }

    /**
     * 获取所有启用的表单定义
     */
    @GetMapping("/enabled")
    public RespInfo getEnabledForms() {
        return RespInfo.success(flowFormService.getEnabledForms());
    }

    /**
     * 获取表单定义详情
     */
    @GetMapping("/{id}")
    public RespInfo getById(@PathVariable Long id) {
        return RespInfo.success(flowFormService.getById(id));
    }

    /**
     * 根据表单Key获取表单定义
     */
    @GetMapping("/key/{formKey}")
    public RespInfo getByFormKey(@PathVariable String formKey) {
        return RespInfo.success(flowFormService.getByFormKey(formKey));
    }

    /**
     * 创建表单定义
     */
    @PostMapping
    public RespInfo create(@RequestBody FlowForm form) {
        flowFormService.createForm(form);
        return RespInfo.success();
    }

    /**
     * 更新表单定义
     */
    @PutMapping
    public RespInfo update(@RequestBody FlowForm form) {
        flowFormService.updateForm(form);
        return RespInfo.success();
    }

    /**
     * 删除表单定义
     */
    @DeleteMapping("/{id}")
    public RespInfo delete(@PathVariable Long id) {
        flowFormService.deleteForm(id);
        return RespInfo.success();
    }

    /**
     * 启用表单
     */
    @PostMapping("/{id}/enable")
    public RespInfo enable(@PathVariable Long id) {
        flowFormService.enableForm(id);
        return RespInfo.success();
    }

    /**
     * 禁用表单
     */
    @PostMapping("/{id}/disable")
    public RespInfo disable(@PathVariable Long id) {
        flowFormService.disableForm(id);
        return RespInfo.success();
    }

    /**
     * 复制表单
     */
    @PostMapping("/{id}/copy")
    public RespInfo copy(
            @PathVariable Long id,
            @RequestParam String newName) {
        Long newId = flowFormService.copyForm(id, newName);
        return RespInfo.success(newId);
    }

    /**
     * 检查表单Key是否存在
     */
    @GetMapping("/checkKey")
    public RespInfo checkKeyExists(
            @RequestParam String formKey,
            @RequestParam(required = false) Long excludeId) {
        return RespInfo.success(flowFormService.checkFormKeyExists(formKey, excludeId));
    }

    /**
     * 预览表单（获取表单Schema）
     */
    @GetMapping("/{id}/preview")
    public RespInfo preview(@PathVariable Long id) {
        FlowForm form = flowFormService.getById(id);
        return RespInfo.success(form != null ? form.getFormSchema() : null);
    }

    /**
     * 更新表单Schema
     */
    @PutMapping("/{id}/schema")
    public RespInfo updateSchema(
            @PathVariable Long id,
            @RequestBody String formSchema) {
        flowFormService.updateFormSchema(id, formSchema);
        return RespInfo.success();
    }

    /**
     * 获取表单Schema（通过Key）
     */
    @GetMapping("/schema/{formKey}")
    public RespInfo getSchemaByKey(@PathVariable String formKey) {
        return RespInfo.success(flowFormService.getFormSchema(formKey));
    }

    /**
     * 发布表单版本
     */
    @PostMapping("/{id}/publish")
    public RespInfo<FlowFormVersion> publish(@PathVariable Long id) {
        return RespInfo.success("发布成功", flowFormService.publishVersion(id));
    }

    /**
     * 查询表单版本列表
     */
    @GetMapping("/{id}/versions")
    public RespInfo<List<FlowFormVersion>> versions(@PathVariable Long id) {
        return RespInfo.success(flowFormService.listVersions(id));
    }

    /**
     * 查询表单字段目录
     */
    @GetMapping("/field-catalog")
    public RespInfo<List<FormFieldCatalogItemDTO>> fieldCatalog(
            @RequestParam(required = false) String formKey,
            @RequestParam(required = false) Long versionId) {
        return RespInfo.success(flowFormService.resolveFieldCatalog(formKey, versionId));
    }
}
