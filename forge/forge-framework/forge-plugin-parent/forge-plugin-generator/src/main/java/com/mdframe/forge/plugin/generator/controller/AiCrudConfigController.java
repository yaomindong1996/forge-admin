package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigDTO;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.dto.AiCrudGenerateRequest;
import com.mdframe.forge.plugin.generator.dto.AiCrudGenerateResult;
import com.mdframe.forge.plugin.generator.service.AiCrudCodegenService;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigGenerateService;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ai/crud-config")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class AiCrudConfigController {

    private final AiCrudConfigService crudConfigService;
    private final AiCrudConfigGenerateService generateService;
    private final AiCrudCodegenService codegenService;

    @GetMapping("/page")
    public RespInfo<Page<AiCrudConfig>> page(PageQuery pageQuery,
                                              @RequestParam(required = false) String configKey,
                                              @RequestParam(required = false) String tableName) {
        return RespInfo.success(crudConfigService.listPage(pageQuery, configKey, tableName));
    }

    @GetMapping("/{id}")
    public RespInfo<AiCrudConfig> getById(@PathVariable Long id) {
        return RespInfo.success(crudConfigService.getById(id));
    }

    @GetMapping("/by-key/{configKey}")
    public RespInfo<AiCrudConfig> getByConfigKey(@PathVariable String configKey) {
        return RespInfo.success(crudConfigService.getByConfigKey(configKey));
    }

    @GetMapping("/render/{configKey}")
    public RespInfo<AiCrudConfigRenderVO> render(@PathVariable String configKey) {
        return RespInfo.success(crudConfigService.getRenderConfig(configKey));
    }

    @PostMapping
    public RespInfo<Void> create(@RequestBody AiCrudConfigDTO dto) {
        crudConfigService.createConfig(dto);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> update(@RequestBody AiCrudConfigDTO dto) {
        crudConfigService.updateConfig(dto);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        crudConfigService.deleteConfig(id);
        return RespInfo.success();
    }

    @PostMapping("/ai/generate")
    public RespInfo<AiCrudGenerateResult> aiGenerate(@RequestBody AiCrudGenerateRequest request) {
        return RespInfo.success(generateService.generateFromDescription(request));
    }

    @PostMapping("/ai/generateFromTable")
    public RespInfo<AiCrudGenerateResult> aiGenerateFromTable(@RequestBody AiCrudGenerateRequest request) {
        return RespInfo.success(generateService.generateFromTable(request));
    }

    /**
     * 下载 CODEGEN 代码包（zip）
     */
    @GetMapping("/codegen/download/{configKey}")
    public void downloadCode(@PathVariable String configKey, HttpServletResponse response) throws Exception {
        byte[] zipBytes = codegenService.generateZip(configKey);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + configKey + "-code.zip\"");
        response.setContentLength(zipBytes.length);
        response.getOutputStream().write(zipBytes);
        response.getOutputStream().flush();
    }
}
