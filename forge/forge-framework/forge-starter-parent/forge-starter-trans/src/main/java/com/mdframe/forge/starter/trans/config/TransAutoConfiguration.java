package com.mdframe.forge.starter.trans.config;

import com.mdframe.forge.starter.trans.handler.impl.DefaultDictTransHandler;
import com.mdframe.forge.starter.trans.handler.impl.EnumTransHandler;
import com.mdframe.forge.starter.trans.handler.impl.ExpressionTransHandler;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 翻译模块自动配置
 */
@Configuration
public class TransAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExpressionTransHandler expressionTransHandler() {
        return new ExpressionTransHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public EnumTransHandler enumTransHandler() {
        return new EnumTransHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultDictTransHandler defaultDictTransHandler(DictValueProvider dictValueProvider) {
        return new DefaultDictTransHandler(dictValueProvider);
    }
}
