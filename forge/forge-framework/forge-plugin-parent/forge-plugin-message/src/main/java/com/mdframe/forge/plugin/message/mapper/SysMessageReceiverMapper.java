package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.domain.vo.MessageVO;
import com.mdframe.forge.plugin.message.domain.vo.ReceiverVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMessageReceiverMapper extends BaseMapper<SysMessageReceiver> {

    
    List<ReceiverVO> selectReceiversWithUserInfo(@Param("messageId") Long messageId);
    
   
    IPage<MessageVO> selectUserWebMessages(Page<MessageVO> page,
                                            @Param("userId") Long userId,
                                            @Param("readFlag") Integer readFlag,
                                            @Param("type") String type,
                                            @Param("keyword") String keyword);
}
