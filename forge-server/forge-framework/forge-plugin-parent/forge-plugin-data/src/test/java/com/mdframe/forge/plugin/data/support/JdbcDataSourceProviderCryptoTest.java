package com.mdframe.forge.plugin.data.support;

import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.persistence.PersistentCiphertext;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcDataSourceProviderCryptoTest {

    @Test
    void shouldFailClosedWhenPasswordCannotBeDecrypted() {
        JdbcDataSourceProvider provider = new JdbcDataSourceProvider(new PersistentCryptoService() {
            @Override
            public String encrypt(String plaintext, String algorithm) {
                return plaintext;
            }

            @Override
            public String decrypt(String ciphertext, String legacyAlgorithm) {
                throw new IllegalStateException("bad key");
            }

            @Override
            public PersistentCiphertext inspect(String ciphertext, String legacyAlgorithm) {
                return null;
            }

            @Override
            public String reencrypt(String ciphertext, String legacyAlgorithm) {
                return ciphertext;
            }
        });
        DataConnection connection = new DataConnection();
        connection.setId(10L);
        connection.setDbType("mysql");
        connection.setDriverClassName("com.mysql.cj.jdbc.Driver");
        connection.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/forge");
        connection.setUsername("forge");
        connection.setPasswordCipher("broken-cipher");
        connection.setTestSql("SELECT 1");

        assertThrows(BusinessException.class, () -> provider.getDataSource(connection));
    }
}
