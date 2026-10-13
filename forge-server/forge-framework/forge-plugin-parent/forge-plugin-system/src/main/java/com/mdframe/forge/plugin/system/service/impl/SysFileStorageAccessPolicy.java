package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysFileStorageConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;

/**
 * 文件存储配置保护：默认项与最后一个启用项不可禁用/删除。
 */
final class SysFileStorageAccessPolicy {

    private SysFileStorageAccessPolicy() {
    }

    static void assertCanSetDefault(SysFileStorageConfig config) {
        if (config == null) {
            throw new BusinessException("文件存储配置不存在");
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new BusinessException("只能将已启用的存储配置设为默认");
        }
    }

    static void assertCanDisableOrRemove(SysFileStorageConfig config, String action, long enabledCount) {
        if (config == null) {
            throw new BusinessException("文件存储配置不存在");
        }
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            throw new BusinessException("默认存储配置不能" + action + "，请先将其它启用配置设为默认");
        }
        if (Boolean.TRUE.equals(config.getEnabled()) && enabledCount <= 1) {
            throw new BusinessException("至少保留一个启用的存储配置，不能" + action + "最后一个启用项");
        }
    }

    static void assertEditableDefault(SysFileStorageConfig existing, Boolean nextEnabled, Boolean nextIsDefault) {
        if (existing == null || !Boolean.TRUE.equals(existing.getIsDefault())) {
            return;
        }
        if (Boolean.FALSE.equals(nextEnabled)) {
            throw new BusinessException("默认存储配置不能禁用，请先将其它启用配置设为默认");
        }
        if (Boolean.FALSE.equals(nextIsDefault)) {
            throw new BusinessException("请通过「设为默认」切换默认存储，不能直接取消默认标记");
        }
    }
}
