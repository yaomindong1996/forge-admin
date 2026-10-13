package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SysDictTypeAccessPolicyTest {

    @Test
    void shouldProtectSysPrefixedTypesFromDelete() {
        SysDictType system = type("sys_user_sex");
        assertTrue(SysDictTypeAccessPolicy.isSystemDictType(system));
        assertThrows(BusinessException.class, () -> SysDictTypeAccessPolicy.assertDeletable(system));
        assertDoesNotThrow(() -> SysDictTypeAccessPolicy.assertDeletable(type("biz_order_status")));
    }

    @Test
    void shouldLockDictTypeCodeAfterCreate() {
        SysDictType existing = type("biz_status");
        assertThrows(BusinessException.class,
                () -> SysDictTypeAccessPolicy.assertDictTypeCodeMutable(existing, "biz_status_v2"));
        assertDoesNotThrow(() -> SysDictTypeAccessPolicy.assertDictTypeCodeMutable(existing, "biz_status"));
    }

    private static SysDictType type(String dictType) {
        SysDictType type = new SysDictType();
        type.setDictId(1L);
        type.setDictType(dictType);
        return type;
    }
}
