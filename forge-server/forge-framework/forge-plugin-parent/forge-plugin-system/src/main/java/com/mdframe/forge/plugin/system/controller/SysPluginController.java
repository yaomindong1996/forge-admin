package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.system.dto.SysPluginQuery;
import com.mdframe.forge.plugin.system.service.SysPluginService;
import com.mdframe.forge.plugin.system.vo.SysPluginPageVO;
import com.mdframe.forge.plugin.system.vo.SysPluginVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 全实例目录仅平台管理员可查，租户管理员即使获得 RBAC 编码仍不放行。 */
@RestController
@RequestMapping("/system/plugin")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SysPluginController {
    private final SysPluginService service;

    @GetMapping("/page")
    @SaCheckPermission("system:plugin:list")
    public RespInfo<SysPluginPageVO> page(@Validated SysPluginQuery query) {
        SessionHelper.assertAdmin("只有平台超级管理员可以查看插件中心");
        return RespInfo.success(service.page(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:plugin:detail")
    public RespInfo<SysPluginVO> detail(@PathVariable String id) {
        SessionHelper.assertAdmin("只有平台超级管理员可以查看插件中心");
        return RespInfo.success(service.detail(id));
    }
}
