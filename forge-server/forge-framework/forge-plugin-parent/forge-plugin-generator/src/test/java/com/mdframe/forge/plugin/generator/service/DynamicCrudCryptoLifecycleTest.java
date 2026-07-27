package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.persistence.PersistentCiphertext;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicCrudCryptoLifecycleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldEncryptSnakeColumnAndDecryptCamelField() throws Exception {
        DynamicCrudService service = service(fakeCrypto(false));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phone_number", "13900000000");

        applyEncrypt(service, data, "{\"phoneNumber\":{\"algorithm\":\"SM4\"}}");

        assertEquals("FPC1:SM4:v2:13900000000", data.get("phone_number"));

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("phoneNumber", "FPC1:SM4:v2:13900000000");
        rows.add(row);

        applyDecrypt(service, rows, "{\"phoneNumber\":{\"algorithm\":\"SM4\"}}");

        assertEquals("13900000000", row.get("phoneNumber"));
    }

    @Test
    void shouldFailClosedWhenLowcodeDecryptFails() {
        DynamicCrudService service = service(fakeCrypto(true));
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("phoneNumber", "broken-cipher");
        rows.add(row);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applyDecrypt(service, rows, "{\"phoneNumber\":{\"algorithm\":\"SM4\"}}"));

        assertTrue(exception.getMessage().contains("低代码加密字段读取失败"));
        assertEquals("broken-cipher", row.get("phoneNumber"));
    }

    @Test
    void shouldFailClosedWhenEncryptConfigIsInvalid() {
        DynamicCrudService service = service(fakeCrypto(false));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phone_number", "13900000000");

        assertThrows(BusinessException.class, () -> applyEncrypt(service, data, "[]"));
    }

    private DynamicCrudService service(PersistentCryptoService cryptoService) {
        return new DynamicCrudService(
                null,
                null,
                objectMapper,
                null,
                null,
                cryptoService,
                new LowcodeEncryptConfigParser(objectMapper),
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private void applyEncrypt(DynamicCrudService service, Map<String, Object> data, String config) throws Exception {
        invoke(service, "applyEncrypt", new Class<?>[]{Map.class, String.class}, data, config);
    }

    @SuppressWarnings("unchecked")
    private void applyDecrypt(DynamicCrudService service, List<Map<String, Object>> rows, String config) throws Exception {
        invoke(service, "applyDecrypt", new Class<?>[]{List.class, String.class}, rows, config);
    }

    private void invoke(DynamicCrudService service, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = DynamicCrudService.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            method.invoke(service, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private PersistentCryptoService fakeCrypto(boolean failDecrypt) {
        return new PersistentCryptoService() {
            @Override
            public String encrypt(String plaintext, String algorithm) {
                return "FPC1:" + algorithm + ":v2:" + plaintext;
            }

            @Override
            public String decrypt(String ciphertext, String legacyAlgorithm) {
                if (failDecrypt) {
                    throw new IllegalStateException("bad key");
                }
                return ciphertext.substring(ciphertext.lastIndexOf(':') + 1);
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
}
