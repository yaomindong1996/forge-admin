package com.mdframe.forge.starter.message.config;

import com.mdframe.forge.starter.message.channel.EmailMessageChannel;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.channel.SmsMessageChannel;
import com.mdframe.forge.starter.message.channel.WebMessageChannel;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import com.mdframe.forge.starter.message.service.MessageTemplateEngine;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(MessageProperties.class)
public class MessageAutoConfiguration {
    
    @Autowired
    private ReadConfig readConfig;
    
    @Bean
    @ConditionalOnMissingBean
    public MessageTemplateEngine messageTemplateEngine() {
        return new MessageTemplateEngine();
    }
    
    @Bean(name = "webMessageChannel")
    @ConditionalOnProperty(prefix = "forge.message.channel.web", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MessageChannel webMessageChannel() {
        return new WebMessageChannel();
    }
    
    @Bean(name = "smsMessageChannel")
    @ConditionalOnBean(SmsConfigProvider.class)
    @ConditionalOnProperty(prefix = "forge.message.channel.sms", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MessageChannel smsMessageChannel() {
        return new SmsMessageChannel();
    }
    
    @Bean(name = "emailMessageChannel")
    @ConditionalOnBean(EmailConfigProvider.class)
    @ConditionalOnProperty(prefix = "forge.message.channel.email", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MessageChannel emailMessageChannel(EmailConfigProvider configProvider) {
        return new EmailMessageChannel(configProvider);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MessageClient messageClient(MessageTemplateEngine templateEngine, MessageProperties properties,
            List<MessageChannel> channels) {
        return new MessageClient(templateEngine, properties, channels);
    }
    
    @EventListener
    public void init(ContextRefreshedEvent event){
        SmsFactory.createSmsBlend(readConfig);
    }
}
