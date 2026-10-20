package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowModelBindingsQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowModelBindingQueryService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BusinessFlowModelBindingControllerTest {
    @Test
    void dtoRejectsMissingEmptyOversizedAndBlankKeys() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var query = new BusinessFlowModelBindingsQueryDTO();
            assertThat(validator.validate(query)).isNotEmpty();
            query.setModelKeys(List.of());
            assertThat(validator.validate(query)).isNotEmpty();
            query.setModelKeys(List.of(" "));
            assertThat(validator.validate(query)).isNotEmpty();
            query.setModelKeys(Collections.nCopies(101, "model-A"));
            assertThat(validator.validate(query)).isNotEmpty();
            query.setModelKeys(List.of("A".repeat(129)));
            assertThat(validator.validate(query)).isNotEmpty();
            query.setModelKeys(List.of("model-A", "model-B"));
            assertThat(validator.validate(query)).isEmpty();
        }
    }

    @Test
    void repeatedQueryParametersBindToValidatedDtoAndKeepOriginalPermission() throws Exception {
        var service = mock(BusinessFlowModelBindingQueryService.class);
        var controller = new BusinessFlowModelBindingController(service);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var mvc = MockMvcBuilders.standaloneSetup(controller)
                    .setValidator(new SpringValidatorAdapter(factory.getValidator())).build();
            mvc.perform(get("/ai/business/flow/model/business-bindings")
                    .param("modelKeys", "model-A", "model-B")).andExpect(status().isOk());
            verify(service).list(argThat(query -> query.getModelKeys().equals(List.of("model-A", "model-B"))));
            mvc.perform(get("/ai/business/flow/model/business-bindings")).andExpect(status().isBadRequest());
        }
        var permission = BusinessFlowModelBindingController.class
                .getMethod("list", BusinessFlowModelBindingsQueryDTO.class).getAnnotation(SaCheckPermission.class);
        assertThat(permission.value()).containsExactly("ai:businessFlow:config");
    }
}
