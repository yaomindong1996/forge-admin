package com.mdframe.forge.plugin.ai.chat.memory;

import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于数据库的 ChatMemory 实现
 * 将对话历史持久化到 ai_chat_record 表，支持跨请求的多轮上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbChatMemory implements ChatMemory {

    /** 默认保留的上下文消息条数（太多会超出 token 限制） */
    private static final int DEFAULT_CONTEXT_WINDOW = 20;

    private final AiChatRecordService recordService;

    @Override
    public void add(String conversationId, Message message) {
        ChatMemory.super.add(conversationId, message);
    }

    /**
     * 当前由 AiChatService 在流结束后统一落库，这里仅保留接口实现，避免重复写入。
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        log.debug("[DbChatMemory] 跳过默认写入，等待服务层统一持久化, sessionId={}, messageCount={}", conversationId, messages.size());
    }

    /**
     * 查询指定会话最近 N 条消息，倒序取后正序返回
     */
    @Override
    public List<Message> get(String conversationId) {
        List<AiChatRecord> records = recordService.listRecentBySession(conversationId, DEFAULT_CONTEXT_WINDOW);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.reverse(records);
        List<Message> messages = new ArrayList<>();
        for (AiChatRecord record : records) {
            Message msg = toMessage(record);
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }

    /**
     * 删除指定会话的所有历史消息
     */
    @Override
    public void clear(String conversationId) {
        recordService.removeBySession(conversationId);
        log.info("[DbChatMemory] 清除会话历史, sessionId={}", conversationId);
    }

    private Message toMessage(AiChatRecord record) {
        String role = record.getRole();
        String content = record.getContent();
        if (content == null) {
            return null;
        }
        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            default -> {
                log.warn("[DbChatMemory] 未知角色: {}", role);
                yield null;
            }
        };
    }
}
