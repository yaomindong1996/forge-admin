package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.plugin.system.service.SystemVersionService;
import com.mdframe.forge.plugin.system.vo.SystemVersionVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/version")
@RequiredArgsConstructor
public class SystemVersionController {
    private final SystemVersionService service;

    /** 所有登录用户可查看非敏感版本信息；只免菜单权限，不免登录校验。 */
    @GetMapping
    @SaCheckLogin
    @ApiPermissionIgnore
    public RespInfo<SystemVersionVO> current(HttpServletResponse response) {
        StpUtil.checkLogin();
        response.setHeader("Cache-Control", "no-store");
        return RespInfo.success(service.current());
    }
}
