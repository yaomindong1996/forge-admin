package com.mdframe.forge.starter.web;

import com.mdframe.forge.starter.core.config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 两代 mapper 同时存在时，真实 MVC 仍必须使用 Forge 的 Jackson 2 字段协议。 */
class JacksonHttpContractTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonConfig.class, Jackson2AutoConfiguration.class,
                    JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
                    WebMvcAutoConfiguration.class))
            .withUserConfiguration(EndpointConfiguration.class)
            .withPropertyValues("spring.http.converters.preferred-json-mapper=jackson2",
                    "spring.jackson2.deserialization.fail-on-unknown-properties=false");

    @Test
    void preservesLargeIdentifiersMoneyAndDateFormatting() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            String result = mvc.perform(get("/t1/contract")).andReturn().getResponse().getContentAsString();
            assertThat(result).contains("\"id\":\"2100942720360046593\"", "\"smallId\":42",
                    "\"amount\":\"1234.50\"", "\"time\":\"2026-10-11 12:30:45\"");
        });
    }

    @Test
    void acceptsLegacyAndIsoDateInputsWithoutChangingOutput() {
        runner.run(context -> {
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            for (String value : new String[]{"2026-10-11 12:30:45", "2026-10-11T12:30:45",
                    "2026-10-11T12:30:45Z"}) {
                var response = mvc.perform(post("/t1/contract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"time\":\"" + value + "\",\"unknown\":1}"))
                        .andReturn().getResponse();
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(response.getContentAsString()).contains("2026-10-11 12:30:45");
            }
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class EndpointConfiguration {
        @Bean
        Endpoint endpoint() {
            return new Endpoint();
        }
    }

    @RestController
    static class Endpoint {
        @GetMapping("/t1/contract")
        Payload getPayload() {
            return new Payload(2100942720360046593L, 42L, new BigDecimal("1234.50"),
                    LocalDateTime.of(2026, 10, 11, 12, 30, 45));
        }

        @PostMapping("/t1/contract")
        Payload echo(@RequestBody Payload payload) {
            return payload;
        }
    }

    record Payload(Long id, Long smallId, BigDecimal amount, LocalDateTime time) { }
}
