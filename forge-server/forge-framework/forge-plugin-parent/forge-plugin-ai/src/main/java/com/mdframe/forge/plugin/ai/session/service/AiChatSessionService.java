package com.mdframe.forge.plugin.ai.session.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.enums.AiChatSessionStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import com.mdframe.forge.plugin.ai.session.dto.AiSessionPageQuery;
import com.mdframe.forge.plugin.ai.session.mapper.AiChatSessionMapper;
import com.mdframe.forge.plugin.ai.session.vo.AiExperienceMetricsVO;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionStatisticsVO;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionVO;
import com.mdframe.forge.plugin.ai.session.vo.DailyTrendItem;
import com.mdframe.forge.plugin.ai.session.vo.ExperienceTrendItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * AI 会话管理 Service
 */
@Service
@RequiredArgsConstructor
public class AiChatSessionService extends ServiceImpl<AiChatSessionMapper, AiChatSession> {

    private final AiChatRecordService recordService;

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
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
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
                    .status(AiChatSessionStatus.ACTIVE.getCode())
                    .pinned(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            try {
                if (!save(session)) {
                    throw new BusinessException("会话创建失败");
                }
            } catch (DuplicateKeyException ex) {
                // 不尝试读取其它租户的行，也不把同一主键重新归属当前用户。
                throw new BusinessException(409, "会话标识已被占用，请创建新会话");
            }
        } else {
            if (!java.util.Objects.equals(session.getUserId(), userId)
                    || !java.util.Objects.equals(session.getTenantId(), tenantId)
                    || !AiChatSessionStatus.ACTIVE.matches(session.getStatus())) {
                throw new BusinessException(403, "无权访问该会话或会话已删除");
            }
            update(new LambdaUpdateWrapper<AiChatSession>()
                    .set(AiChatSession::getUpdateTime, LocalDateTime.now())
                    .eq(AiChatSession::getId, sessionId));
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
     * 删除单条消息（软删除）。校验归属：仅消息所属用户可删，防止越权删除他人消息。
     *
     * @param recordId 消息记录ID
     * @param userId   当前登录用户ID（归属校验）
     * @return 是否成功（消息不存在/已删除/非本人时返回 false）
     */
    public boolean deleteMessage(Long recordId, Long userId) {
        if (recordId == null) {
            return false;
        }
        AiChatRecord record = recordService.getById(recordId);
        if (record == null) {
            return false;
        }
        if (userId != null && !userId.equals(record.getUserId())) {
            return false;
        }
        return recordService.softDeleteById(recordId);
    }

    /**
     * 置顶或取消置顶会话。
     */
    public boolean pinSession(String sessionId, Long userId, boolean pinned) {
        if (!StringUtils.hasText(sessionId)) {
            return false;
        }
        AiChatSession session = getById(sessionId);
        if (session == null || !EnableStatus.DISABLED.matches(session.getStatus())) {
            return false;
        }
        if (userId != null && !userId.equals(session.getUserId())) {
            return false;
        }
        return update(new LambdaUpdateWrapper<AiChatSession>()
                .set(AiChatSession::getPinned, pinned ? 1 : 0)
                .set(AiChatSession::getPinnedTime, pinned ? LocalDateTime.now() : null)
                .set(AiChatSession::getUpdateTime, LocalDateTime.now())
                .eq(AiChatSession::getId, sessionId));
    }

    /**
     * 更新会话最后修改时间
     */
    public void touchSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        update(new LambdaUpdateWrapper<AiChatSession>()
                .set(AiChatSession::getUpdateTime, LocalDateTime.now())
                .eq(AiChatSession::getId, sessionId));
    }

    /**
     * 显式创建会话（决策 29/30）：新会话即落库并绑定 Agent。
     * 幂等——已存在则仅刷新更新时间并返回原会话；标题由服务端规则生成（决策 34），前端不再自行截断。
     *
     * @param sessionId   会话ID（前端生成的 UUID）
     * @param userId      用户ID
     * @param tenantId    租户ID
     * @param agentCode   绑定的 Agent 编码
     * @param sessionName 会话标题（为空时默认“新对话”）
     * @return 会话实体
     */
    public AiChatSession createSession(String sessionId, Long userId, Long tenantId, String agentCode, String sessionName) {
        return getOrCreate(sessionId, userId, tenantId, agentCode, sessionName);
    }

    /**
     * 重命名会话（决策 31）：校验归属，仅本人可改。标题以服务端为准。
     *
     * @param sessionId   会话ID
     * @param userId      当前登录用户ID（归属校验）
     * @param sessionName 新标题
     * @return 是否成功
     */
    public boolean renameSession(String sessionId, Long userId, String sessionName) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(sessionName)) {
            return false;
        }
        AiChatSession session = getById(sessionId);
        if (session == null || !EnableStatus.DISABLED.matches(session.getStatus())) {
            return false;
        }
        if (userId != null && !userId.equals(session.getUserId())) {
            return false;
        }
        String name = sessionName.length() > 50 ? sessionName.substring(0, 50) + "…" : sessionName;
        return update(new LambdaUpdateWrapper<AiChatSession>()
                .set(AiChatSession::getSessionName, name)
                .set(AiChatSession::getUpdateTime, LocalDateTime.now())
                .eq(AiChatSession::getId, sessionId));
    }

    /**
     * 用户侧会话分页（决策 32）：复用 selectSessionPage（XML 查询，受租户/数据权限约束），
     * 强制按当前登录用户过滤，支持关键词与 AgentCode 过滤。
     *
     * @param query  分页与过滤条件
     * @param userId 当前登录用户ID
     * @return 分页结果
     */
    public Page<AiSessionVO> userPage(AiSessionPageQuery query, Long userId) {
        query.setUserId(userId);
        return baseMapper.selectSessionPage(query.toPage(), query);
    }

    public Page<AiSessionVO> adminPage(AiSessionPageQuery query) {
        return baseMapper.selectSessionPage(query.toPage(), query);
    }

    public AiSessionStatisticsVO getStatistics() {
        AiSessionStatisticsVO stats = baseMapper.selectStatistics();
        if (stats == null) {
            stats = new AiSessionStatisticsVO();
            stats.setTotalSessions(0L);
            stats.setTotalMessages(0L);
            stats.setTodaySessions(0L);
            stats.setTotalTokenUsage(0L);
        }
        List<DailyTrendItem> trend = baseMapper.selectDailyTrend();
        stats.setDailyTrend(trend);
        return stats;
    }

    /**
     * 对话体验指标（阶段四#4）：基于 ai_chat_record 中 assistant 回复的
     * 首字延迟/总耗时/token/状态聚合；完成率、错误率、中断率在此按计数派生。
     */
    public AiExperienceMetricsVO getExperienceMetrics() {
        AiExperienceMetricsVO metrics = baseMapper.selectExperienceMetrics();
        if (metrics == null) {
            metrics = new AiExperienceMetricsVO();
        }
        long total = metrics.getTotalReplies() != null ? metrics.getTotalReplies() : 0L;
        metrics.setTotalReplies(total);
        metrics.setCompletedCount(nz(metrics.getCompletedCount()));
        metrics.setErrorCount(nz(metrics.getErrorCount()));
        metrics.setAbortedCount(nz(metrics.getAbortedCount()));
        if (total > 0) {
            metrics.setCompletionRate(round4(metrics.getCompletedCount() / (double) total));
            metrics.setErrorRate(round4(metrics.getErrorCount() / (double) total));
            metrics.setAbortRate(round4(metrics.getAbortedCount() / (double) total));
        } else {
            metrics.setCompletionRate(0d);
            metrics.setErrorRate(0d);
            metrics.setAbortRate(0d);
        }
        metrics.setDailyTrend(baseMapper.selectExperienceDailyTrend());
        return metrics;
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    private static double round4(double v) {
        return Math.round(v * 10000d) / 10000d;
    }
}
