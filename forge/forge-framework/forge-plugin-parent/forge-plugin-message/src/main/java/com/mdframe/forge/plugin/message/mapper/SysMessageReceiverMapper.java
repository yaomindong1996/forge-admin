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
    
    @Select("SELECT r.user_id, u.real_name as user_name, o.org_name, r.read_flag, r.read_time " +
            "FROM sys_message_receiver r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "LEFT JOIN sys_user_org so on so.user_id = u.id and so.is_main = '1'" +
            "LEFT JOIN sys_org o ON so.org_id = o.id " +
            "WHERE r.message_id = #{messageId}")
    List<ReceiverVO> selectReceiversWithUserInfo(@Param("messageId") Long messageId);
    
    @Select("<script>" +
            "SELECT m.id, m.title, m.content, m.type, m.send_channel, r.read_flag, r.read_time, m.create_time, m.biz_type, m.biz_key " +
            "FROM sys_message_receiver r " +
            "INNER JOIN sys_message m ON r.message_id = m.id " +
            "WHERE r.user_id = #{userId} " +
            "AND m.send_channel = 'WEB' " +
            "<if test='readFlag != null'> AND r.read_flag = #{readFlag} </if>" +
            "<if test='type != null and type != \"\"'> AND m.type = #{type} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (m.title LIKE CONCAT('%', #{keyword}, '%') OR m.content LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY r.create_time DESC " +
            "</script>")
    IPage<MessageVO> selectUserWebMessages(Page<MessageVO> page, 
                                            @Param("userId") Long userId, 
                                            @Param("readFlag") Integer readFlag,
                                            @Param("type") String type,
                                            @Param("keyword") String keyword);
}
