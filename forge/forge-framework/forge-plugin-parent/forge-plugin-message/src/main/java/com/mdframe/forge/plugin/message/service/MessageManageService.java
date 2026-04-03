package com.mdframe.forge.plugin.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.message.domain.dto.MessageManageQueryDTO;
import com.mdframe.forge.plugin.message.domain.vo.MessageDetailVO;
import com.mdframe.forge.plugin.message.domain.vo.MessageManageVO;

public interface MessageManageService {
    
    IPage<MessageManageVO> pageMessages(MessageManageQueryDTO query, Integer pageNum, Integer pageSize);
    
    MessageDetailVO getMessageDetail(Long messageId);
}