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
}
