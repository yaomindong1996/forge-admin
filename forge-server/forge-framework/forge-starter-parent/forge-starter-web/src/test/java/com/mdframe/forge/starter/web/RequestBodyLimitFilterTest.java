package com.mdframe.forge.starter.web;

import com.mdframe.forge.starter.core.exception.GlobalExceptionHandler;
import com.mdframe.forge.starter.web.config.RequestBodyLimitAutoConfiguration;
import com.mdframe.forge.starter.web.config.RequestBodyLimitExceptionHandler;
import com.mdframe.forge.starter.web.filter.RequestBodyLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class RequestBodyLimitFilterTest {

    @Test
    void rejectsKnownLengthBeforeTheBusinessChain() throws Exception {
        var request = new MockHttpServletRequest();
        request.setContent("12345".getBytes(StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();
        new RequestBodyLimitFilter(4).doFilter(request, response, (req, res) -> called.set(true));
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("请求体超过大小限制");
        assertThat(called.get()).isFalse();
    }

    @Test
    void boundsChunkedReadsAndReadersInsteadOfBufferingTheWholeBody() throws Exception {
        for (boolean reader : new boolean[]{false, true}) {
            var response = new MockHttpServletResponse();
            new RequestBodyLimitFilter(4).doFilter(chunked("123456"), response, (req, res) -> {
                if (reader) {
                    ((HttpServletRequest) req).getReader().readLine();
                } else {
                    req.getInputStream().readAllBytes();
                }
            });
            assertThat(response.getStatus()).isEqualTo(413);
        }
    }

    @Test
    void allowsExactlyTheLimitAndExplicitUnlimitedMode() throws Exception {
        for (long limit : new long[]{4, -1}) {
            var response = new MockHttpServletResponse();
            new RequestBodyLimitFilter(limit).doFilter(chunked("1234"), response, (req, res) -> {
                assertThat(req.getInputStream().readAllBytes()).isEqualTo("1234".getBytes(StandardCharsets.UTF_8));
            });
            assertThat(response.getStatus()).isEqualTo(200);
        }
        assertThatThrownBy(() -> new RequestBodyLimitFilter(-2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returns413WhenMvcWrapsTheReadFailureAndGlobalAdviceIsPresent() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new Endpoint())
                .setControllerAdvice(new GlobalExceptionHandler(), new RequestBodyLimitExceptionHandler())
                .addFilters(new RequestBodyLimitFilter(4)).build();
        var response = mvc.perform(post("/t1/body").contentType("application/json")
                .content("{\"value\":\"large\"}").with(request -> {
                    // 不带可信 Content-Length，模拟 chunked；必须在读流时阻断。
                    request.setContent(new byte[0]);
                    return chunked("{\"value\":\"large\"}");
                })).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("413", "请求体超过大小限制");
    }

    @Test
    void autoConfigurationKeepsTheEnvironmentVariableAndAllowsPerHostOverride() {
        var runner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RequestBodyLimitAutoConfiguration.class));
        runner.withPropertyValues("FORGE_MAX_HTTP_POST_SIZE=4B").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RequestBodyLimitExceptionHandler.class);
            var bean = context.getBean("requestBodyLimitFilter", FilterRegistrationBean.class);
            var response = new MockHttpServletResponse();
            bean.getFilter().doFilter(chunked("12345"), response, (req, res) -> req.getInputStream().readAllBytes());
            assertThat(response.getStatus()).isEqualTo(413);
        });
        runner.withPropertyValues("FORGE_MAX_HTTP_POST_SIZE=4B", "forge.web.max-body-size=8B").run(context -> {
            var bean = context.getBean("requestBodyLimitFilter", FilterRegistrationBean.class);
            var response = new MockHttpServletResponse();
            bean.getFilter().doFilter(chunked("12345"), response, (req, res) -> req.getInputStream().readAllBytes());
            assertThat(response.getStatus()).isEqualTo(200);
        });
    }

    private MockHttpServletRequest chunked(String content) {
        var request = new MockHttpServletRequest("POST", "/t1/body") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent(content.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    @RestController
    static class Endpoint {
        @PostMapping("/t1/body")
        Payload echo(@RequestBody Payload payload) {
            return payload;
        }
    }

    record Payload(String value) { }
}
