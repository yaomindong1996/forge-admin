package com.mdframe.forge.starter.auth.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaResponseSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldOmitNullCaptchaCodeFromJson() throws Exception {
        CaptchaResult result = CaptchaResult.builder()
                .codeKey("key-1")
                .image("image")
                .build();

        assertThat(objectMapper.writeValueAsString(result)).doesNotContain("\"code\"");
    }

    @Test
    void shouldOmitNullSmsCaptchaCodeFromJson() throws Exception {
        SmsCaptchaResult result = SmsCaptchaResult.builder()
                .codeKey("key-2")
                .status("success")
                .build();

        assertThat(objectMapper.writeValueAsString(result)).doesNotContain("\"code\"");
    }

    @Test
    void shouldSerializeCaptchaCodeWhenDevelopmentEchoIsAllowed() throws Exception {
        CaptchaResult result = CaptchaResult.builder().code("ABCD").build();
        SmsCaptchaResult smsResult = SmsCaptchaResult.builder().code("123456").build();

        assertThat(objectMapper.writeValueAsString(result)).contains("\"code\":\"ABCD\"");
        assertThat(objectMapper.writeValueAsString(smsResult)).contains("\"code\":\"123456\"");
    }
}
