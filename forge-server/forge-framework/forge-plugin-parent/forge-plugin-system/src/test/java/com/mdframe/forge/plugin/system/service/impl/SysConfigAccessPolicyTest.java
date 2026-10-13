package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SysConfigAccessPolicyTest {

    @Test
    void shouldBlockBuiltInDeleteAndKeyMutation() {
        SysConfig builtIn = config("Y", "sys.export.max");
        assertThrows(BusinessException.class, () -> SysConfigAccessPolicy.assertDeletable(builtIn));
        assertThrows(BusinessException.class,
                () -> SysConfigAccessPolicy.assertMutable(builtIn, "sys.export.other", "Y"));
        assertThrows(BusinessException.class,
                () -> SysConfigAccessPolicy.assertMutable(builtIn, "sys.export.max", "N"));
        assertDoesNotThrow(() -> SysConfigAccessPolicy.assertMutable(builtIn, "sys.export.max", "Y"));
    }

    @Test
    void shouldAllowCustomConfigMutation() {
        SysConfig custom = config("N", "biz.custom.key");
        assertDoesNotThrow(() -> SysConfigAccessPolicy.assertDeletable(custom));
        assertDoesNotThrow(() -> SysConfigAccessPolicy.assertMutable(custom, "biz.custom.key2", "N"));
    }

    private static SysConfig config(String type, String key) {
        SysConfig config = new SysConfig();
        config.setConfigType(type);
        config.setConfigKey(key);
        return config;
    }
}
