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
@RequestMapping("/print")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class PrintRuntimeController {

    private final PrintPrepareService runtime;

    private final PrintExecutionService executions;

    @PostMapping("/catalog")
    @SaCheckPermission(value = { "print:template:view", "print:execute" }, mode = SaMode.OR)
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询授权打印字段", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintFieldCatalogVO> catalog(@Valid @RequestBody PrintCatalogQueryDTO dto) {
        return RespInfo.success(runtime.catalog(dto));
    }

    @PostMapping("/available-templates")
    @SaCheckPermission("print:execute")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询可用打印模板", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<List<PrintAvailableTemplateVO>> available(@Valid @RequestBody PrintAvailableTemplatesDTO dto) {
        return RespInfo.success(runtime.available(dto));
    }

    @PostMapping("/prepare")
    @SaCheckPermission("print:execute")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "准备打印文档", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintContextVO> prepare(@Valid @RequestBody PrintPrepareDTO dto) {
        return RespInfo.success(runtime.prepare(dto));
    }

    @PostMapping("/executions/{id}/events")
    @SaCheckPermission("print:execute")
    @OperationLog(module = "打印", type = OperationType.UPDATE, desc = "记录打印会话事件", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintExecutionService.Event> event(@PathVariable Long id, @Valid @RequestBody PrintExecutionEventDTO dto) {
        return RespInfo.success(executions.record(id, dto));
    }
}
