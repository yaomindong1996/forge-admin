package com.mdframe.forge.starter.plugin.catalog;

import com.mdframe.forge.starter.plugin.descriptor.PluginEdition;

import java.util.List;

/** 当前服务声明的不可变元数据；模块加载不等价于功能可用/健康检查成功。 */
public record RuntimePlugin(String id, String name, String version, PluginOrigin origin, PluginEdition edition,
                            String requiresCore, String serverModule, boolean hasUi, List<String> features) {

    public RuntimePlugin {
        features = List.copyOf(features);
    }
}
