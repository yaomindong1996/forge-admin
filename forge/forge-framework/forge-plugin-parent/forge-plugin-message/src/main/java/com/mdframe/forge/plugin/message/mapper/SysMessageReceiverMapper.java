package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.domain.vo.ReceiverVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMessageReceiverMapper extends BaseMapper<SysMessageReceiver> {
    
    @Select("SELECT r.user_id, u.real_name as user_name, o.org_name, r.read_flag, r.read_time " +
            "FROM sys_message_receiver r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "LEFT JOIN sys_org o ON u.org_id = o.id " +
            "WHERE r.message_id = #{messageId}")
    List<ReceiverVO> selectReceiversWithUserInfo(@Param("messageId") Long messageId);
}
