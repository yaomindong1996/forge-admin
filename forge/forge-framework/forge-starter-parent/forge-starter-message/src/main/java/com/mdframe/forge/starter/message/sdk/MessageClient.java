package com.mdframe.forge.starter.message.sdk;

import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.channel.MessageChannel.SendRequest;
import com.mdframe.forge.starter.message.config.MessageProperties;
import com.mdframe.forge.starter.message.service.MessageTemplateEngine;

import java.util.Map;

public class MessageClient {
    
    private final MessageTemplateEngine templateEngine;
    
    private final MessageProperties properties;
    
    private final Map<String, MessageChannel> channels;
    
    public MessageClient(MessageTemplateEngine templateEngine, MessageProperties properties,
            Map<String, MessageChannel> channels) {
        this.templateEngine = templateEngine;
        this.properties = properties;
        this.channels = channels;
        // 初始化渠道配置
        if (properties.getChannel() != null) {
            properties.getChannel().forEach((k, cfg) -> {
                MessageChannel c = channels.get(k + "MessageChannel");
                if (c != null) {
                    c.init(cfg.getConfig());
                }
            });
        }
    }
    
    public MessageChannel.SendResult send(SendRequest request) {
        // 渲染模板（如果提供了模板内容）
        if (request.content != null && request.params != null) {
            request.content = templateEngine.render(request.content, request.params);
        }
        String channelKey = request.channel != null ? request.channel : properties.getDefaultChannel();
        MessageChannel channel = resolveChannel(channelKey);
        if (channel == null) {
            return MessageChannel.SendResult.fail("channel not available: " + channelKey);
        }
        return channel.send(request);
    }
    
    private MessageChannel resolveChannel(String key) {
        // Bean 名称为 xxxMessageChannel
        MessageChannel channel = channels.get(key + "MessageChannel");
        if (channel == null) {
            channel = channels.get(key); // 兜底
        }
        return channel;
    }
}
