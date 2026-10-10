package com.mdframe.forge.plugin.hello.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.hello.vo.HelloPluginInfoVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptor;
import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import com.mdframe.forge.starter.plugin.feature.RequiresFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 业务组件由宿主的包扫描装配；RBAC 与功能使用权必须同时满足。 */
@RestController
@RequestMapping("/plugin/hello")
@RequiresFeature("community.hello")
public class HelloPluginController {

    private static final Logger LOG = LoggerFactory.getLogger(HelloPluginController.class);
    private static final String PLUGIN_ID = "hello";

    private final PluginDescriptor descriptor;

    public HelloPluginController(PluginRegistry registry) {
        // 缺少运行描述是装配错误，不能伪造版本或在接口中降级成“安装成功”。
        descriptor = registry.findById(PLUGIN_ID)
                .orElseThrow(() -> new IllegalStateException("示例插件缺少运行描述：" + PLUGIN_ID));
    }

    @GetMapping("/info")
    @SaCheckPermission("plugin:hello:info")
    public RespInfo<HelloPluginInfoVO> info() {
        LOG.info("读取示例插件信息，pluginId={}", descriptor.id());
        return RespInfo.success(new HelloPluginInfoVO(descriptor.id(), descriptor.version(), ForgeVersion.CURRENT));
    }
}
