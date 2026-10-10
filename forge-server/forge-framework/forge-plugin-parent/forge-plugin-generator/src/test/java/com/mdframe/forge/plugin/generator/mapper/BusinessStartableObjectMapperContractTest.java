package com.mdframe.forge.plugin.generator.mapper;

import com.mdframe.forge.plugin.generator.controller.BusinessFlowController;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableObjectVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessStartableObjectMapperContractTest {

    private static final Path MAPPER = Path.of("src/main/resources/mapper/BusinessStartableObjectMapper.xml");

    @Test
    void catalogOnlyReturnsEnabledFlowBindingsOfCurrentTenant() throws IOException {
        String xml = Files.readString(MAPPER);

        assertThat(xml).contains(
                "b.tenant_id = #{tenantId}",
                "b.target_type = 'OBJECT'",
                "b.binding_type = 'FLOW'",
                "b.status = 1",
                "o.tenant_id = b.tenant_id",
                "o.status = 1",
                "o.del_flag = 0",
                "ao.del_flag = 0",
                "a.status = 1",
                "a.del_flag = 0",
                "o.config_key IS NOT NULL");
        assertThat(xml).doesNotContain("${");
    }

    @Test
    void voOnlyExposesCatalogMetadata() {
        String[] fields = Arrays.stream(BusinessStartableObjectVO.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .sorted()
                .toArray(String[]::new);

        assertThat(fields).containsExactly(
                "applicationIcon", "applicationId", "applicationName", "configKey",
                "objectCode", "objectIcon", "objectName", "sortOrder");
    }

    @Test
    void endpointIsLoginOnlyAndEncrypted() throws NoSuchMethodException {
        Method method = BusinessFlowController.class.getMethod("startableObjects");

        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/startable-objects");
        assertThat(method.isAnnotationPresent(ApiPermissionIgnore.class)).isTrue();
        assertThat(BusinessFlowController.class.isAnnotationPresent(ApiDecrypt.class)).isTrue();
        assertThat(BusinessFlowController.class.isAnnotationPresent(ApiEncrypt.class)).isTrue();
    }
}
