package com.mdframe.forge.plugin.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.message.domain.dto.MessageQueryDTO;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.vo.MessageVO;
import com.mdframe.forge.plugin.message.domain.vo.UnreadCountVO;

/**
 * 消息服务接口
 */
public interface MessageService extends IService<SysMessage> {
    
    /**
     * 发送消息
     */
    SysMessage send(MessageSendRequestDTO req);

    /**
     * 按业务类型和业务键幂等发送消息。
     */
    SysMessage sendIfAbsent(MessageSendRequestDTO req, String bizType, String bizKey);
    
    /**
     * 标记为已读
     */
    void markRead(Long messageId, Long userId);
    
    /**
     * 批量标记为已读
     */
    void markReadBatch(java.util.List<Long> messageIds, Long userId);
    
    /**
     * 全部标记为已读
     */
    void markAllRead(Long userId);

    /**
     * 按业务类型和业务键将站内信标记为已读（全部收件人，供流程办结等系统场景）。
     *
     * @return 更新的接收人记录数
     */
    int markWebReadByBiz(String bizType, String bizKey);

    /**
     * 按业务类型、业务键和用户将站内信标记为已读。
     * {@code userId} 为 null 时等同于 {@link #markWebReadByBiz(String, String)}。
     *
     * @return 更新的接收人记录数
     */
    int markWebReadByBiz(String bizType, String bizKey, Long userId);
    
    /**
     * 分页查询用户消息
     */
    IPage<MessageVO> pageUserMessages(Long userId, MessageQueryDTO query, Integer pageNum, Integer pageSize);
    
    /**
     * 查询未读消息统计
     */
    UnreadCountVO getUnreadCount(Long userId);
    
    /**
     * 查询消息详情
     */
    MessageVO getMessageDetail(Long messageId, Long userId);

    /**
     * 按候选顺序解析首个「存在且启用」的模板编码，用于平台差异化模板回退。
     * <p>例如依次传入 {@code FLOW_TODO_CARD_WECOM}、{@code FLOW_TODO_CARD}，
     * 返回第一个命中的编码；全部不存在或未启用时返回 {@code null}。</p>
     *
     * @param candidateCodes 候选模板编码（按优先级从高到低）
     * @return 首个启用的模板编码；无匹配返回 null
     */
    String resolveEnabledTemplateCode(String... candidateCodes);
}
