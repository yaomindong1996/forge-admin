package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationBundleImportService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationBundleImportResultVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 应用调试包导入入口；multipart 不走通用请求体解密。
 */
@RestController
@RequestMapping("/ai/business/application")
@RequiredArgsConstructor
public class BusinessApplicationBundleController {

    private final BusinessApplicationBundleImportService bundleImportService;

    @PostMapping("/debug-bundle/import")
    @SaCheckPermission("ai:businessApplication:add")
    @ApiEncrypt
    @OperationLog(module = "业务应用", type = OperationType.ADD, desc = "导入应用调试包")
    public RespInfo<BusinessApplicationBundleImportResultVO> importDebugBundle(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "autoPublish", defaultValue = "false") boolean autoPublish) {
        return RespInfo.success(bundleImportService.importBundle(file, autoPublish));
    }
}
