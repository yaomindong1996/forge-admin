package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SysClientAccessPolicyTest {

    @Test
    void shouldRecognizeBuiltInClientCodesCaseInsensitively() {
        assertTrue(SysClientAccessPolicy.isBuiltIn("pc"));
        assertTrue(SysClientAccessPolicy.isBuiltIn("APP"));
        assertTrue(SysClientAccessPolicy.isBuiltIn("H5"));
        assertTrue(SysClientAccessPolicy.isBuiltIn("forge_report"));
        assertFalse(SysClientAccessPolicy.isBuiltIn("custom"));
        assertFalse(SysClientAccessPolicy.isBuiltIn((String) null));
        assertFalse(SysClientAccessPolicy.isBuiltIn((SysClient) null));
    }

    @Test
    void shouldBlockDeleteAndCodeOrStatusMutationForBuiltInClients() {
        SysClient builtIn = client("pc");
        BusinessException deleteError = assertThrows(BusinessException.class,
                () -> SysClientAccessPolicy.assertDeletable(builtIn));
        assertTrue(deleteError.getMessage().contains("不能删除"));

        BusinessException codeError = assertThrows(BusinessException.class,
                () -> SysClientAccessPolicy.assertMutable(builtIn, "pc-renamed", EnableStatus.ENABLED.getCode()));
        assertTrue(codeError.getMessage().contains("编码不能修改"));

        BusinessException statusError = assertThrows(BusinessException.class,
                () -> SysClientAccessPolicy.assertMutable(builtIn, "pc", EnableStatus.DISABLED.getCode()));
        assertTrue(statusError.getMessage().contains("不能禁用"));
    }

    @Test
    void shouldAllowNonBuiltInClientsAndBuiltInSafeEdits() {
        SysClient custom = client("custom-a");
        assertDoesNotThrow(() -> SysClientAccessPolicy.assertDeletable(custom));
        assertDoesNotThrow(() -> SysClientAccessPolicy.assertMutable(custom, "custom-b", 0));

        SysClient builtIn = client("app");
        assertDoesNotThrow(() -> SysClientAccessPolicy.assertMutable(
                builtIn, "app", EnableStatus.ENABLED.getCode()));
        assertDoesNotThrow(() -> SysClientAccessPolicy.assertMutable(builtIn, "APP", null));
    }

    private static SysClient client(String code) {
        SysClient client = new SysClient();
        client.setId(1L);
        client.setClientCode(code);
        client.setStatus(EnableStatus.ENABLED.getCode());
        return client;
    }
}
