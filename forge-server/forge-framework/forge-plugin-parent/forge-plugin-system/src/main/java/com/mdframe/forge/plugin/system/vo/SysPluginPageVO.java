package com.mdframe.forge.plugin.system.vo;

import java.util.List;

/** 核心版本和插件版本独立返回；分页 total 是过滤后的总数。 */
public record SysPluginPageVO(List<SysPluginVO> records, long total, int current, int size,
                              String coreVersion, String edition) {
}
