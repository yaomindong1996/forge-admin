package com.mdframe.forge.plugin.generator.controller;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("BusinessAppController compatibility")
class BusinessAppControllerCompatibilityTest {

    @Test
    @DisplayName("legacy access-entry base path remains unchanged")
    void legacyBasePathRemainsUnchanged() {
        RequestMapping mapping = BusinessAppController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/ai/business/app"}, mapping.value());
    }

    @Test
    @DisplayName("page keeps pageNum and pageSize request parameters")
    void pageKeepsPaginationContract() throws NoSuchMethodException {
        Method page = BusinessAppController.class.getDeclaredMethod(
                "page", Integer.class, Integer.class, BusinessAppQueryDTO.class);
        GetMapping mapping = page.getAnnotation(GetMapping.class);
        Parameter[] parameters = page.getParameters();

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/page"}, mapping.value());
        assertEquals("1", parameters[0].getAnnotation(RequestParam.class).defaultValue());
        assertEquals("10", parameters[1].getAnnotation(RequestParam.class).defaultValue());
    }

    @Test
    @DisplayName("detail and open-info keep access-entry id paths")
    void detailAndOpenInfoKeepAccessEntryIdentity() throws NoSuchMethodException {
        Method detail = BusinessAppController.class.getDeclaredMethod("detail", Long.class);
        Method openInfo = BusinessAppController.class.getDeclaredMethod("openInfo", Long.class);

        assertArrayEquals(new String[]{"/{id}"}, detail.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/open-info"}, openInfo.getAnnotation(GetMapping.class).value());
    }

    @Test
    @DisplayName("legacy write routes remain unchanged")
    void legacyWriteRoutesRemainUnchanged() throws NoSuchMethodException {
        Method create = BusinessAppController.class.getDeclaredMethod("create", BusinessAppDTO.class);
        Method update = BusinessAppController.class.getDeclaredMethod("update", BusinessAppDTO.class);
        Method status = BusinessAppController.class.getDeclaredMethod("updateStatus", Long.class, Integer.class);
        Method delete = BusinessAppController.class.getDeclaredMethod("delete", Long.class);

        assertNotNull(create.getAnnotation(PostMapping.class));
        assertNotNull(update.getAnnotation(PutMapping.class));
        assertArrayEquals(new String[]{"/{id}/status"}, status.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[]{"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
    }
}
