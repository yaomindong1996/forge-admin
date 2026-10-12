package com.mdframe.forge.plugin.ai.chat.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.mapper.AiChatRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 对话记录 Service
 */
@Service
public class AiChatRecordService extends ServiceImpl<AiChatRecordMapper, AiChatRecord> {

    private static final int DEFAULT_HISTORY_LIMIT = 12;

    /**
     * 查询指定会话的所有消息，按时间升序。
     */
    public List<AiChatRecord> listBySession(String sessionId) {
        List<AiChatRecord> records = listRecentBySession(sessionId, Integer.MAX_VALUE);
        records.sort((a, b) -> a.getId().compareTo(b.getId()));
        return records;
    }

    /**
     * 按 HITL 中断标识查询等待确认的 assistant 消息，供 resume 接回原消息。
     */
    public AiChatRecord findByInterruptId(String interruptId) {
        return baseMapper.selectByInterruptId(interruptId);
    }

    /**
     * 查询会话内指定角色的最新一条未删除消息。
     */
    public AiChatRecord findLatestBySessionAndRole(String sessionId, String role) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(role)) {
            return null;
        }
        return baseMapper.selectLatestBySessionAndRole(sessionId, role);
    }

    /**
     * 查询会话内最新一条 assistant 消息。
     */
    public AiChatRecord findLatestAssistantBySession(String sessionId) {
        return findLatestBySessionAndRole(sessionId, "assistant");
    }

    /**
     * 查询会话内最新一条 user 消息。
     */
    public AiChatRecord findLatestUserBySession(String sessionId) {
        return findLatestBySessionAndRole(sessionId, "user");
    }

    /**
     * 查询会话内最近 N 条消息（用于上下文拼装）。
     */
    public List<AiChatRecord> listRecentBySession(String sessionId, int limit) {
        if (!StringUtils.hasText(sessionId) || limit <= 0) {
            return new ArrayList<>();
        }
        List<AiChatRecord> records = baseMapper.selectRecentBySession(sessionId, limit);
        Collections.reverse(records);
        return records;
    }

    /**
     * 查询会话内某条记录之前的最近 N 条消息（用于重试/编辑时截断上下文）。
     */
    public List<AiChatRecord> listRecentBySessionBeforeRecord(String sessionId, Long beforeRecordId, int limit) {
        if (!StringUtils.hasText(sessionId) || beforeRecordId == null || limit <= 0) {
            return new ArrayList<>();
        }
        List<AiChatRecord> records = baseMapper.selectRecentBySessionBeforeRecord(sessionId, beforeRecordId, limit);
        Collections.reverse(records);
        return records;
    }

    /**
     * 单条软删除。
     */
    public boolean softDeleteById(Long recordId) {
        if (recordId == null) {
            return false;
        }
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiChatRecord>()
                .setSql("del_flag = id")
                .set(AiChatRecord::getUpdateTime, LocalDateTime.now())
                .eq(AiChatRecord::getId, recordId));
    }

    /**
     * 从指定记录开始，软删除该记录及其之后的所有消息。
     */
    public int softDeleteFromRecordId(String sessionId, Long startRecordId) {
        if (!StringUtils.hasText(sessionId) || startRecordId == null) {
            return 0;
        }
        return baseMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiChatRecord>()
                        .setSql("del_flag = id")
                        .set(AiChatRecord::getUpdateTime, LocalDateTime.now())
                        .eq(AiChatRecord::getSessionId, sessionId)
                        .ge(AiChatRecord::getId, startRecordId)
                        .eq(AiChatRecord::getDelFlag, 0));
    }

    /**
     * 删除指定会话所有消息。
     */
    public void removeBySession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        baseMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiChatRecord>()
                        .setSql("del_flag = id")
                        .set(AiChatRecord::getUpdateTime, LocalDateTime.now())
                        .eq(AiChatRecord::getSessionId, sessionId)
                        .eq(AiChatRecord::getDelFlag, 0));
    }
}
