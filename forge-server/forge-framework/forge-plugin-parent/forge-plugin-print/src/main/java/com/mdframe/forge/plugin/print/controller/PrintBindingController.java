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
@RequestMapping("/print/bindings")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class PrintBindingController {

    private final PrintBindingService bindings;

    @GetMapping
    @SaCheckPermission("print:template:view")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询打印绑定", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<List<PrintBindingService.Binding>> list(@Valid @ModelAttribute PrintBindingQueryDTO dto) {
        return RespInfo.success(bindings.list(dto));
    }

    @PutMapping
    @SaCheckPermission("print:template:manage")
    @OperationLog(module = "打印", type = OperationType.UPDATE, desc = "保存打印绑定", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintBindingService.Binding> save(@Valid @RequestBody PrintBindingSaveDTO dto) {
        return RespInfo.success(bindings.save(dto));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("print:template:manage")
    @OperationLog(module = "打印", type = OperationType.DELETE, desc = "解除打印绑定", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> delete(@PathVariable Long id, @RequestParam Long expectedRevision) {
        bindings.delete(id, expectedRevision);
        return RespInfo.success();
    }
}
