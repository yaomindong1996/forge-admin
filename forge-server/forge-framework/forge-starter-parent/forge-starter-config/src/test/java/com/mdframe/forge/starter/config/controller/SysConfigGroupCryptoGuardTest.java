package com.mdframe.forge.starter.config.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import com.mdframe.forge.starter.config.security.CryptoConfigSanitizer;
import com.mdframe.forge.starter.config.service.ISysConfigGroupService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SysConfigGroupCryptoGuardTest {

    @Test
    void detailShouldSanitizeCryptoSecrets() {
        SysConfigGroup group = group(1L, "crypto", "{\"enabled\":true,\"secretKey\":\"redacted\"}");
        ServiceStub stub = new ServiceStub();
        stub.current.set(group);
        SysConfigGroupController controller = controller(stub);

        RespInfo<SysConfigGroup> response = controller.getInfo(1L);

        assertEquals(200, response.getCode());
        assertFalse(response.getData().getConfigValue().contains("secretKey"));
    }

    @Test
    void editShouldRejectSecretsWhenCryptoGroupIsRenamed() {
        SysConfigGroup current = group(1L, "crypto", "{\"enabled\":true}");
        SysConfigGroup request = group(1L, "renamed", "{\"secretKey\":\"redacted\"}");
        ServiceStub stub = new ServiceStub();
        stub.current.set(current);
        SysConfigGroupController controller = controller(stub);

        RespInfo<Void> response = controller.edit(request);

        assertEquals(500, response.getCode());
        assertFalse(stub.updated.get());
    }

    private SysConfigGroupController controller(ServiceStub stub) {
        return new SysConfigGroupController(stub.service(), new CryptoConfigSanitizer(new ObjectMapper()));
    }

    private SysConfigGroup group(Long id, String groupCode, String configValue) {
        SysConfigGroup group = new SysConfigGroup();
        group.setId(id);
        group.setGroupCode(groupCode);
        group.setConfigValue(configValue);
        return group;
    }

    private static final class ServiceStub {

        private final AtomicReference<SysConfigGroup> current = new AtomicReference<>();
        private final AtomicBoolean updated = new AtomicBoolean();

        private ISysConfigGroupService service() {
            return (ISysConfigGroupService) Proxy.newProxyInstance(
                    ISysConfigGroupService.class.getClassLoader(),
                    new Class<?>[]{ISysConfigGroupService.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getById" -> current.get();
                        case "selectByGroupCode" -> null;
                        case "updateById" -> {
                            updated.set(true);
                            yield true;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
