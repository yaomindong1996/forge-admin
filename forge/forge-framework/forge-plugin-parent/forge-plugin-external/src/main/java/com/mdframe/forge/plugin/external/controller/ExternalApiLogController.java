package com.mdframe.forge.plugin.external.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.external.dto.ExternalApiLogQuery;
import com.mdframe.forge.plugin.external.entity.ExternalApiLog;
import com.mdframe.forge.plugin.external.service.ExternalApiLogService;
import com.mdframe.forge.plugin.external.vo.ExternalApiLogSummary;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/external/api/log")
@RequiredArgsConstructor
@ApiEncrypt
@ApiDecrypt
public class ExternalApiLogController {

    private final ExternalApiLogService logService;

    @GetMapping("/page")
    public RespInfo<IPage<ExternalApiLog>> page(ExternalApiLogQuery query) {
        return RespInfo.success(logService.page(query));
    }

    @GetMapping("/summary")
    public RespInfo<ExternalApiLogSummary> summary(ExternalApiLogQuery query) {
        return RespInfo.success(logService.summary(query));
    }

    @GetMapping("/{id}")
    public RespInfo<ExternalApiLog> getById(@PathVariable Long id) {
        return RespInfo.success(logService.getById(id));
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> remove(@PathVariable Long id) {
        logService.removeById(id);
        return RespInfo.success();
    }

    @DeleteMapping("/clear")
    public RespInfo<Integer> clear(ExternalApiLogQuery query) {
        return RespInfo.success(logService.clearLogs(query));
    }
}
