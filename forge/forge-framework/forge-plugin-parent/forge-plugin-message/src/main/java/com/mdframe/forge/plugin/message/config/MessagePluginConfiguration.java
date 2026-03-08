package com.mdframe.forge.plugin.message.config;

import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageSendRecordMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageTemplateMapper;
import com.mdframe.forge.plugin.message.service.MessageReceiverResolver;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.plugin.message.service.MessageTemplateService;
import com.mdframe.forge.plugin.message.service.impl.MessageServiceImpl;
import com.mdframe.forge.plugin.message.service.impl.MessageTemplateServiceImpl;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import com.mdframe.forge.starter.message.service.MessageTemplateEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息插件配置
 */
@Configuration
public class MessagePluginConfiguration {
}
