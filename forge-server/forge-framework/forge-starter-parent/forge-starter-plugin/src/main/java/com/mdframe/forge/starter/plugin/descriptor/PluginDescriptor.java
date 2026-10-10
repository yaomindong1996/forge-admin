package com.mdframe.forge.starter.plugin.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 不可变插件元数据；classPath 描述不包含源码文件存在性检查。 */
public record PluginDescriptor(String id, String name, String version, PluginEdition edition,
                               String requiresCore, List<String> features, Server server, Ui ui) {

    public PluginDescriptor {
        // 保留非法 null 项给校验器解释，防止底层复制异常掩盖具体插件信息。
        features = features == null ? null : Collections.unmodifiableList(new ArrayList<>(features));
    }

    public record Server(String module) {
    }

    public record Ui(String dir) {
    }
}
