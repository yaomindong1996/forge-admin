package com.mdframe.forge.plugin.ai.session.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.mapper.AiChatSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 会话管理 Service
 */
@Service
public class AiChatSessionService extends ServiceImpl<AiChatSessionMapper, AiChatSession> {

    /**
     * 获取或创建会话（幂等）
     * 若 sessionId 对应的会话不存在，则自动创建
     *
     * @param sessionId  会话ID（UUID）
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @param agentCode  Agent 编码
     * @param firstMsg   首条消息（用于生成会话标题）
     * @return 会话实体
     */
    public AiChatSession getOrCreate(String sessionId, Long userId, Long tenantId, String agentCode, String firstMsg) {
        AiChatSession session = getById(sessionId);
        if (session == null) {
            String name = StringUtils.hasText(firstMsg)
                    ? (firstMsg.length() > 50 ? firstMsg.substring(0, 50) + "…" : firstMsg)
                    : "新对话";
            session = AiChatSession.builder()
                    .id(sessionId)
                    .tenantId(tenantId)
                    .userId(userId)
                    .agentCode(agentCode)
                    .sessionName(name)
                    .status("0")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            save(session);
        }
        return session;
    }

    /**
     * 查询用户的历史会话列表（正常状态，按更新时间倒序）
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<AiChatSession> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .eq(AiChatSession::getStatus, "0")
                .orderByDesc(AiChatSession::getUpdateTime));
    }

    /**
     * 软删除会话（设 status=1）
     *
     * @param sessionId 会话ID
     */
    public void deleteSession(String sessionId) {
        update(new LambdaUpdateWrapper<AiChatSession>()
                .set(AiChatSession::getStatus, "1")
                .set(AiChatSession::getUpdateTime, LocalDateTime.now())
                .eq(AiChatSession::getId, sessionId));
    }

    /**
     * 更新会话最后修改时间
     */
    public void touchSession(String sessionId) {
        update(new LambdaUpdateWrapper<AiChatSession>()
                .set(AiChatSession::getUpdateTime, LocalDateTime.now())
                .eq(AiChatSession::getId, sessionId));
    }
}
