package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.starter.auth.service.ICaptchaService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLoadServiceImplCaptchaTest {

    @Test
    void shouldUseSmsCaptchaContractForPhoneLogin() {
        ICaptchaService captchaService = mock(ICaptchaService.class);
        UserLoadServiceImpl service = new UserLoadServiceImpl(
                null, null, null, null, null, null, null, null, null,
                captchaService, null, null);
        when(captchaService.validateAndDeleteSmsCaptcha("13800138000", "123456"))
                .thenReturn(true);

        assertThat(service.validatePhoneCode("13800138000", "123456")).isTrue();
        verify(captchaService).validateAndDeleteSmsCaptcha("13800138000", "123456");
    }
}
