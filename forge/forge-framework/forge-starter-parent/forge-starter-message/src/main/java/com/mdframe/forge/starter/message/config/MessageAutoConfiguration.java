package com.mdframe.forge.starter.message.config;

import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.channel.SmsMessageChannel;
import com.mdframe.forge.starter.message.channel.WebMessageChannel;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import com.mdframe.forge.starter.message.service.MessageTemplateEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(MessageProperties.class)
public class MessageAutoConfiguration {
    
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
    @ConditionalOnProperty(prefix = "forge.message.channel.sms", name = "enabled", havingValue = "true")
    public MessageChannel smsMessageChannel() {
        return new SmsMessageChannel();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MessageClient messageClient(MessageTemplateEngine templateEngine, MessageProperties properties,
            Map<String, MessageChannel> channelBeans) {
        Map<String, MessageChannel> channels = new HashMap<>(channelBeans);
        return new MessageClient(templateEngine, properties, channels);
    }
}
