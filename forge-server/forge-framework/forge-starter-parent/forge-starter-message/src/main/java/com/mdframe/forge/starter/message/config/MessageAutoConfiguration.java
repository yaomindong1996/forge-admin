package com.mdframe.forge.starter.message.config;

import com.mdframe.forge.starter.message.channel.EmailMessageChannel;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.channel.SmsMessageChannel;
import com.mdframe.forge.starter.message.channel.WebMessageChannel;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import com.mdframe.forge.starter.message.service.MessageTemplateEngine;
import com.mdframe.forge.starter.message.sms.LocalSmsRouteSequence;
import com.mdframe.forge.starter.message.sms.SmsRouteSequence;
import com.mdframe.forge.starter.message.sms.SmsRouter;
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

import java.util.List;

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
    
    @Bean
    @ConditionalOnMissingBean(SmsRouteSequence.class)
    public SmsRouteSequence smsRouteSequence() {
        return new LocalSmsRouteSequence();
    }

    @Bean
    @ConditionalOnBean(SmsConfigProvider.class)
    @ConditionalOnMissingBean(SmsRouter.class)
    public SmsRouter smsRouter(SmsConfigProvider smsConfigProvider, SmsRouteSequence smsRouteSequence) {
        return new SmsRouter(smsConfigProvider, smsRouteSequence);
    }

    @Bean(name = "smsMessageChannel")
    @ConditionalOnBean({SmsConfigProvider.class, SmsRouter.class})
    @ConditionalOnProperty(prefix = "forge.message.channel.sms", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MessageChannel smsMessageChannel(SmsRouter smsRouter, SmsConfigProvider smsConfigProvider) {
        return new SmsMessageChannel(smsRouter, smsConfigProvider);
    }

    @Bean
    @ConditionalOnBean(ReadConfig.class)
    public SmsBlendRefresher smsBlendRefresher(ReadConfig readConfig) {
        return new SmsBlendRefresher(readConfig);
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
