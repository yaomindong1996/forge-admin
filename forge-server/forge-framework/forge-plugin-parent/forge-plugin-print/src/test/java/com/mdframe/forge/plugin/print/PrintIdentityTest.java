package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.starter.core.session.*;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintIdentityTest {

    @Test
    void requiresLoginPermissionAndMatchingTenantContext() {
        try (var validator = Validation.buildDefaultValidatorFactory();
            var session = mockStatic(SessionHelper.class)) {
            var identity = new PrintIdentity(validator.getValidator());
            assertThatThrownBy(() -> identity.require("print:execute")).isInstanceOf(BusinessException.class);
            var user = new LoginUser();
            user.setTenantId(1L);
            user.setUserId(9L);
            user.setMainOrgId(3L);
            session.when(SessionHelper::getLoginUser).thenReturn(user);
            assertThat(identity.current().userId()).isEqualTo(9L);
            assertThatThrownBy(() -> identity.require("print:execute")).isInstanceOf(BusinessException.class);
            session.when(() -> SessionHelper.hasPermission("print:execute")).thenReturn(true);
            TenantContextHolder.setTenantId(2L);
            assertThatThrownBy(identity::current).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> identity.require("print:execute")).isInstanceOf(BusinessException.class);
            TenantContextHolder.setTenantId(1L);
            assertThat(identity.require("print:execute").userId()).isEqualTo(9L);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
