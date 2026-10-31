package com.mdframe.forge.starter.message.config;

import com.mdframe.forge.starter.message.config.SmsConfigProvider.SmsConfig;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadConfigBlendTest {

    @Test
    void unknownSupplierIsSkippedAndRuntimeIdIncludesTenant() throws Exception {
        SmsConfig bad = channel(1L, "bad", "not-a-vendor");
        SmsConfig good = channel(2L, "alibaba", "alibaba");
        ReadConfig readConfig = readConfigOf(List.of(bad, good));

        List<BaseConfig> listed = readConfig.getSupplierConfigList();

        assertEquals(1, listed.size());
        assertEquals("2:alibaba", listed.get(0).getConfigId());
        assertEquals("2:alibaba", readConfig.getSupplierConfig("2:alibaba").getConfigId());
        assertEquals("2:alibaba", readConfig.getSupplierConfig("alibaba").getConfigId());
        assertNull(readConfig.getSupplierConfig("1:bad"));
        assertTrue(listed.stream().noneMatch(item -> item.getConfigId().startsWith("1:")));
    }

    private static ReadConfig readConfigOf(List<SmsConfig> configs) throws Exception {
        ReadConfig readConfig = new ReadConfig();
        Field field = ReadConfig.class.getDeclaredField("configProvider");
        field.setAccessible(true);
        SmsConfigProvider provider = new SmsConfigProvider() {
            @Override
            public SmsConfig getSmsConfig() {
                return configs.isEmpty() ? null : configs.get(0);
            }

            @Override
            public List<SmsConfig> listEnabledSmsConfigs() {
                return configs;
            }
        };
        field.set(readConfig, provider);
        return readConfig;
    }

    private static SmsConfig channel(Long tenantId, String configId, String supplier) {
        SmsConfig config = new SmsConfig();
        config.setTenantId(tenantId);
        config.setConfigId(configId);
        config.setSupplier(supplier);
        config.setAccessKeyId("ak");
        config.setAccessKeySecret("sk");
        config.setSignature("sign");
        config.setTemplateId("SMS_1");
        config.setWeight(1);
        return config;
    }
}
