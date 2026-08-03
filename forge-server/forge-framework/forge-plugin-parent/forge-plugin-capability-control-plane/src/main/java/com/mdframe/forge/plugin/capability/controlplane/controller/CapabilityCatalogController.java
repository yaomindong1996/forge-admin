package com.mdframe.forge.plugin.capability.controlplane.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityOpenApiDocumentService;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCallGuideService;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityCallGuideVO;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityClientVO;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityVersionDraftVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/ai/capability")
@RequiredArgsConstructor
public class CapabilityCatalogController {

    private final CapabilityCatalogService catalogService;
    private final CapabilityOpenApiDocumentService openApiDocumentService;
    private final CapabilityCallGuideService callGuideService;

    @GetMapping("/page")
    @SaCheckPermission("ai:capability:query")
    @OperationLog(module = "AI中枢能力", type = OperationType.QUERY, desc = "分页查询能力目录")
    public RespInfo<Page<AiCapability>> page(
            PageQuery pageQuery,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String publishStatus) {
        return RespInfo.success(catalogService.page(
                SessionHelper.getTenantId(), pageQuery, keyword, publishStatus));
    }

    @PostMapping("/getById")
    @SaCheckPermission("ai:capability:query")
    @OperationLog(module = "AI中枢能力", type = OperationType.QUERY, desc = "查询能力详情")
    public RespInfo<AiCapability> getById(@RequestParam Long id) {
        return RespInfo.success(catalogService.getById(SessionHelper.getTenantId(), id));
    }

    @GetMapping("/{id}/version-draft")
    @SaCheckPermission("ai:capability:query")
    @OperationLog(module = "AI中枢能力", type = OperationType.QUERY, desc = "查询能力新版本草稿")
    public RespInfo<CapabilityVersionDraftVO> versionDraft(@PathVariable Long id) {
        return RespInfo.success(catalogService.versionDraft(SessionHelper.getTenantId(), id));
    }

    @GetMapping("/{id}/openapi")
    @SaCheckPermission("ai:capability:query")
    @OperationLog(
            module = "AI中枢能力",
            type = OperationType.EXPORT,
            desc = "下载单能力OpenAPI文档",
            saveResponseResult = false)
    public ResponseEntity<byte[]> downloadOpenApi(@PathVariable Long id) {
        CapabilityOpenApiDocumentService.CapabilityOpenApiDocument document =
                openApiDocumentService.generate(SessionHelper.getTenantId(), id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(document.content());
    }

    @GetMapping("/{id}/document")
    @SaCheckPermission("ai:capability:query")
    @OperationLog(
            module = "AI中枢能力",
            type = OperationType.EXPORT,
            desc = "下载单能力Markdown调用文档",
            saveResponseResult = false)
    public ResponseEntity<byte[]> downloadMarkdown(@PathVariable Long id) {
        CapabilityOpenApiDocumentService.CapabilityMarkdownDocument document =
                openApiDocumentService.generateMarkdown(SessionHelper.getTenantId(), id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(document.content());
    }

    @GetMapping("/{id}/call-guide")
    @SaCheckPermission("ai:capability:query")
    @OperationLog(module = "AI中枢能力", type = OperationType.QUERY, desc = "查询客户端调用指南")
    public RespInfo<CapabilityCallGuideVO> callGuide(
            @PathVariable Long id,
            @RequestParam Long clientId) {
        return RespInfo.success(callGuideService.guide(
                SessionHelper.getTenantId(), id, clientId));
    }

    @GetMapping("/call-guide/clients")
    @SaCheckPermission("ai:capability:query")
    @OperationLog(module = "AI中枢能力", type = OperationType.QUERY, desc = "查询调用指南客户端")
    public RespInfo<List<CapabilityClientVO>> callGuideClients() {
        return RespInfo.success(callGuideService.clients(SessionHelper.getTenantId()));
    }

    @PostMapping("/publish")
    @SaCheckPermission("ai:capability:publish")
    @OperationLog(module = "AI中枢能力", type = OperationType.ADD, desc = "发布能力版本")
    @ApiDecrypt
    public RespInfo<Long> publish(@Valid @RequestBody CapabilityPublishDTO dto) {
        return RespInfo.success(catalogService.publish(SessionHelper.getTenantId(), dto));
    }

    @PostMapping("/disable/{id}")
    @SaCheckPermission("ai:capability:publish")
    @OperationLog(module = "AI中枢能力", type = OperationType.UPDATE, desc = "停用能力")
    public RespInfo<Void> disable(@PathVariable Long id) {
        catalogService.disable(SessionHelper.getTenantId(), id);
        return RespInfo.success();
    }

    @PostMapping("/enable/{id}")
    @SaCheckPermission("ai:capability:publish")
    @OperationLog(module = "AI中枢能力", type = OperationType.UPDATE, desc = "重新启用能力")
    public RespInfo<Void> enable(@PathVariable Long id) {
        catalogService.enable(SessionHelper.getTenantId(), id);
        return RespInfo.success();
    }
}
