package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApp access-entry compatibility")
class BusinessAppServiceCompatibilityTest {

    @Test
    @DisplayName("legacy entity remains mapped to ai_business_app with explicit logic delete")
    void entityKeepsTableAndLogicDeleteContract() throws NoSuchFieldException {
        TableName tableName = AiBusinessApp.class.getAnnotation(TableName.class);

        assertNotNull(tableName);
        assertEquals("ai_business_app", tableName.value());
        assertNotNull(AiBusinessApp.class.getDeclaredField("delFlag").getAnnotation(TableLogic.class));
    }

    @Test
    @DisplayName("legacy DTO query and VO retain access-entry fields")
    void transportObjectsKeepAccessEntryFields() throws NoSuchFieldException {
        assertNotNull(BusinessAppDTO.class.getDeclaredField("appCode"));
        assertNotNull(BusinessAppDTO.class.getDeclaredField("objectCode"));
        assertNotNull(BusinessAppDTO.class.getDeclaredField("entryMode"));
        assertNotNull(BusinessAppDTO.class.getDeclaredField("configKey"));
        assertNotNull(BusinessAppDTO.class.getDeclaredField("options"));
        assertNotNull(BusinessAppDTO.class.getDeclaredField("applicationId"));
    }

    @Test
    @DisplayName("legacy query and VO retain filtering and open fields")
    void queryAndVoKeepLegacyFields() throws NoSuchFieldException {
        assertNotNull(BusinessAppQueryDTO.class.getDeclaredField("suiteCode"));
        assertNotNull(BusinessAppQueryDTO.class.getDeclaredField("objectCode"));
        assertNotNull(BusinessAppQueryDTO.class.getDeclaredField("entryMode"));
        assertNotNull(BusinessAppQueryDTO.class.getDeclaredField("applicationId"));
        assertNotNull(BusinessAppVO.class.getDeclaredField("entryUrl"));
        assertNotNull(BusinessAppVO.class.getDeclaredField("runtimeOpenMode"));
        assertNotNull(BusinessAppVO.class.getDeclaredField("appMode"));
    }

    @Test
    @DisplayName("legacy mapper XML keeps tenant and logic-delete filters")
    void mapperXmlKeepsTenantAndLogicDeleteFilters() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/BusinessAppMapper.xml")) {
            assertNotNull(input);
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(xml.contains("FROM ai_business_app"));
            assertTrue(xml.contains("tenant_id = #{tenantId}"));
            assertTrue(xml.contains("del_flag = '0'"));
            assertTrue(xml.contains("app_code"));
            assertTrue(xml.contains("object_code"));
            assertTrue(xml.contains("entry_mode"));
            assertTrue(xml.contains("config_key"));
        }
    }
}
