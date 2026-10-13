package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysFileStorageConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SysFileStorageAccessPolicyTest {

    @Test
    void shouldOnlyAllowEnabledConfigAsDefault() {
        assertThrows(BusinessException.class,
                () -> SysFileStorageAccessPolicy.assertCanSetDefault(config(false, false)));
        assertDoesNotThrow(() -> SysFileStorageAccessPolicy.assertCanSetDefault(config(true, false)));
    }

    @Test
    void shouldBlockDisableOrDeleteDefaultAndLastEnabled() {
        assertThrows(BusinessException.class,
                () -> SysFileStorageAccessPolicy.assertCanDisableOrRemove(config(true, true), "禁用", 2));
        assertThrows(BusinessException.class,
                () -> SysFileStorageAccessPolicy.assertCanDisableOrRemove(config(true, false), "删除", 1));
        assertDoesNotThrow(
                () -> SysFileStorageAccessPolicy.assertCanDisableOrRemove(config(true, false), "禁用", 2));
    }

    @Test
    void shouldBlockEditingDefaultOff() {
        SysFileStorageConfig existing = config(true, true);
        assertThrows(BusinessException.class,
                () -> SysFileStorageAccessPolicy.assertEditableDefault(existing, false, true));
        assertThrows(BusinessException.class,
                () -> SysFileStorageAccessPolicy.assertEditableDefault(existing, true, false));
        assertDoesNotThrow(
                () -> SysFileStorageAccessPolicy.assertEditableDefault(existing, true, true));
    }

    private static SysFileStorageConfig config(boolean enabled, boolean isDefault) {
        SysFileStorageConfig config = new SysFileStorageConfig();
        config.setEnabled(enabled);
        config.setIsDefault(isDefault);
        return config;
    }
}
