package com.mdframe.forge.plugin.data.service;

import com.mdframe.forge.plugin.data.dto.DataConnectionSaveDTO;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.mapper.DataConnectionMapper;
import com.mdframe.forge.plugin.data.service.impl.DataConnectionServiceImpl;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.crypto.persistence.PersistentCiphertext;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DataConnectionServiceCryptoTest {

    @Test
    void shouldEncryptPasswordWhenSavingConnection() {
        AtomicReference<DataConnection> captured = new AtomicReference<>();
        DataConnectionServiceImpl service = new DataConnectionServiceImpl(
                mapper(captured, null),
                cryptoService("FPC1:SM4:v2:cipher")
        );

        try (ExecutionIdentityContextHolder.Scope ignored = openIdentity()) {
            service.saveConnection(dto("plain-password"));
        }

        assertEquals("FPC1:SM4:v2:cipher", captured.get().getPasswordCipher());
        assertNotEquals("plain-password", captured.get().getPasswordCipher());
        assertEquals(1L, captured.get().getTenantId());
    }

    @Test
    void shouldKeepExistingCipherWhenUpdatePasswordIsEmpty() {
        AtomicReference<DataConnection> captured = new AtomicReference<>();
        DataConnection existing = new DataConnection();
        existing.setId(10L);
        existing.setPasswordCipher("existing-cipher");
        DataConnectionServiceImpl service = new DataConnectionServiceImpl(
                mapper(captured, existing),
                cryptoService("unused")
        );
        DataConnectionSaveDTO dto = dto("");
        dto.setId(10L);

        try (ExecutionIdentityContextHolder.Scope ignored = openIdentity()) {
            service.updateConnection(dto);
        }

        assertEquals("existing-cipher", captured.get().getPasswordCipher());
    }

    private static DataConnectionMapper mapper(AtomicReference<DataConnection> captured,
                                               DataConnection existing) {
        return (DataConnectionMapper) Proxy.newProxyInstance(
                DataConnectionMapper.class.getClassLoader(),
                new Class<?>[]{DataConnectionMapper.class},
                (proxy, method, args) -> {
                    if ("insert".equals(method.getName()) || "updateById".equals(method.getName())) {
                        captured.set((DataConnection) args[0]);
                        return 1;
                    }
                    if ("selectById".equals(method.getName())) {
                        return existing;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static PersistentCryptoService cryptoService(String ciphertext) {
        return new PersistentCryptoService() {
            @Override
            public String encrypt(String plaintext, String algorithm) {
                return ciphertext;
            }

            @Override
            public String decrypt(String ciphertext, String legacyAlgorithm) {
                return "plain";
            }

            @Override
            public PersistentCiphertext inspect(String ciphertext, String legacyAlgorithm) {
                return null;
            }

            @Override
            public String reencrypt(String ciphertext, String legacyAlgorithm) {
                return ciphertext;
            }
        };
    }

    private static ExecutionIdentityContextHolder.Scope openIdentity() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setTenantId(1L);
        return ExecutionIdentityContextHolder.open(new ExecutionIdentity(
                loginUser,
                "USER",
                1L,
                1L,
                1L,
                "test-client",
                "test-token",
                Set.of()
        ));
    }

    private static DataConnectionSaveDTO dto(String password) {
        DataConnectionSaveDTO dto = new DataConnectionSaveDTO();
        dto.setConnectionCode("main");
        dto.setConnectionName("Main");
        dto.setDbType("mysql");
        dto.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dto.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/forge");
        dto.setUsername("forge");
        dto.setPassword(password);
        return dto;
    }
}
