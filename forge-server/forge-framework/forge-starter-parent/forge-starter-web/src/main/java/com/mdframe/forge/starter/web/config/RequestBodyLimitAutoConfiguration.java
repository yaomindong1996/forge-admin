package com.mdframe.forge.starter.web.config;

import com.mdframe.forge.starter.web.filter.RequestBodyLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.util.unit.DataSize;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RequestBodyLimitAutoConfiguration {

    @Bean
    public FilterRegistrationBean<RequestBodyLimitFilter> requestBodyLimitFilter(
            @Value("${forge.web.max-body-size:${FORGE_MAX_HTTP_POST_SIZE:20MB}}") String size) {
        var registration = new FilterRegistrationBean<>(new RequestBodyLimitFilter(DataSize.parse(size).toBytes()));
        registration.setName("forgeRequestBodyLimitFilter");
        registration.addUrlPatterns("/*");
        // 先生成请求关联 ID / 安全响应头，再拒绝超限请求；早于加密过滤器读取原始正文。
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 30);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestBodyLimitExceptionHandler requestBodyLimitExceptionHandler() {
        return new RequestBodyLimitExceptionHandler();
    }
}
