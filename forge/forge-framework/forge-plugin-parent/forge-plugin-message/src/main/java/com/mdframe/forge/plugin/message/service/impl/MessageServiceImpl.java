package com.mdframe.forge.plugin.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.MessageSendScope;
import com.mdframe.forge.plugin.message.domain.dto.MessageQueryDTO;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageSendRecord;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;
import com.mdframe.forge.plugin.message.domain.vo.MessageVO;
import com.mdframe.forge.plugin.message.domain.vo.UnreadCountVO;
import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageSendRecordMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageTemplateMapper;
import com.mdframe.forge.plugin.message.service.MessageReceiverResolver;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.plugin.message.service.SysMessageReceiverService;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import com.mdframe.forge.starter.message.service.MessageTemplateEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<SysMessageMapper,SysMessage> implements MessageService {

    private final SysMessageMapper messageMapper;
    private final SysMessageReceiverMapper receiverMapper;
    private final SysMessageSendRecordMapper recordMapper;
    private final SysMessageTemplateMapper templateMapper;
    private final MessageClient messageClient;
    private final MessageTemplateEngine templateEngine;
    private final MessageReceiverResolver receiverResolver;
    private final SysMessageReceiverService sysMessageReceiverService;

    public MessageServiceImpl(SysMessageMapper messageMapper,
                              SysMessageReceiverMapper receiverMapper,
                              SysMessageSendRecordMapper recordMapper,
                              SysMessageTemplateMapper templateMapper,
                              MessageClient messageClient,
                              MessageTemplateEngine templateEngine,
                              MessageReceiverResolver receiverResolver,
            SysMessageReceiverService sysMessageReceiverService) {
        this.messageMapper = messageMapper;
        this.receiverMapper = receiverMapper;
        this.recordMapper = recordMapper;
        this.templateMapper = templateMapper;
        this.messageClient = messageClient;
        this.templateEngine = templateEngine;
        this.receiverResolver = receiverResolver;
        this.sysMessageReceiverService = sysMessageReceiverService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMessage send(MessageSendRequestDTO req) {
        // 1. 渲染消息内容（处理模板）
        RenderResult renderResult = renderMessageContent(req);
        
        // 2. 创建消息主记录
        SysMessage msg = createMessageRecord(req, renderResult);
        
        // 3. 批量创建接收人记录（优化：使用回调方式避免内存溢出）
        int receiverCount = batchCreateReceiverRecords(msg.getId(), req);
        
        // 4. 发送消息到渠道（对于WEB站内信，无需调用第三方）
        MessageChannel.SendResult sendResult = sendToChannel(msg, renderResult, req);
        
        // 5. 创建发送记录
        createSendRecord(msg.getId(), req.getChannel(), receiverCount, sendResult);
        
        return msg;
    }
    
    /**
     * 渲染消息内容（处理模板）
     */
    private RenderResult renderMessageContent(MessageSendRequestDTO req) {
        String title = req.getTitle();
        String content = req.getContent();
        String channel = req.getChannel();
        
        if (StrUtil.isNotBlank(req.getTemplateCode())) {
            SysMessageTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<SysMessageTemplate>()
                    .eq(SysMessageTemplate::getTemplateCode, req.getTemplateCode())
                    .eq(SysMessageTemplate::getEnabled, 1)
            );
            if (template != null) {
                if (StrUtil.isBlank(title) && StrUtil.isNotBlank(template.getTitleTemplate())) {
                    title = templateEngine.render(template.getTitleTemplate(), req.getParams());
                }
                if (StrUtil.isBlank(content)) {
                    content = templateEngine.render(template.getContentTemplate(), req.getParams());
                }
                if (StrUtil.isBlank(channel)) {
                    channel = template.getDefaultChannel();
                }
            }
        }
        
        return new RenderResult(title, content, channel);
    }
    
    /**
     * 创建消息主记录
     */
    private SysMessage createMessageRecord(MessageSendRequestDTO req, RenderResult renderResult) {
        SysMessage msg = new SysMessage();
        msg.setTitle(renderResult.title);
        msg.setContent(renderResult.content);
        msg.setType(req.getType());
        msg.setSendScope(req.getSendScope());
        msg.setSendChannel(renderResult.channel);
        msg.setTemplateCode(req.getTemplateCode());
        msg.setTemplateParams(req.getParams());
        msg.setStatus(0); // 初始状态：发送中
        messageMapper.insert(msg);
        return msg;
    }
    
    /**
     * 批量创建接收人记录（优化：使用回调方式处理，避免内存溢出）
     * @return 接收人总数
     */
    private int batchCreateReceiverRecords(Long messageId, MessageSendRequestDTO req) {
        final int BATCH_SIZE = 500; // 每批次处理500条
        final int[] totalCount = {0}; // 使用数组包装以便在lambda中修改
        
        // 使用回调方式批量处理用户ID，避免一次性加载所有数据到内存
        receiverResolver.processUsersByScope(
            req.getSendScope(),
            req.getUserIds(),
            req.getOrgIds(),
            req.getTenantIds(),
            (userIdBatch) -> {
                // 批量插入接收人记录
                List<SysMessageReceiver> receivers = new ArrayList<>(userIdBatch.size());
                LocalDateTime now = LocalDateTime.now();
                
                for (Long userId : userIdBatch) {
                    SysMessageReceiver receiver = new SysMessageReceiver();
                    receiver.setMessageId(messageId);
                    receiver.setUserId(userId);
                    receiver.setReadFlag(0);
                    receiver.setCreateTime(now);
                    receivers.add(receiver);
                }
                
                // 批量插入
                if (!receivers.isEmpty()) {
                    receivers.forEach(receiverMapper::insert);
                    totalCount[0] += receivers.size();
                }
            },
            BATCH_SIZE
        );
        
        return totalCount[0];
    }
    
    /**
     * 发送消息到指定渠道
     */
    private MessageChannel.SendResult sendToChannel(SysMessage msg, RenderResult renderResult, MessageSendRequestDTO req) {
        // 对于WEB站内信，不需要调用第三方渠道
        if ("WEB".equals(renderResult.channel)) {
            MessageChannel.SendResult result = new MessageChannel.SendResult();
            result.success = true;
            result.msg = "站内信发送成功";
            return result;
        }
        
        // 对于SMS/EMAIL/PUSH等渠道，需要调用第三方服务
        // 注意：这里不传递所有userIds，避免内存溢出
        MessageChannel.SendRequest sr = new MessageChannel.SendRequest();
        sr.title = renderResult.title;
        sr.content = renderResult.content;
        sr.templateCode = req.getTemplateCode();
        sr.params = req.getParams();
        sr.channel = renderResult.channel;
        sr.type = req.getType();
        // userIds留空，由渠道服务根据messageId查询接收人
        
        return messageClient.send(sr);
    }
    
    /**
     * 创建发送记录
     */
    private void createSendRecord(Long messageId, String channel, int receiverCount, MessageChannel.SendResult result) {
        SysMessageSendRecord record = new SysMessageSendRecord();
        record.setMessageId(messageId);
        record.setChannel(channel);
        record.setReceiverCount(receiverCount);
        record.setSuccessCount(result.success ? receiverCount : 0);
        record.setFailCount(result.success ? 0 : receiverCount);
        record.setExternalId(result.externalId);
        record.setStatus(result.success ? 1 : 2);
        record.setErrorMsg(result.msg);
        record.setSendTime(LocalDateTime.now());
        recordMapper.insert(record);
        
        // 更新消息状态
        SysMessage updateMsg = new SysMessage();
        updateMsg.setId(messageId);
        updateMsg.setStatus(result.success ? 1 : 2); // 1-已发送，2-发送失败
        messageMapper.updateById(updateMsg);
    }
    
    /**
     * 消息渲染结果
     */
    private static class RenderResult {
        String title;
        String content;
        String channel;
        
        RenderResult(String title, String content, String channel) {
            this.title = title;
            this.content = content;
            this.channel = channel;
        }
    }

    @Override
    public void markRead(Long messageId, Long userId) {
        SysMessageReceiver receiver = receiverMapper.selectOne(
            new LambdaQueryWrapper<SysMessageReceiver>()
                .eq(SysMessageReceiver::getMessageId, messageId)
                .eq(SysMessageReceiver::getUserId, userId)
        );
        if (receiver != null && receiver.getReadFlag() == 0) {
            receiver.setReadFlag(1);
            receiver.setReadTime(LocalDateTime.now());
            receiverMapper.updateById(receiver);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReadBatch(List<Long> messageIds, Long userId) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        List<SysMessageReceiver> receivers = receiverMapper.selectList(
            new LambdaQueryWrapper<SysMessageReceiver>()
                .in(SysMessageReceiver::getMessageId, messageIds)
                .eq(SysMessageReceiver::getUserId, userId)
                .eq(SysMessageReceiver::getReadFlag, 0)
        );
        if (!receivers.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            receivers.forEach(r -> {
                r.setReadFlag(1);
                r.setReadTime(now);
            });
            receivers.forEach(receiverMapper::updateById);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        List<SysMessageReceiver> receivers = receiverMapper.selectList(
            new LambdaQueryWrapper<SysMessageReceiver>()
                .eq(SysMessageReceiver::getUserId, userId)
                .eq(SysMessageReceiver::getReadFlag, 0)
        );
        if (!receivers.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            receivers.forEach(r -> {
                r.setReadFlag(1);
                r.setReadTime(now);
            });
            receivers.forEach(receiverMapper::updateById);
        }
    }

    @Override
    public IPage<MessageVO> pageUserMessages(Long userId, MessageQueryDTO query, Integer pageNum, Integer pageSize) {
        Page<SysMessageReceiver> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<SysMessageReceiver> wrapper = new LambdaQueryWrapper<SysMessageReceiver>()
            .eq(SysMessageReceiver::getUserId, userId);
        
        if (query.getReadFlag() != null) {
            wrapper.eq(SysMessageReceiver::getReadFlag, query.getReadFlag());
        }
        
        wrapper.orderByDesc(SysMessageReceiver::getCreateTime);
        
        Page<SysMessageReceiver> receiverPage = sysMessageReceiverService.page(page, wrapper);
        
        // 转换为VO
        List<MessageVO> vos = receiverPage.getRecords().stream().map(receiver -> {
            SysMessage message = messageMapper.selectById(receiver.getMessageId());
            MessageVO vo = new MessageVO();
            if (message != null) {
                vo.setId(message.getId());
                vo.setTitle(message.getTitle());
                vo.setContent(message.getContent());
                vo.setType(message.getType());
                vo.setSendChannel(message.getSendChannel());
                vo.setCreateTime(message.getCreateTime());
            }
            vo.setReadFlag(receiver.getReadFlag());
            vo.setReadTime(receiver.getReadTime());
            return vo;
        }).collect(Collectors.toList());
        
        Page<MessageVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(vos);
        result.setTotal(receiverPage.getTotal());
        return result;
    }

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        UnreadCountVO vo = new UnreadCountVO();
        
        // 查询未读接收记录
        List<SysMessageReceiver> unreadReceivers = receiverMapper.selectList(
            new LambdaQueryWrapper<SysMessageReceiver>()
                .eq(SysMessageReceiver::getUserId, userId)
                .eq(SysMessageReceiver::getReadFlag, 0)
        );
        
        vo.setTotalCount((long) unreadReceivers.size());
        
        // 统计各类型未读数
        Map<String, Long> typeCountMap = new HashMap<>();
        for (SysMessageReceiver receiver : unreadReceivers) {
            SysMessage message = messageMapper.selectById(receiver.getMessageId());
            if (message != null) {
                typeCountMap.put(message.getType(),
                    typeCountMap.getOrDefault(message.getType(), 0L) + 1);
            }
        }
        
        vo.setSystemCount(typeCountMap.getOrDefault("SYSTEM", 0L));
        vo.setSmsCount(typeCountMap.getOrDefault("SMS", 0L));
        vo.setEmailCount(typeCountMap.getOrDefault("EMAIL", 0L));
        
        return vo;
    }

    @Override
    public MessageVO getMessageDetail(Long messageId, Long userId) {
        SysMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            return null;
        }
        
        SysMessageReceiver receiver = receiverMapper.selectOne(
            new LambdaQueryWrapper<SysMessageReceiver>()
                .eq(SysMessageReceiver::getMessageId, messageId)
                .eq(SysMessageReceiver::getUserId, userId)
        );
        
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        
        if (receiver != null) {
            vo.setReadFlag(receiver.getReadFlag());
            vo.setReadTime(receiver.getReadTime());
        }
        
        return vo;
    }
}
