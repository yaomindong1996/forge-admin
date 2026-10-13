package com.mdframe.forge.plugin.message.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.MessageSendScope;
import com.mdframe.forge.plugin.message.domain.MessageSendStatus;
import com.mdframe.forge.plugin.message.domain.dto.MessageQueryDTO;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageSendRecord;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;
import com.mdframe.forge.plugin.message.domain.vo.MessageVO;
import com.mdframe.forge.plugin.message.domain.vo.UnreadCountVO;
import com.mdframe.forge.plugin.message.event.MessageSendEvent;
import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageSendRecordMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageTemplateMapper;
import com.mdframe.forge.plugin.message.service.MessageReceiverResolver;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.plugin.message.service.SysMessageReceiverService;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import com.mdframe.forge.starter.message.service.MessageTemplateEngine;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<SysMessageMapper,SysMessage> implements MessageService {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private static final String CHANNEL_COLLABORATION = "COLLABORATION";

    private static final String DELIVERY_STATUS_PENDING = "PENDING";

    private final SysMessageMapper messageMapper;
    private final SysMessageReceiverMapper receiverMapper;
    private final SysMessageSendRecordMapper recordMapper;
    private final SysMessageTemplateMapper templateMapper;
    private final MessageClient messageClient;
    private final MessageTemplateEngine templateEngine;
    private final MessageReceiverResolver receiverResolver;
    private final SysMessageReceiverService sysMessageReceiverService;
    private final ISysUserService sysUserService;
    private final ApplicationEventPublisher eventPublisher;

    public MessageServiceImpl(SysMessageMapper messageMapper,
                              SysMessageReceiverMapper receiverMapper,
                              SysMessageSendRecordMapper recordMapper,
                              SysMessageTemplateMapper templateMapper,
                              MessageClient messageClient,
                              MessageTemplateEngine templateEngine,
                              MessageReceiverResolver receiverResolver,
            SysMessageReceiverService sysMessageReceiverService,
            ISysUserService sysUserService,
            ApplicationEventPublisher eventPublisher) {
        this.messageMapper = messageMapper;
        this.receiverMapper = receiverMapper;
        this.recordMapper = recordMapper;
        this.templateMapper = templateMapper;
        this.messageClient = messageClient;
        this.templateEngine = templateEngine;
        this.receiverResolver = receiverResolver;
        this.sysMessageReceiverService = sysMessageReceiverService;
        this.sysUserService = sysUserService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMessage send(MessageSendRequestDTO req) {
        Long tenantId = resolveTenantId();

        // 0. 幂等：相同幂等键并发只创建一份逻辑消息
        if (StrUtil.isNotBlank(req.getIdempotencyKey())) {
            SysMessage existing = messageMapper.selectByIdempotencyKey(tenantId, req.getIdempotencyKey());
            if (existing != null) {
                log.debug("消息幂等命中，跳过重复发送: idempotencyKey={}, messageId={}",
                        req.getIdempotencyKey(), existing.getId());
                return existing;
            }
        }

        // 1. 渲染消息内容（处理模板）
        RenderResult renderResult = renderMessageContent(req);
        boolean collaboration = CHANNEL_COLLABORATION.equals(renderResult.channel);
        
        // 2. 创建消息主记录（幂等键唯一索引兜底并发创建）
        SysMessage msg;
        try {
            msg = createMessageRecord(req, renderResult, tenantId);
        } catch (DuplicateKeyException e) {
            SysMessage existing = StrUtil.isNotBlank(req.getIdempotencyKey())
                    ? messageMapper.selectByIdempotencyKey(tenantId, req.getIdempotencyKey())
                    : null;
            if (existing != null) {
                log.debug("消息幂等并发命中: idempotencyKey={}, messageId={}",
                        req.getIdempotencyKey(), existing.getId());
                return existing;
            }
            throw e;
        }
        
        // 3. 批量创建接收人记录（协同渠道初始化 PENDING 投递状态）
        int receiverCount = batchCreateReceiverRecords(msg.getId(), req, tenantId,
                collaboration ? DELIVERY_STATUS_PENDING : null);
        
        // 4. 发布发送事件，事务提交后由监听器执行渠道发送（远程调用移出事务，避免长事务占 DB 连接）
        eventPublisher.publishEvent(new MessageSendEvent(msg, req, renderResult.title, renderResult.content,
                renderResult.channel, tenantId, receiverCount, collaboration));
        
        return msg;
    }

    /**
     * 事务提交后执行渠道发送（远程调用在事务外，不占用 DB 连接）。
     * 发送失败仅标记状态为失败并告警，不回滚已落库消息记录。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageSendEvent(MessageSendEvent event) {
        SysMessage msg = event.getMessage();
        try {
            RenderResult renderResult = new RenderResult(event.getRenderedTitle(), event.getRenderedContent(),
                    event.getChannel());
            if (event.isCollaboration()) {
                CollaborationDeliveryOutcome outcome = sendToCollaboration(msg, renderResult, event.getRequest());
                createCollaborationSendRecord(msg, event.getRequest(), event.getReceiverCount(), outcome,
                        event.getTenantId());
            } else {
                MessageChannel.SendResult sendResult = sendToChannel(msg, renderResult, event.getRequest());
                createSendRecord(msg.getId(), event.getRequest().getChannel(), event.getReceiverCount(),
                        sendResult, event.getTenantId());
            }
        } catch (Exception e) {
            log.error("消息发送失败，已标记为失败状态，请人工排查: messageId={}, channel={}",
                    msg.getId(), event.getChannel(), e);
            SysMessage updateMsg = new SysMessage();
            updateMsg.setId(msg.getId());
            updateMsg.setStatus(MessageSendStatus.FAILED.getCode());
            messageMapper.updateById(updateMsg);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMessage sendIfAbsent(MessageSendRequestDTO req, String bizType, String bizKey) {
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(bizKey)) {
            return send(req);
        }
        Long tenantId = resolveTenantId();
        SysMessage existing = TenantContextHolder.executeWithTenant(tenantId,
                () -> messageMapper.selectByBizTypeAndBizKey(bizType, bizKey));
        if (existing != null) {
            log.debug("消息已存在，跳过重复发送: bizType={}, bizKey={}, messageId={}",
                    bizType, bizKey, existing.getId());
            return existing;
        }
        req.setBizType(bizType);
        req.setBizKey(bizKey);
        return TenantContextHolder.executeWithTenant(tenantId, () -> send(req));
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
            if (template == null) {
                throw new BusinessException("消息模板不存在或已停用：" + req.getTemplateCode());
            }
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

        if (StrUtil.isBlank(title) && StrUtil.isBlank(content)) {
            throw new BusinessException("消息标题和内容不能同时为空");
        }
        
        return new RenderResult(title, content, channel);
    }
    
    /**
     * 创建消息主记录
     */
    private SysMessage createMessageRecord(MessageSendRequestDTO req, RenderResult renderResult, Long tenantId) {
        SysMessage msg = new SysMessage();
        msg.setTenantId(tenantId);
        msg.setTitle(renderResult.title);
        msg.setContent(renderResult.content);
        msg.setType(req.getType());
        msg.setSendScope(req.getSendScope());
        msg.setSendChannel(renderResult.channel);
        msg.setTemplateCode(req.getTemplateCode());
        msg.setTemplateParams(req.getParams());
        msg.setBizType(req.getBizType());
        msg.setBizKey(req.getBizKey());
        msg.setConnectionId(req.getConnectionId());
        msg.setIdempotencyKey(StrUtil.blankToDefault(req.getIdempotencyKey(), null));
        msg.setStatus(MessageSendStatus.SENDING.getCode());
        messageMapper.insert(msg);
        return msg;
    }
    
    /**
     * 批量创建接收人记录（优化：使用回调方式处理，避免内存溢出）
     *
     * @param deliveryStatus 初始投递状态；外部逐人投递渠道传 PENDING，站内信等传 null
     * @return 接收人总数
     */
    private int batchCreateReceiverRecords(Long messageId, MessageSendRequestDTO req, Long tenantId,
                                           String deliveryStatus) {
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
                    receiver.setTenantId(tenantId);
                    receiver.setMessageId(messageId);
                    receiver.setUserId(userId);
                    receiver.setReadFlag(0);
                    if (deliveryStatus != null) {
                        receiver.setDeliveryStatus(deliveryStatus);
                        receiver.setDeliveryAttempts(0);
                    }
                    receiver.setCreateTime(now);
                    receivers.add(receiver);
                }
                
                // 批量插入（saveBatch 批次刷入，避免逐条 insert 往返数据库）
                if (!receivers.isEmpty()) {
                    sysMessageReceiverService.saveBatch(receivers);
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
        sr.messageId = msg.getId();
        // 解析接收人
        List<SysMessageReceiver> sysMessageReceivers = sysMessageReceiverService.lambdaQuery()
                .eq(SysMessageReceiver::getMessageId, msg.getId()).list();
        if (CollectionUtil.isEmpty(sysMessageReceivers)) {
            MessageChannel.SendResult result = new MessageChannel.SendResult();
            result.success = true;
            result.msg = "发送成功";
            return result;
        }
        List<Long> userIds = sysMessageReceivers.stream().map(SysMessageReceiver::getUserId)
                .collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.lambdaQuery().in(SysUser::getId, userIds).list();
        List<String> phoneList = sysUsers.stream().map(SysUser::getPhone).filter(Objects::nonNull)
                .toList();
        sr.setPhoneList(phoneList);
        List<String> emilList = sysUsers.stream().map(SysUser::getEmail).filter(Objects::nonNull)
                .toList();
        sr.setEmailList(emilList);
        return messageClient.send(sr);
    }
    
    /**
     * 企业协同渠道逐人投递：调用协同渠道并将逐人结果落库；
     * 部分失败不影响已成功接收人，失败接收人由投递补偿任务按 next_retry_time 重试
     */
    private CollaborationDeliveryOutcome sendToCollaboration(SysMessage msg, RenderResult renderResult,
                                                             MessageSendRequestDTO req) {
        List<SysMessageReceiver> receivers = sysMessageReceiverService.lambdaQuery()
                .eq(SysMessageReceiver::getMessageId, msg.getId()).list();
        if (CollectionUtil.isEmpty(receivers)) {
            return new CollaborationDeliveryOutcome(0, 0, 0, null, null, null);
        }
        List<MessageChannel.ChannelRecipient> recipients = receivers.stream()
                .map(r -> MessageChannel.ChannelRecipient.of(r.getUserId()))
                .toList();
        MessageChannel.ChannelSendRequest request = new MessageChannel.ChannelSendRequest(
                msg.getTenantId(), req.getConnectionId(), msg.getId(), req.getIdempotencyKey(),
                recipients, renderResult.title, renderResult.content, req.getParams());
        MessageChannel.ChannelSendResult result = messageClient.sendToRecipients(CHANNEL_COLLABORATION, request);
        
        int sent = 0;
        int failed = 0;
        int skipped = 0;
        String firstError = null;
        LocalDateTime now = LocalDateTime.now();
        if (result != null && result.deliveries() != null) {
            for (MessageChannel.RecipientDeliveryResult delivery : result.deliveries()) {
                boolean isFailed = MessageChannel.RecipientDeliveryResult.STATUS_FAILED.equals(delivery.status());
                // 失败接收人给出下次重试时间，供投递补偿任务扫描
                LocalDateTime nextRetryTime = isFailed ? now.plusMinutes(1) : null;
                receiverMapper.updateDeliveryResult(msg.getId(), delivery.userId(), delivery.status(),
                        delivery.externalId(), delivery.errorCode(), now, nextRetryTime);
                if (MessageChannel.RecipientDeliveryResult.STATUS_SENT.equals(delivery.status())) {
                    sent++;
                } else if (isFailed) {
                    failed++;
                    if (firstError == null) {
                        firstError = StrUtil.blankToDefault(delivery.errorCode(), delivery.errorMessage());
                    }
                } else {
                    skipped++;
                }
            }
        }
        String providerRequestId = result != null ? result.providerRequestId() : null;
        String platform = result != null ? result.platform() : null;
        return new CollaborationDeliveryOutcome(sent, failed, skipped, providerRequestId, firstError, platform);
    }
    
    /**
     * 创建企业协同渠道发送记录并更新消息状态
     */
    private void createCollaborationSendRecord(SysMessage msg, MessageSendRequestDTO req, int receiverCount,
                                               CollaborationDeliveryOutcome outcome, Long tenantId) {
        SysMessageSendRecord record = new SysMessageSendRecord();
        record.setTenantId(tenantId);
        record.setMessageId(msg.getId());
        record.setConnectionId(req.getConnectionId());
        record.setPlatform(outcome.platform());
        record.setIdempotencyKey(req.getIdempotencyKey());
        record.setAttemptNo(1);
        record.setProviderRequestId(outcome.providerRequestId());
        record.setChannel(CHANNEL_COLLABORATION);
        record.setReceiverCount(receiverCount);
        record.setSuccessCount(outcome.sentCount());
        record.setFailCount(outcome.failedCount());
        MessageSendStatus sendStatus = resolveCollaborationStatus(receiverCount, outcome.sentCount(),
                outcome.failedCount(), outcome.skippedCount());
        record.setStatus(sendStatus.getCode());
        record.setErrorMsg(outcome.firstErrorMsg());
        record.setSendTime(LocalDateTime.now());
        recordMapper.insert(record);
        
        // 同时回写企业协同平台编码，供消息列表/投递记录区分平台
        SysMessage updateMsg = new SysMessage();
        updateMsg.setId(msg.getId());
        updateMsg.setStatus(sendStatus.getCode());
        updateMsg.setPlatform(outcome.platform());
        messageMapper.updateById(updateMsg);
        // 同步回写返回实例，调用方可据此判断投递结果（逐人失败不抛异常）
        msg.setStatus(updateMsg.getStatus());
        msg.setPlatform(outcome.platform());
    }

    static MessageSendStatus resolveCollaborationStatus(int receiverCount, int sentCount, int failedCount, int skippedCount) {
        if (receiverCount <= 0) {
            return MessageSendStatus.SUCCESS;
        }
        if (sentCount <= 0) {
            return MessageSendStatus.FAILED;
        }
        if (sentCount < receiverCount || failedCount > 0 || skippedCount > 0) {
            return MessageSendStatus.PARTIAL;
        }
        return MessageSendStatus.SUCCESS;
    }
    
    /**
     * 创建发送记录
     */
    private void createSendRecord(Long messageId, String channel, int receiverCount, MessageChannel.SendResult result,
                                  Long tenantId) {
        SysMessageSendRecord record = new SysMessageSendRecord();
        record.setTenantId(tenantId);
        record.setMessageId(messageId);
        record.setChannel(channel);
        record.setReceiverCount(receiverCount);
        record.setSuccessCount(result.success ? receiverCount : 0);
        record.setFailCount(result.success ? 0 : receiverCount);
        record.setExternalId(result.externalId);
        record.setStatus(result.success ? MessageSendStatus.SUCCESS.getCode() : MessageSendStatus.FAILED.getCode());
        record.setErrorMsg(result.msg);
        record.setSendTime(LocalDateTime.now());
        recordMapper.insert(record);
        
        // 更新消息状态
        SysMessage updateMsg = new SysMessage();
        updateMsg.setId(messageId);
        updateMsg.setStatus(result.success ? MessageSendStatus.SUCCESS.getCode() : MessageSendStatus.FAILED.getCode());
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

    /**
     * 企业协同渠道逐人投递汇总
     */
    private record CollaborationDeliveryOutcome(int sentCount, int failedCount, int skippedCount,
                                                String providerRequestId, String firstErrorMsg, String platform) {
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
        receiverMapper.markMessagesReadBatch(resolveTenantId(), userId, messageIds, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        receiverMapper.markAllMessagesRead(resolveTenantId(), userId, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markWebReadByBiz(String bizType, String bizKey) {
        // 流程办结等系统场景：按业务键清理该笔站内信全部收件人未读（任务已结束）
        return markWebReadByBiz(bizType, bizKey, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markWebReadByBiz(String bizType, String bizKey, Long userId) {
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(bizKey)) {
            return 0;
        }
        Long tenantId = resolveTenantId();
        Integer updated = TenantContextHolder.executeWithTenant(tenantId,
                () -> receiverMapper.markWebMessagesReadByBiz(bizType, bizKey, userId, LocalDateTime.now()));
        int count = updated == null ? 0 : updated;
        if (count > 0) {
            log.info("按业务键标记站内信已读: bizType={}, bizKey={}, userId={}, updated={}",
                    bizType, bizKey, userId, count);
        }
        return count;
    }

    @Override
    public IPage<MessageVO> pageUserMessages(Long userId, MessageQueryDTO query, Integer pageNum, Integer pageSize) {
        Page<MessageVO> page = new Page<>(pageNum, pageSize);
        MessageQueryDTO safeQuery = query == null ? new MessageQueryDTO() : query;
        
        IPage<MessageVO> messagePage = receiverMapper.selectUserWebMessages(
            page,
            userId,
            safeQuery.getReadFlag(),
            safeQuery.getType(),
            safeQuery.getKeyword(),
            safeQuery.getStartTime(),
            safeQuery.getEndTime()
        );
        
        return messagePage;
    }

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        UnreadCountVO vo = new UnreadCountVO();

        List<String> unreadTypes = receiverMapper.selectUnreadWebMessageTypes(userId);
        vo.setTotalCount((long) unreadTypes.size());

        Map<String, Long> typeCountMap = unreadTypes.stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.groupingBy(type -> type, Collectors.counting()));

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
        if (receiver == null) {
            throw new BusinessException(403, "无权查看该消息");
        }

        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        vo.setReadFlag(receiver.getReadFlag());
        vo.setReadTime(receiver.getReadTime());
        return vo;
    }

    @Override
    public String resolveEnabledTemplateCode(String... candidateCodes) {
        if (candidateCodes == null) {
            return null;
        }
        for (String code : candidateCodes) {
            if (StrUtil.isBlank(code)) {
                continue;
            }
            Long count = templateMapper.selectCount(
                    new LambdaQueryWrapper<SysMessageTemplate>()
                            .eq(SysMessageTemplate::getTemplateCode, code)
                            .eq(SysMessageTemplate::getEnabled, 1));
            if (count != null && count > 0) {
                return code;
            }
        }
        return null;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && tenantId > 0) {
            return tenantId;
        }
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception ignored) {
            tenantId = null;
        }
        return tenantId != null && tenantId > 0 ? tenantId : DEFAULT_TENANT_ID;
    }
}
