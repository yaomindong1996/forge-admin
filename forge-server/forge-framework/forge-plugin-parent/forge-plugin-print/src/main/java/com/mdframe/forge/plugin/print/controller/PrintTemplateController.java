package com.mdframe.forge.plugin.print.controller;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.service.*;
import com.mdframe.forge.plugin.print.vo.*;
import com.mdframe.forge.starter.core.annotation.crypto.*;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/print/templates")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class PrintTemplateController {

    private final PrintTemplateService templates;

    private final PrintTemplateVersionService versions;

    @GetMapping("/page")
    @SaCheckPermission("print:template:view")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询打印模板", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintTemplateVO.Page> page(@RequestParam Long applicationId, @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "20") int pageSize) {
        return RespInfo.success(templates.page(applicationId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("print:template:view")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询打印模板详情", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintTemplateVO> detail(@PathVariable Long id) {
        return RespInfo.success(templates.detail(id));
    }

    @PostMapping
    @SaCheckPermission("print:template:manage")
    @OperationLog(module = "打印", type = OperationType.ADD, desc = "新增打印模板", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintTemplateVO> create(@Valid @RequestBody PrintTemplateCreateDTO dto) {
        return RespInfo.success(templates.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("print:template:manage")
    @OperationLog(module = "打印", type = OperationType.UPDATE, desc = "保存打印草稿", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintTemplateVO> update(@PathVariable Long id, @Valid @RequestBody PrintTemplateUpdateDTO dto) {
        return RespInfo.success(templates.update(id, dto));
    }

    @PostMapping("/{id}/copy")
    @SaCheckPermission("print:template:manage")
    @OperationLog(module = "打印", type = OperationType.ADD, desc = "复制打印模板", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintTemplateVO> copy(@PathVariable Long id, @Valid @RequestBody PrintTemplateCopyDTO dto) {
        return RespInfo.success(templates.copy(id, dto));
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("print:template:manage")
    @OperationLog(module = "打印", type = OperationType.UPDATE, desc = "启停打印模板", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintTemplateVO> status(@PathVariable Long id, @Valid @RequestBody PrintTemplateStatusDTO dto) {
        return RespInfo.success(templates.status(id, dto));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("print:template:manage")
    @OperationLog(module = "打印", type = OperationType.DELETE, desc = "删除打印模板", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> delete(@PathVariable Long id, @RequestParam Long expectedRevision) {
        templates.delete(id, expectedRevision);
        return RespInfo.success();
    }

    @PostMapping("/{id}/publish")
    @SaCheckPermission("print:template:publish")
    @OperationLog(module = "打印", type = OperationType.UPDATE, desc = "发布打印模板", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintTemplateVersionService.Publication> publish(@PathVariable Long id, @Valid @RequestBody PrintTemplatePublishDTO dto) {
        return RespInfo.success(versions.publish(id, dto));
    }

    @GetMapping("/{id}/versions")
    @SaCheckPermission("print:template:view")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询打印版本", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<List<PrintVersionVO>> versions(@PathVariable Long id) {
        return RespInfo.success(versions.list(id));
    }

    @GetMapping("/{id}/versions/{versionId}")
    @SaCheckPermission("print:template:view")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询打印版本详情", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintVersionVO> version(@PathVariable Long id, @PathVariable Long versionId) {
        return RespInfo.success(versions.detail(id, versionId));
    }
}
