package com.mdframe.forge.plugin.print.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceCreateDTO;
import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceQueryDTO;
import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceStatusDTO;
import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceUpdateDTO;
import com.mdframe.forge.plugin.print.service.PrintBusinessSourceService;
import com.mdframe.forge.plugin.print.vo.PrintBusinessSourceVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/print/sources")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class PrintBusinessSourceController {

    private final PrintBusinessSourceService sources;

    @GetMapping("/page")
    @SaCheckPermission("print:source:view")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询打印业务来源",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintBusinessSourceVO.Page> page(@Valid @ModelAttribute PrintBusinessSourceQueryDTO query) {
        return RespInfo.success(sources.page(query));
    }

    @GetMapping("/options")
    @SaCheckPermission("print:source:view")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询可用打印来源",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<List<PrintBusinessSourceVO.Option>> options() {
        return RespInfo.success(sources.options());
    }

    @GetMapping("/resolve/{sourceCode}")
    @SaCheckPermission("print:execute")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "解析运行时打印来源",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintBusinessSourceVO.Option> resolveRuntime(@PathVariable String sourceCode) {
        return RespInfo.success(sources.resolveRuntime(sourceCode));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("print:source:view")
    @OperationLog(module = "打印", type = OperationType.QUERY, desc = "查询打印来源详情",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintBusinessSourceVO> detail(@PathVariable Long id) {
        return RespInfo.success(sources.detail(id));
    }

    @PostMapping
    @SaCheckPermission("print:source:manage")
    @OperationLog(module = "打印", type = OperationType.ADD, desc = "新增打印业务来源",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintBusinessSourceVO> create(@Valid @RequestBody PrintBusinessSourceCreateDTO dto) {
        return RespInfo.success(sources.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("print:source:manage")
    @OperationLog(module = "打印", type = OperationType.UPDATE, desc = "修改打印业务来源",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintBusinessSourceVO> update(@PathVariable Long id,
                                                  @Valid @RequestBody PrintBusinessSourceUpdateDTO dto) {
        return RespInfo.success(sources.update(id, dto));
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("print:source:manage")
    @OperationLog(module = "打印", type = OperationType.UPDATE, desc = "启停打印业务来源",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PrintBusinessSourceVO> status(@PathVariable Long id,
                                                  @Valid @RequestBody PrintBusinessSourceStatusDTO dto) {
        return RespInfo.success(sources.status(id, dto));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("print:source:manage")
    @OperationLog(module = "打印", type = OperationType.DELETE, desc = "删除打印业务来源",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> delete(@PathVariable Long id, @RequestParam Long expectedRevision) {
        sources.delete(id, expectedRevision);
        return RespInfo.success();
    }
}
