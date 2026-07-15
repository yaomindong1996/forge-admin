package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigDTO;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.dto.AiCrudGenerateRequest;
import com.mdframe.forge.plugin.generator.dto.AiCrudGenerateResult;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigGenerateService;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectDesignerService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeCodegenService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletResponse;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
    private final LowcodeCodegenService lowcodeCodegenService;
    private final BusinessObjectService businessObjectService;
    private final BusinessObjectDesignerService businessObjectDesignerService;

    @GetMapping("/page")
    @SaCheckPermission("ai:crud-config:list")
    public RespInfo<Page<AiCrudConfig>> page(PageQuery pageQuery,
                                              @RequestParam(required = false) String configKey,
                                              @RequestParam(required = false) String tableName) {
        return RespInfo.success(crudConfigService.listPage(pageQuery, configKey, tableName));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("ai:crud-config:list")
    public RespInfo<AiCrudConfig> getById(@PathVariable Long id) {
        return RespInfo.success(crudConfigService.getById(id));
    }

    @GetMapping("/by-key/{configKey}")
    @SaCheckPermission("ai:crud-config:list")
    public RespInfo<AiCrudConfig> getByConfigKey(@PathVariable String configKey) {
        return RespInfo.success(crudConfigService.getByConfigKey(configKey));
    }

    @GetMapping("/render/{configKey}")
    public RespInfo<AiCrudConfigRenderVO> render(
            @PathVariable String configKey,
            @RequestParam(defaultValue = "false") boolean designPreview) {
        if (designPreview) {
            crudConfigService.assertDesignPreviewPermission();
            var businessObject = businessObjectService.findByConfigKey(configKey);
            if (businessObject != null) {
                businessObjectDesignerService.prepareRuntimeDraft(businessObject.getId());
            }
        }
        return RespInfo.success(crudConfigService.getRenderConfig(configKey, designPreview));
    }

    @PostMapping
    @SaCheckPermission("ai:crud-config:list")
    public RespInfo<Void> create(@RequestBody AiCrudConfigDTO dto) {
        crudConfigService.createConfig(dto);
        return RespInfo.success();
    }

    @PutMapping
    @SaCheckPermission("ai:crud-config:list")
    public RespInfo<Void> update(@RequestBody AiCrudConfigDTO dto) {
        crudConfigService.updateConfig(dto);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:crud-config:list")
    public RespInfo<Void> delete(@PathVariable Long id) {
        crudConfigService.deleteConfig(id);
        return RespInfo.success();
    }

    @PostMapping("/ai/generate")
    @SaCheckPermission("ai:crud-config:list")
    public RespInfo<AiCrudGenerateResult> aiGenerate(@RequestBody AiCrudGenerateRequest request) {
        return RespInfo.success(generateService.generateFromDescription(request));
    }

    @PostMapping("/ai/generateFromTable")
    @SaCheckPermission("ai:crud-config:list")
    public RespInfo<AiCrudGenerateResult> aiGenerateFromTable(@RequestBody AiCrudGenerateRequest request) {
        return RespInfo.success(generateService.generateFromTable(request));
    }

    /**
     * 下载 CODEGEN 代码包（zip）
     */
    @GetMapping("/codegen/download/{configKey}")
    @SaCheckPermission("ai:crud-config:list")
    public void downloadCode(@PathVariable String configKey, HttpServletResponse response) throws Exception {
        log.warn("[AiCrudConfigController] 旧代码下载入口已废弃, configKey={}, 请使用 /ai/business/app/{{id}}/code/download", configKey);
        byte[] zipBytes = lowcodeCodegenService.downloadByConfigKey(configKey);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + configKey + "-code.zip\"");
        response.setContentLength(zipBytes.length);
        response.getOutputStream().write(zipBytes);
        response.getOutputStream().flush();
    }
}
