package com.mdframe.forge.plugin.ai.chat.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatMessageToolCall;
import com.mdframe.forge.plugin.ai.chat.mapper.AiChatMessageToolCallMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 对话工具调用明细 Service。
 * 查询走 Mapper XML，写入复用 MyBatis-Plus 内置能力。
 */
@Service
public class AiChatMessageToolCallService extends ServiceImpl<AiChatMessageToolCallMapper, AiChatMessageToolCall> {

    /**
     * 查询指定会话的全部工具调用（未删除）。
     */
    public List<AiChatMessageToolCall> listBySession(String sessionId) {
        return baseMapper.selectBySessionId(sessionId);
    }

    /**
     * 查询指定 assistant 消息的工具调用（未删除）。
     */
    public List<AiChatMessageToolCall> listByRecord(Long recordId) {
        return baseMapper.selectByRecordId(recordId);
    }
}
