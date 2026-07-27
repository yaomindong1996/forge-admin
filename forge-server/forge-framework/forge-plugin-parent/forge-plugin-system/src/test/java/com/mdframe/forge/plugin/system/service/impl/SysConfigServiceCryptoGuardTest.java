package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.dto.SysConfigDTO;
import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.mapper.SysConfigMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysConfigServiceCryptoGuardTest {

    @Test
    void insertShouldRejectDeploymentCryptoSecretKey() {
        MapperStub stub = new MapperStub();
        SysConfigServiceImpl service = new SysConfigServiceImpl(stub.mapper());
        SysConfigDTO dto = dto(null, "forge.crypto.persistence.active-key");

        assertThrows(BusinessException.class, () -> service.insertConfig(dto));
        assertFalse(stub.inserted.get());
    }

    @Test
    void insertShouldRejectBracketNotationPersistenceKey() {
        MapperStub stub = new MapperStub();
        SysConfigServiceImpl service = new SysConfigServiceImpl(stub.mapper());
        SysConfigDTO dto = dto(null, "forge.crypto.persistence.keys[previous]");

        assertThrows(BusinessException.class, () -> service.insertConfig(dto));
        assertFalse(stub.inserted.get());
    }

    @Test
    void updateShouldRejectRenamingToOrFromDeploymentCryptoSecretKey() {
        MapperStub stub = new MapperStub();
        SysConfigServiceImpl service = new SysConfigServiceImpl(stub.mapper());
        SysConfig existing = new SysConfig();
        existing.setConfigId(1L);
        existing.setConfigKey("ordinary.setting");
        stub.existing.set(existing);

        assertThrows(BusinessException.class,
                () -> service.updateConfig(dto(1L, "forge.crypto.secret-key")));

        existing.setConfigKey("forge.crypto.secret-key");
        assertThrows(BusinessException.class,
                () -> service.updateConfig(dto(1L, "ordinary.setting")));
        assertFalse(stub.updated.get());
    }

    @Test
    void ordinarySensitiveConfigurationShouldRemainWritable() {
        MapperStub stub = new MapperStub();
        SysConfigServiceImpl service = new SysConfigServiceImpl(stub.mapper());
        SysConfigDTO dto = dto(null, "storage.access-key");

        assertTrue(service.insertConfig(dto));
        assertTrue(stub.inserted.get());
    }

    private SysConfigDTO dto(Long id, String key) {
        SysConfigDTO dto = new SysConfigDTO();
        dto.setConfigId(id);
        dto.setConfigKey(key);
        dto.setConfigValue("redacted");
        return dto;
    }

    private static final class MapperStub {

        private final AtomicReference<SysConfig> existing = new AtomicReference<>();
        private final AtomicBoolean inserted = new AtomicBoolean();
        private final AtomicBoolean updated = new AtomicBoolean();

        private SysConfigMapper mapper() {
            return (SysConfigMapper) Proxy.newProxyInstance(
                    SysConfigMapper.class.getClassLoader(),
                    new Class<?>[]{SysConfigMapper.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "selectById" -> existing.get();
                        case "insert" -> {
                            inserted.set(true);
                            yield 1;
                        }
                        case "updateById" -> {
                            updated.set(true);
                            yield 1;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
