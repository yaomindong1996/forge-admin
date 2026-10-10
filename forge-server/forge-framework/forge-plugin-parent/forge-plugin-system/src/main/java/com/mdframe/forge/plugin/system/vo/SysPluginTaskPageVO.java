package com.mdframe.forge.plugin.system.vo;

import java.util.List;

public record SysPluginTaskPageVO(List<SysPluginTaskVO> records, long total, int current, int size) {
}
