package com.mdframe.forge.starter.excel.controller;

import com.mdframe.forge.starter.excel.core.DynamicExportEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 通用Excel导出接口
 * 通过配置键和查询参数动态导出，无需编写业务代码
 */
@Slf4j
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "forge.excel.enable-generic-export", havingValue = "true", matchIfMissing = true)
public class GenericExportController {

    private final DynamicExportEngine dynamicExportEngine;

    /**
     * 通用导出接口
     *
     * @param configKey   配置键（必填）
     * @param queryParams 查询参数（可选）
     * @param response    响应对象
     */
    @PostMapping("/export/{configKey}")
    public void export(@PathVariable String configKey,
                       @RequestBody(required = false) Map<String, Object> queryParams,
                       HttpServletResponse response) {
        log.info("通用导出接口调用: configKey={}, params={}", configKey, queryParams);
        dynamicExportEngine.export(response, configKey, queryParams);
    }

    /**
     * GET方式导出（适用于简单查询参数）
     */
    @GetMapping("/export/{configKey}")
    public void exportByGet(@PathVariable String configKey,
                            @RequestParam(required = false) Map<String, Object> queryParams,
                            HttpServletResponse response) {
        log.info("通用导出接口调用(GET): configKey={}, params={}", configKey, queryParams);
        dynamicExportEngine.export(response, configKey, queryParams);
    }
}
