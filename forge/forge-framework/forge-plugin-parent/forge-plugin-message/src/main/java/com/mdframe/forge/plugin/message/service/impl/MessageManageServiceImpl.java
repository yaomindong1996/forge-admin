package com.mdframe.forge.plugin.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.message.domain.dto.MessageManageQueryDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageSendRecord;
import com.mdframe.forge.plugin.message.domain.vo.MessageDetailVO;
import com.mdframe.forge.plugin.message.domain.vo.MessageManageVO;
import com.mdframe.forge.plugin.message.domain.vo.ReceiverVO;
import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageSendRecordMapper;
import com.mdframe.forge.plugin.message.service.MessageManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageManageServiceImpl implements MessageManageService {

    private final SysMessageMapper messageMapper;
    private final SysMessageReceiverMapper receiverMapper;
    private final SysMessageSendRecordMapper sendRecordMapper;

    public MessageManageServiceImpl(
            SysMessageMapper messageMapper,
            SysMessageReceiverMapper receiverMapper,
            SysMessageSendRecordMapper sendRecordMapper) {
        this.messageMapper = messageMapper;
        this.receiverMapper = receiverMapper;
        this.sendRecordMapper = sendRecordMapper;
    }

    @Override
    public IPage<MessageManageVO> pageMessages(MessageManageQueryDTO query, Integer pageNum, Integer pageSize) {
        Page<SysMessage> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        
        if (StrUtil.isNotBlank(query.getType())) {
            wrapper.eq(SysMessage::getType, query.getType());
        }
        
        if (StrUtil.isNotBlank(query.getChannel())) {
            wrapper.eq(SysMessage::getSendChannel, query.getChannel());
        }
        
        if (query.getStatus() != null) {
            wrapper.eq(SysMessage::getStatus, query.getStatus());
        }
        
        if (StrUtil.isNotBlank(query.getStartTime())) {
            LocalDateTime startTime = LocalDateTime.parse(query.getStartTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            wrapper.ge(SysMessage::getCreateTime, startTime);
        }
        
        if (StrUtil.isNotBlank(query.getEndTime())) {
            LocalDateTime endTime = LocalDateTime.parse(query.getEndTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            wrapper.le(SysMessage::getCreateTime, endTime);
        }
        
        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.and(w -> w.like(SysMessage::getTitle, query.getKeyword())
                    .or()
                    .like(SysMessage::getContent, query.getKeyword()));
        }
        
        wrapper.orderByDesc(SysMessage::getCreateTime);
        
        Page<SysMessage> messagePage = messageMapper.selectPage(page, wrapper);
        
        List<MessageManageVO> vos = messagePage.getRecords().stream().map(msg -> {
            MessageManageVO vo = new MessageManageVO();
            vo.setId(msg.getId());
            vo.setTitle(msg.getTitle());
            vo.setType(msg.getType());
            vo.setChannel(msg.getSendChannel());
            vo.setStatus(msg.getStatus());
            vo.setCreateTime(msg.getCreateTime());
            vo.setSenderName(msg.getSenderName());
            
            List<SysMessageReceiver> receivers = receiverMapper.selectList(
                new LambdaQueryWrapper<SysMessageReceiver>()
                    .eq(SysMessageReceiver::getMessageId, msg.getId())
            );
            vo.setReceiverCount(receivers.size());
            vo.setReadCount((int) receivers.stream().filter(r -> r.getReadFlag() == 1).count());
            vo.setUnreadCount((int) receivers.stream().filter(r -> r.getReadFlag() == 0).count());
            
            return vo;
        }).collect(Collectors.toList());
        
        Page<MessageManageVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(vos);
        result.setTotal(messagePage.getTotal());
        return result;
    }

    @Override
    public MessageDetailVO getMessageDetail(Long messageId) {
        SysMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            return null;
        }
        
        SysMessageSendRecord sendRecord = sendRecordMapper.selectOne(
            new LambdaQueryWrapper<SysMessageSendRecord>()
                .eq(SysMessageSendRecord::getMessageId, messageId)
        );
        
        List<ReceiverVO> receiverVos = receiverMapper.selectReceiversWithUserInfo(messageId);
        
        MessageDetailVO detailVO = new MessageDetailVO();
        detailVO.setMessage(message);
        detailVO.setSendRecord(sendRecord);
        detailVO.setReceivers(receiverVos);
        
        return detailVO;
    }
}
