package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageSendRecordMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageTemplateMapper;
import com.mdframe.forge.plugin.message.service.MessageReceiverResolver;
import com.mdframe.forge.plugin.message.service.SysMessageReceiverService;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import com.mdframe.forge.starter.message.service.MessageTemplateEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplDetailAccessTest {

    @Mock
    private SysMessageMapper messageMapper;
    @Mock
    private SysMessageReceiverMapper receiverMapper;
    @Mock
    private SysMessageSendRecordMapper recordMapper;
    @Mock
    private SysMessageTemplateMapper templateMapper;
    @Mock
    private MessageClient messageClient;
    @Mock
    private MessageTemplateEngine templateEngine;
    @Mock
    private MessageReceiverResolver receiverResolver;
    @Mock
    private SysMessageReceiverService sysMessageReceiverService;
    @Mock
    private ISysUserService sysUserService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(
                messageMapper, receiverMapper, recordMapper, templateMapper,
                messageClient, templateEngine, receiverResolver,
                sysMessageReceiverService, sysUserService, eventPublisher);
    }

    @Test
    void getMessageDetailShouldRejectNonReceiver() {
        SysMessage message = new SysMessage();
        message.setId(10L);
        message.setTitle("secret");
        when(messageMapper.selectById(10L)).thenReturn(message);
        when(receiverMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.getMessageDetail(10L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权查看");
    }

    @Test
    void getMessageDetailShouldReturnForReceiver() {
        SysMessage message = new SysMessage();
        message.setId(10L);
        message.setTitle("hello");
        SysMessageReceiver receiver = new SysMessageReceiver();
        receiver.setReadFlag(0);
        when(messageMapper.selectById(10L)).thenReturn(message);
        when(receiverMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(receiver);

        assertThat(service.getMessageDetail(10L, 1L).getTitle()).isEqualTo("hello");
        assertThat(service.getMessageDetail(10L, 1L).getReadFlag()).isZero();
    }
}
