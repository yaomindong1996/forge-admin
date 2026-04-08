package com.mdframe.forge.starter.core.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.context.DemoProperties;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system/demo")
@RequiredArgsConstructor
public class DemoController {
    
    private final DemoProperties demoProperties;
    
    @GetMapping("/status")
    @SaIgnore
    @IgnoreTenant
    public RespInfo<Map<String, Object>> getDemoStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", demoProperties.getEnabled());
        result.put("message", demoProperties.getMessage());
        return RespInfo.success(result);
    }
}
