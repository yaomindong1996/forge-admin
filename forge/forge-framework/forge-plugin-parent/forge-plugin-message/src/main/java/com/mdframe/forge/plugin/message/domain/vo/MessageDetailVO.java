package com.mdframe.forge.plugin.message.domain.vo;

import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageSendRecord;
import lombok.Data;

import java.util.List;

@Data
public class MessageDetailVO {
    
    private SysMessage message;
    
    private SysMessageSendRecord sendRecord;
    
    private List<ReceiverVO> receivers;
}