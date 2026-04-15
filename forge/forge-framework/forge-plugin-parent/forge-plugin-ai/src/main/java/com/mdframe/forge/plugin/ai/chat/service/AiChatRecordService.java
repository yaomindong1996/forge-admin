package com.mdframe.forge.plugin.ai.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.mapper.AiChatRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 对话记录 Service
 */
@Service
public class AiChatRecordService extends ServiceImpl<AiChatRecordMapper, AiChatRecord> {

    /**
     * 查询指定会话的所有消息，按时间升序
     */
    public List<AiChatRecord> listBySession(String sessionId) {
        return list(new LambdaQueryWrapper<AiChatRecord>()
                .eq(AiChatRecord::getSessionId, sessionId)
                .orderByAsc(AiChatRecord::getCreateTime));
    }

    /**
     * 查询指定会话最近 N 条消息（用于上下文拼装）
     */
    public List<AiChatRecord> listRecentBySession(String sessionId, int limit) {
        return list(new LambdaQueryWrapper<AiChatRecord>()
                .eq(AiChatRecord::getSessionId, sessionId)
                .orderByDesc(AiChatRecord::getCreateTime)
                .last("LIMIT " + limit));
    }

    /**
     * 删除指定会话所有消息
     */
    public void removeBySession(String sessionId) {
        remove(new LambdaQueryWrapper<AiChatRecord>()
                .eq(AiChatRecord::getSessionId, sessionId));
    }
}
