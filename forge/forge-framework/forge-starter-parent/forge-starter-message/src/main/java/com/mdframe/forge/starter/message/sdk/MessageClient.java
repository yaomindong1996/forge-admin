package com.mdframe.forge.starter.message.sdk;

import cn.hutool.core.collection.CollectionUtil;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.channel.MessageChannel.SendRequest;
import com.mdframe.forge.starter.message.config.MessageProperties;
import com.mdframe.forge.starter.message.service.MessageTemplateEngine;

import java.util.List;
import java.util.Map;

public class MessageClient {
    
    private final MessageTemplateEngine templateEngine;
    
    private final MessageProperties properties;
    
    private final List<MessageChannel> channels;
    
    public MessageClient(MessageTemplateEngine templateEngine, MessageProperties properties,
            List<MessageChannel> channels) {
        this.templateEngine = templateEngine;
        this.properties = properties;
        this.channels = channels;
        // 初始化渠道配置
        if (CollectionUtil.isNotEmpty(channels)) {
            channels.forEach(channel -> {
                channel.init(null);
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
        return channels.stream().filter(ch -> key.equals(ch.key().name())).findFirst()
                .orElseThrow(() -> new RuntimeException("channel not found: " + key));
    }
}
