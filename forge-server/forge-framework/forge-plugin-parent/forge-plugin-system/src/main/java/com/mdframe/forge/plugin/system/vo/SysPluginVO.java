package com.mdframe.forge.plugin.system.vo;

import java.util.List;

/** 不输出描述资源位置/UI 源码路径，避免暴露部署目录。 */
public record SysPluginVO(String id, String name, String version, String origin, String edition,
                          String loadState, String requiresCore, String serverModule,
                          boolean hasUi, List<Feature> features) {
    public record Feature(String code, boolean enabled) {
    }
}
