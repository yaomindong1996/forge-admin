package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.controller.*;
import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.annotation.crypto.*;
import com.mdframe.forge.starter.core.exception.BusinessException;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc 验证 DTO→事务 Service→真实 Mapper；加密/Sa-Token 切面在真实环境另验。
 */
class PrintTemplateControllerTest extends PrintServiceFixture {

    @RestControllerAdvice
    static class Errors {

        @ExceptionHandler(BusinessException.class)
        ResponseEntity<?> error(BusinessException ex) {
            return ResponseEntity.status(ex.getCode()).body(Map.of("message", ex.getMessage()));
        }
    }

    @Test
    void httpRoundTripUsesTypedDtosAndReturnsCanonicalSchemaWithoutPrivateData() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new PrintTemplateController(service, publication), new PrintRuntimeController(runtime, events), new PrintBindingController(bindings)).setControllerAdvice(new Errors()).setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
        schema = schema.replace("宋体, sans-serif", "Arial, sans-serif");
        var row = published();
        mvc.perform(get("/print/templates/page").param("applicationId", "2").param("pageNum", "1").param("pageSize", "20")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.records[0].schemaJson").isEmpty());
        mvc.perform(put("/print/templates/" + row.id()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedRevision\":0,\"templateName\":\"invalid\",\"schemaJson\":\"{}\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/print/bindings").param("applicationId", "2").param("sourceType", "LOWCODE").param("pageId", "page_purchase").param("objectCode", "purchase").param("scene", "DETAIL")).andExpect(status().isOk());
        permissions.clear();
        permissions.add("print:execute");
        adapter.designDenied = true;
        String wire = mvc.perform(post("/print/prepare").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new PrintPrepareDTO(record(), row.id())))).andExpect(status().isOk()).andExpect(jsonPath("$.data.dataMode").value("CURRENT")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(wire).doesNotContain("MUST_NOT_LEAK");
        assertThat(json.readTree(wire).path("data").path("schemaJson").asText()).isEqualTo(protocol.validate(schema).canonicalJson());
        Files.writeString(Path.of("target/print-prepare-wire.json"), wire);
        mvc.perform(get("/print/templates/" + row.id())).andExpect(status().isForbidden());
    }

    @Test
    void everyEndpointHasPermissionEncryptionAndDisablesBodyLogging() {
        for (Class<?> controller : List.of(PrintTemplateController.class, PrintBindingController.class, PrintRuntimeController.class)) {
            assertThat(controller.isAnnotationPresent(ApiDecrypt.class)).isTrue();
            assertThat(controller.isAnnotationPresent(ApiEncrypt.class)).isTrue();
            for (var method : controller.getDeclaredMethods()) {
                assertThat(method.isAnnotationPresent(SaCheckPermission.class)).as(method.getName()).isTrue();
                var log = method.getAnnotation(OperationLog.class);
                assertThat(log).isNotNull();
                assertThat(log.saveRequestParams()).isFalse();
                assertThat(log.saveResponseResult()).isFalse();
                assertThat(Arrays.stream(method.getParameterTypes()).anyMatch(Map.class::isAssignableFrom)).isFalse();
            }
        }
    }
}
