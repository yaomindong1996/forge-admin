package com.mdframe.forge.plugin.system.vo;

import java.util.List;

/** 对比整个实例，而不是拿列表当前页冒充完整运行清单。 */
public record SysPluginSnapshotVO(List<SysPluginVO> records, String coreVersion, String edition) {
}
