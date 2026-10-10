package com.mdframe.forge.starter.crypto.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.core.util.MachineProtocolAttributes;
import com.mdframe.forge.starter.crypto.advice.DecryptRequestBodyAdvice;
import com.mdframe.forge.starter.crypto.advice.EncryptResponseBodyAdvice;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.keyexchange.SessionKeyStore;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MachineJsonProtocolTest {
    public Object endpoint(Object body) { return body; }
    @Test void verified_server_attribute_keeps_fixed_json_but_header_cannot_bypass_crypto() throws Exception {
        var properties = new CryptoProperties(); properties.setEnabled(true); properties.setEnableApiCrypto(true);
        var manager = mock(IApiConfigManager.class);
        var forced = new ApiConfigInfo(); forced.setNeedEncrypt(true);
        when(manager.getApiConfig(anyString(), anyString())).thenReturn(forced);
        var factory = mock(EncryptorFactory.class); var keys = mock(SessionKeyStore.class);
        var verifier = mock(InternalCallRequestVerifier.class);
        var decrypt = new DecryptRequestBodyAdvice(properties, factory, new ObjectMapper(), keys, manager, verifier);
        var encrypt = new EncryptResponseBodyAdvice(properties, factory, new ObjectMapper(), keys, manager, verifier);
        var method = getClass().getMethod("endpoint", Object.class);
        var req = new MockHttpServletRequest("POST", "/internal/plugin-delivery/test/finish");
        req.addHeader(MachineProtocolAttributes.VERIFIED_JSON, "true");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        try {
            assertThat(decrypt.supports(new MethodParameter(method, 0), Object.class,
                    MappingJackson2HttpMessageConverter.class)).isTrue();
            assertThat(encrypt.supports(new MethodParameter(method, -1),
                    MappingJackson2HttpMessageConverter.class)).isTrue();
            clearInvocations(manager, verifier);
            req.setAttribute(MachineProtocolAttributes.VERIFIED_JSON, Boolean.TRUE);
            assertThat(decrypt.supports(new MethodParameter(method, 0), Object.class,
                    MappingJackson2HttpMessageConverter.class)).isFalse();
            assertThat(encrypt.supports(new MethodParameter(method, -1),
                    MappingJackson2HttpMessageConverter.class)).isFalse();
            verifyNoInteractions(manager, verifier);
        } finally { RequestContextHolder.resetRequestAttributes(); }
    }
}
