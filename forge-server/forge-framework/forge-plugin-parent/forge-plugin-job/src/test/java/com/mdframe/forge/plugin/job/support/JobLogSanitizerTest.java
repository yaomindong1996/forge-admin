package com.mdframe.forge.plugin.job.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobLogSanitizerTest {

    private JobLogSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new JobLogSanitizer(new ObjectMapper());
    }

    @Test
    void shouldRedactSensitiveJsonFieldsAndPhoneNumbers() {
        String source = "{\"accessToken\":\"secret-token\",\"nested\":{\"password\":\"123456\"},"
                + "\"mobile\":\"13800138000\",\"message\":\"联系人 13912345678\"}";

        String sanitized = sanitizer.sanitizeJobParam(source);

        assertFalse(sanitized.contains("secret-token"));
        assertFalse(sanitized.contains("123456"));
        assertFalse(sanitized.contains("13800138000"));
        assertFalse(sanitized.contains("13912345678"));
        assertTrue(sanitized.contains("****"));
        assertTrue(sanitized.contains("139****5678"));
    }

    @Test
    void shouldRedactAuthorizationAndBearerTokensInPlainText() {
        String source = "Authorization: Bearer abc.def.ghi, token=raw-token";

        String sanitized = sanitizer.sanitizeResult(source);

        assertFalse(sanitized.contains("abc.def.ghi"));
        assertFalse(sanitized.contains("raw-token"));
        assertTrue(sanitized.contains("Authorization: ****"));
        assertTrue(sanitized.contains("token=****"));
    }

    @Test
    void shouldCapParameterResultAndExceptionLengths() {
        String longText = "x".repeat(6000);

        assertEquals(JobLogSanitizer.MAX_JOB_PARAM_LENGTH,
                sanitizer.sanitizeJobParam(longText).length());
        assertEquals(JobLogSanitizer.MAX_RESULT_LENGTH,
                sanitizer.sanitizeResult(longText).length());
        assertEquals(JobLogSanitizer.MAX_EXCEPTION_LENGTH,
                sanitizer.sanitizeException(longText).length());
        assertTrue(sanitizer.sanitizeException(longText).endsWith(JobLogSanitizer.TRUNCATED_SUFFIX));
    }

    @Test
    void shouldPreserveNullAndOrdinaryText() {
        assertNull(sanitizer.sanitizeJobParam(null));
        assertEquals("任务执行完成", sanitizer.sanitizeResult("任务执行完成"));
    }
}
