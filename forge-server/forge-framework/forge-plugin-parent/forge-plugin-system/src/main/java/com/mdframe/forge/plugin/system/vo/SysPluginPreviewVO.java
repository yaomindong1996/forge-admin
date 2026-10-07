package com.mdframe.forge.plugin.system.vo;

import com.mdframe.forge.starter.plugin.delivery.SourcePluginPackage;

import java.util.List;

public record SysPluginPreviewVO(SourcePluginPackage source, String coreVersion, String currentVersion,
                                 String operation, String runtimeSnapshot, List<String> blockers,
                                 List<String> warnings) {
}
