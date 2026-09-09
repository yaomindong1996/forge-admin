package com.mdframe.forge.starter.auth.service.impl;

import com.mdframe.forge.starter.auth.config.CaptchaProperties;
import com.mdframe.forge.starter.auth.domain.CaptchaResult;
import com.mdframe.forge.starter.auth.domain.EmailCaptchaResult;
import com.mdframe.forge.starter.auth.domain.SmsCaptchaResult;
import com.mdframe.forge.starter.auth.domain.SliderCaptchaResult;
import com.mdframe.forge.starter.auth.email.EmailCaptchaSender;
import com.mdframe.forge.starter.auth.sms.SmsCaptchaSender;
import com.mdframe.forge.starter.cache.service.ICacheService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CaptchaServiceImplTest {

    private static final String PHONE = "13800138000";
    private static final String EMAIL = "user@example.com";
    private static final String SMS_CACHE_KEY = "captcha:sms:" + PHONE;
    private static final String SMS_INTERVAL_KEY = "captcha:sms:interval:" + PHONE;
    private static final String EMAIL_CACHE_KEY = "captcha:email:" + EMAIL;
    private static final String EMAIL_INTERVAL_KEY = "captcha:email:interval:" + EMAIL;
    private static final Duration DURATION = Duration.ofMinutes(5);

    @Test
    void shouldHideGraphicCaptchaCodeByDefault() {
        ICacheService cacheService = mock(ICacheService.class);
        CaptchaResult result = service(cacheService, false, null, Optional.empty())
                .generateGraphicCaptcha();

        assertThat(result.getCode()).isNull();
        assertThat(result.getCodeKey()).isNotBlank();
        assertThat(result.getImage()).startsWith("data:image/");
    }

    @Test
    void shouldEchoGraphicCaptchaCodeOnlyInDevelopmentProfile() {
        ICacheService cacheService = mock(ICacheService.class);
        CaptchaResult result = service(cacheService, true, "dev", Optional.empty())
                .generateGraphicCaptcha();

        assertThat(result.getCode()).isNotBlank();
    }

    @Test
    void shouldHideGraphicCaptchaCodeWhenProductionEchoIsMisconfigured() {
        ICacheService cacheService = mock(ICacheService.class);
        CaptchaResult result = service(cacheService, true, "prod", Optional.empty())
                .generateGraphicCaptcha();

        assertThat(result.getCode()).isNull();
    }

    @Test
    void shouldIgnoreDevelopmentEchoInProductionProfile() {
        ICacheService cacheService = mock(ICacheService.class);
        SmsCaptchaSender sender = mock(SmsCaptchaSender.class);
        when(sender.sendVerificationCode(eq(PHONE), anyString(), eq(DURATION))).thenReturn(true);

        SmsCaptchaResult result = service(cacheService, true, "prod", Optional.of(sender))
                .sendSmsCaptcha(PHONE, DURATION);

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getCode()).isNull();
        verify(sender).sendVerificationCode(eq(PHONE), anyString(), eq(DURATION));
    }

    @Test
    void shouldFailClosedWhenSmsSenderIsUnavailable() {
        ICacheService cacheService = mock(ICacheService.class);

        SmsCaptchaResult result = service(cacheService, false, "prod", Optional.empty())
                .sendSmsCaptcha(PHONE, DURATION);

        assertThat(result.getStatus()).isEqualTo("fail");
        assertThat(result.getCode()).isNull();
        verify(cacheService).delete(SMS_CACHE_KEY);
        verify(cacheService, never()).set(eq(SMS_INTERVAL_KEY), any(), any(Duration.class));
    }

    @Test
    void shouldRollbackCachedCodeWhenSmsSenderFails() {
        ICacheService cacheService = mock(ICacheService.class);
        SmsCaptchaSender sender = mock(SmsCaptchaSender.class);
        when(sender.sendVerificationCode(eq(PHONE), anyString(), eq(DURATION))).thenReturn(false);

        SmsCaptchaResult result = service(cacheService, false, "prod", Optional.of(sender))
                .sendSmsCaptcha(PHONE, DURATION);

        assertThat(result.getStatus()).isEqualTo("fail");
        verify(cacheService).set(eq(SMS_CACHE_KEY), anyString(), eq(DURATION));
        verify(cacheService).delete(SMS_CACHE_KEY);
        verify(cacheService, never()).set(eq(SMS_INTERVAL_KEY), any(), any(Duration.class));
    }

    @Test
    void shouldRollbackCachedCodeWhenSmsSenderThrows() {
        ICacheService cacheService = mock(ICacheService.class);
        SmsCaptchaSender sender = mock(SmsCaptchaSender.class);
        when(sender.sendVerificationCode(eq(PHONE), anyString(), eq(DURATION)))
                .thenThrow(new IllegalStateException("provider unavailable"));

        SmsCaptchaResult result = service(cacheService, false, "prod", Optional.of(sender))
                .sendSmsCaptcha(PHONE, DURATION);

        assertThat(result.getStatus()).isEqualTo("fail");
        verify(cacheService).delete(SMS_CACHE_KEY);
        verify(cacheService, never()).set(eq(SMS_INTERVAL_KEY), any(), any(Duration.class));
    }

    @Test
    void shouldKeepCachedCodeAndIntervalAfterSmsSenderSucceeds() {
        ICacheService cacheService = mock(ICacheService.class);
        SmsCaptchaSender sender = mock(SmsCaptchaSender.class);
        when(sender.sendVerificationCode(eq(PHONE), anyString(), eq(DURATION))).thenReturn(true);

        SmsCaptchaResult result = service(cacheService, false, "prod", Optional.of(sender))
                .sendSmsCaptcha(PHONE, DURATION);

        ArgumentCaptor<Object> codeCaptor = ArgumentCaptor.forClass(Object.class);
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getCode()).isNull();
        verify(cacheService).set(eq(SMS_CACHE_KEY), codeCaptor.capture(), eq(DURATION));
        verify(sender).sendVerificationCode(PHONE, String.valueOf(codeCaptor.getValue()), DURATION);
        verify(cacheService).set(eq(SMS_INTERVAL_KEY), anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    void shouldUseLocalSimulationWhenDevelopmentEchoIsEnabled() {
        ICacheService cacheService = mock(ICacheService.class);
        SmsCaptchaSender sender = mock(SmsCaptchaSender.class);

        SmsCaptchaResult result = service(cacheService, true, "local", Optional.of(sender))
                .sendSmsCaptcha(PHONE, DURATION);

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getCode()).matches("\\d{6}");
        verifyNoInteractions(sender);
        verify(cacheService).set(eq(SMS_INTERVAL_KEY), anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    void shouldDeleteSmsCaptchaOnlyAfterSuccessfulValidation() {
        ICacheService cacheService = mock(ICacheService.class);
        when(cacheService.get(SMS_CACHE_KEY)).thenReturn("123456");
        CaptchaServiceImpl service = service(cacheService, false, "prod", Optional.empty());

        assertThat(service.validateAndDeleteSmsCaptcha(PHONE, "654321")).isFalse();
        verify(cacheService, never()).delete(SMS_CACHE_KEY);

        assertThat(service.validateAndDeleteSmsCaptcha(PHONE, "123456")).isTrue();
        verify(cacheService).delete(SMS_CACHE_KEY);
    }

    @Test
    void shouldBindSignedSliderChallengeToRequest() {
        ICacheService cacheService = mock(ICacheService.class);
        CaptchaServiceImpl captchaService = service(cacheService, false, "test", Optional.empty());
        MockHttpServletRequest request = request("10.0.0.1", "forge-test", null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            SliderCaptchaResult result = captchaService.generateSliderCaptcha(DURATION);
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(cacheService).set(eq("captcha:slider:" + result.getCodeKey()), payloadCaptor.capture(), eq(DURATION));
            String payload = String.valueOf(payloadCaptor.getValue());
            when(cacheService.get("captcha:slider:" + result.getCodeKey())).thenReturn(payload);

            assertThat(captchaService.validateSliderCaptcha(result.getCodeKey(), extractExpectedX(payload))).isTrue();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void shouldRejectSliderChallengeFromDifferentRequestBinding() {
        ICacheService cacheService = mock(ICacheService.class);
        CaptchaServiceImpl captchaService = service(cacheService, false, "test", Optional.empty());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                request("10.0.0.1", "forge-test", null)));

        try {
            SliderCaptchaResult result = captchaService.generateSliderCaptcha(DURATION);
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(cacheService).set(eq("captcha:slider:" + result.getCodeKey()), payloadCaptor.capture(), eq(DURATION));
            when(cacheService.get("captcha:slider:" + result.getCodeKey()))
                    .thenReturn(String.valueOf(payloadCaptor.getValue()));

            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                    request("10.0.0.2", "forge-test", null)));
            assertThat(captchaService.validateSliderCaptcha(result.getCodeKey(), extractExpectedX(payloadCaptor.getValue().toString())))
                    .isFalse();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void shouldRejectTamperedSliderChallengeSignature() {
        ICacheService cacheService = mock(ICacheService.class);
        CaptchaServiceImpl captchaService = service(cacheService, false, "test", Optional.empty());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                request("10.0.0.1", "forge-test", null)));

        try {
            SliderCaptchaResult result = captchaService.generateSliderCaptcha(DURATION);
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(cacheService).set(eq("captcha:slider:" + result.getCodeKey()), payloadCaptor.capture(), eq(DURATION));
            String[] parts = payloadCaptor.getValue().toString().split("\\.", -1);
            parts[4] = "tampered";
            when(cacheService.get("captcha:slider:" + result.getCodeKey()))
                    .thenReturn(String.join(".", parts));

            assertThat(captchaService.validateSliderCaptcha(result.getCodeKey(), Integer.parseInt(parts[1]))).isFalse();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void shouldRequireExplicitSecretOutsideDevelopmentProfiles() {
        ICacheService cacheService = mock(ICacheService.class);
        CaptchaServiceImpl captchaService = service(cacheService, false, "prod", Optional.empty());

        assertThatThrownBy(() -> captchaService.generateSliderCaptcha(DURATION))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("CAPTCHA_CHALLENGE_SECRET_REQUIRED");
    }

    @Test
    void shouldRejectShortExplicitChallengeSecret() {
        ICacheService cacheService = mock(ICacheService.class);
        CaptchaProperties properties = new CaptchaProperties();
        properties.setChallengeSecret("too-short");
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
        CaptchaServiceImpl captchaService = new CaptchaServiceImpl(cacheService, properties, environment,
                Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> captchaService.generateSliderCaptcha(DURATION))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("CAPTCHA_CHALLENGE_SECRET_TOO_SHORT");
    }

    @Test
    void shouldFailClosedWhenEmailSenderIsUnavailable() {
        ICacheService cacheService = mock(ICacheService.class);

        EmailCaptchaResult result = service(cacheService, false, "prod", Optional.empty())
                .sendEmailCaptcha(EMAIL, DURATION);

        assertThat(result.getStatus()).isEqualTo("fail");
        assertThat(result.getCode()).isNull();
        verify(cacheService).delete(EMAIL_CACHE_KEY);
        verify(cacheService, never()).set(eq(EMAIL_INTERVAL_KEY), any(), any(Duration.class));
    }

    @Test
    void shouldKeepCachedCodeAfterEmailSenderSucceeds() {
        ICacheService cacheService = mock(ICacheService.class);
        EmailCaptchaSender sender = mock(EmailCaptchaSender.class);
        when(sender.sendVerificationCode(eq(EMAIL), anyString(), eq(DURATION))).thenReturn(true);

        EmailCaptchaResult result = serviceWithEmail(cacheService, false, "prod", Optional.of(sender))
                .sendEmailCaptcha(EMAIL, DURATION);

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getCode()).isNull();
        verify(cacheService).set(eq(EMAIL_CACHE_KEY), anyString(), eq(DURATION));
        verify(cacheService).set(eq(EMAIL_INTERVAL_KEY), anyString(), eq(Duration.ofSeconds(60)));
    }

    private CaptchaServiceImpl service(ICacheService cacheService, boolean echoEnabled,
                                       String profile, Optional<SmsCaptchaSender> sender) {
        return service(cacheService, echoEnabled, profile, sender, Optional.empty());
    }

    private CaptchaServiceImpl serviceWithEmail(ICacheService cacheService, boolean echoEnabled,
                                                String profile, Optional<EmailCaptchaSender> emailSender) {
        return service(cacheService, echoEnabled, profile, Optional.empty(), emailSender);
    }

    private CaptchaServiceImpl service(ICacheService cacheService, boolean echoEnabled,
                                       String profile, Optional<SmsCaptchaSender> sender,
                                       Optional<EmailCaptchaSender> emailSender) {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setDevEchoCode(echoEnabled);
        MockEnvironment environment = new MockEnvironment();
        if (profile != null) {
            environment.setActiveProfiles(profile);
        }
        return new CaptchaServiceImpl(cacheService, properties, environment, sender, emailSender);
    }

    private MockHttpServletRequest request(String remoteAddress, String userAgent, String sessionId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        request.addHeader("User-Agent", userAgent);
        if (sessionId != null) {
            request.setRequestedSessionId(sessionId);
        }
        return request;
    }

    private int extractExpectedX(String payload) {
        return Integer.parseInt(payload.split("\\.", -1)[1]);
    }
}
