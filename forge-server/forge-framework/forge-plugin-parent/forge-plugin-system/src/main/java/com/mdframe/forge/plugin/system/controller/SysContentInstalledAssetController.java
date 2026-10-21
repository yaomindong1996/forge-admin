package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.ContentInstalledAssetInstallDTO;
import com.mdframe.forge.plugin.system.service.ISysContentInstalledAssetService;
import com.mdframe.forge.plugin.system.vo.ContentInstalledAssetVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户已安装内容资产（打印/表单等模板库）。
 */
@RestController
@RequestMapping("/system/content-assets")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SysContentInstalledAssetController {

    private final ISysContentInstalledAssetService contentAssets;

    @GetMapping("/page")
    @SaCheckPermission(value = {
            "system:contentAsset:view",
            "print:template:view",
            "print:template:manage",
            "ai:businessApplication:runtime"
    }, mode = SaMode.OR)
    @OperationLog(module = "内容资产", type = OperationType.QUERY, desc = "分页查询已安装模板", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Page<ContentInstalledAssetVO>> page(@RequestParam String kind,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "20") int pageSize) {
        return RespInfo.success(contentAssets.page(kind, keyword, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(value = {
            "system:contentAsset:view",
            "print:template:view",
            "print:template:manage",
            "ai:businessApplication:runtime"
    }, mode = SaMode.OR)
    @OperationLog(module = "内容资产", type = OperationType.QUERY, desc = "查询已安装模板详情", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<ContentInstalledAssetVO> detail(@PathVariable Long id) {
        return RespInfo.success(contentAssets.detail(id));
    }

    @PostMapping
    @SaCheckPermission(value = {
            "system:contentAsset:manage",
            "print:template:manage",
            "ai:businessApplication:runtime"
    }, mode = SaMode.OR)
    @OperationLog(module = "内容资产", type = OperationType.ADD, desc = "安装模板到我的库", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<ContentInstalledAssetVO> install(@Valid @RequestBody ContentInstalledAssetInstallDTO dto) {
        return RespInfo.success(contentAssets.install(dto));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(value = {
            "system:contentAsset:manage",
            "print:template:manage",
            "ai:businessApplication:runtime"
    }, mode = SaMode.OR)
    @OperationLog(module = "内容资产", type = OperationType.DELETE, desc = "卸载已安装模板", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> remove(@PathVariable Long id) {
        contentAssets.remove(id);
        return RespInfo.success();
    }
}
