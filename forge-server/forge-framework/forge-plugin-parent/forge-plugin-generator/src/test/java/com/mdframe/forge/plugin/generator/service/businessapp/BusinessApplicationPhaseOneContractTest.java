package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.controller.BusinessApplicationController;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("BusinessApplication Phase 1 contract")
class BusinessApplicationPhaseOneContractTest {

    @Test
    @DisplayName("application aggregate uses an independent logic-delete table")
    void applicationAggregateUsesIndependentTable() throws NoSuchFieldException {
        TableName tableName = AiBusinessApplication.class.getAnnotation(TableName.class);

        assertNotNull(tableName);
        assertEquals("ai_business_application", tableName.value());
        assertNotNull(AiBusinessApplication.class.getDeclaredField("delFlag").getAnnotation(TableLogic.class));
    }

    @Test
    @DisplayName("application-object association uses explicit logic delete")
    void applicationObjectUsesExplicitLogicDelete() throws NoSuchFieldException {
        TableName tableName = AiBusinessApplicationObject.class.getAnnotation(TableName.class);

        assertNotNull(tableName);
        assertEquals("ai_business_application_object", tableName.value());
        assertNotNull(AiBusinessApplicationObject.class.getDeclaredField("delFlag").getAnnotation(TableLogic.class));
    }

    @Test
    @DisplayName("application transport contract exposes stable aggregate fields")
    void transportContractExposesAggregateFields() throws NoSuchFieldException {
        assertNotNull(BusinessApplicationDTO.class.getDeclaredField("applicationCode"));
        assertNotNull(BusinessApplicationDTO.class.getDeclaredField("applicationName"));
        assertNotNull(BusinessApplicationDTO.class.getDeclaredField("suiteCode"));
        assertNotNull(BusinessApplicationQueryDTO.class.getDeclaredField("suiteCodes"));
        assertNotNull(BusinessApplicationVO.class.getDeclaredField("objectCount"));
        assertNotNull(BusinessApplicationVO.class.getDeclaredField("entryCount"));
    }

    @Test
    @DisplayName("application aggregate has a separate API namespace")
    void applicationAggregateHasSeparateApiNamespace() {
        RequestMapping mapping = BusinessApplicationController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/ai/business/application"}, mapping.value());
    }

    @Test
    @DisplayName("application object roles are frozen")
    void applicationObjectRolesAreFrozen() {
        assertEquals(Set.of("PRIMARY", "DETAIL", "REFERENCE", "SHARED"),
                BusinessApplicationObjectRole.supportedRoles());
    }
}
