package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.Objects;

/**
 * 系统参数内置项保护：configType=Y 禁删、禁改键名。
 */
final class SysConfigAccessPolicy {

    static final String BUILT_IN = "Y";

    private SysConfigAccessPolicy() {
    }

    static boolean isBuiltIn(SysConfig config) {
        return config != null && BUILT_IN.equalsIgnoreCase(StrUtil.trim(config.getConfigType()));
    }

    static void assertDeletable(SysConfig existing) {
        if (isBuiltIn(existing)) {
            throw new BusinessException("系统内置参数不能删除");
        }
    }

    static void assertMutable(SysConfig existing, String nextConfigKey, String nextConfigType) {
        if (!isBuiltIn(existing)) {
            return;
        }
        if (StrUtil.isNotBlank(nextConfigKey)
                && !Objects.equals(existing.getConfigKey(), nextConfigKey.trim())) {
            throw new BusinessException("系统内置参数键名不能修改");
        }
        if (StrUtil.isNotBlank(nextConfigType)
                && !BUILT_IN.equalsIgnoreCase(nextConfigType.trim())) {
            throw new BusinessException("系统内置参数不能改为非内置");
        }
    }
}
