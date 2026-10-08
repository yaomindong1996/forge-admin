package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.system.service.plugin.RuntimeLicenseStatusService;
import com.mdframe.forge.plugin.system.vo.RuntimeLicenseStatusVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 失效授权也必须能诊断；仍保留全实例平台管理员与 RBAC 边界。 */
@RestController
@RequestMapping("/system/plugin/runtime-license")
@RequiredArgsConstructor
@ApiEncrypt
public class RuntimeLicenseStatusController {
    private final RuntimeLicenseStatusService service;

    @GetMapping("/status")
    @SaCheckPermission("system:plugin:list")
    public RespInfo<RuntimeLicenseStatusVO> status(HttpServletResponse response) {
        SessionHelper.assertAdmin("只有平台超级管理员可以查看运行时授权");
        response.setHeader("Cache-Control", "no-store");
        return RespInfo.success(service.status());
    }
}
