package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.domain.vo.MessageVO;
import com.mdframe.forge.plugin.message.domain.vo.ReceiverStatVO;
import com.mdframe.forge.plugin.message.domain.vo.ReceiverVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SysMessageReceiverMapper extends BaseMapper<SysMessageReceiver> {

    List<ReceiverStatVO> selectReceiverStatsByMessageIds(@Param("messageIds") List<Long> messageIds);

    List<ReceiverVO> selectReceiversWithUserInfo(@Param("messageId") Long messageId);
    
   
    IPage<MessageVO> selectUserWebMessages(Page<MessageVO> page,
                                            @Param("userId") Long userId,
                                            @Param("readFlag") Integer readFlag,
                                            @Param("type") String type,
                                            @Param("keyword") String keyword,
                                            @Param("startTime") String startTime,
                                            @Param("endTime") String endTime);

    List<String> selectUnreadWebMessageTypes(@Param("userId") Long userId);

    int markWebMessagesReadByBiz(@Param("bizType") String bizType,
                                 @Param("bizKey") String bizKey,
                                 @Param("userId") Long userId,
                                 @Param("readTime") LocalDateTime readTime);

    int markMessagesReadBatch(@Param("tenantId") Long tenantId,
                              @Param("userId") Long userId,
                              @Param("messageIds") List<Long> messageIds,
                              @Param("readTime") LocalDateTime readTime);

    int markAllMessagesRead(@Param("tenantId") Long tenantId,
                            @Param("userId") Long userId,
                            @Param("readTime") LocalDateTime readTime);

    int updateDeliveryResult(@Param("messageId") Long messageId,
                             @Param("userId") Long userId,
                             @Param("deliveryStatus") String deliveryStatus,
                             @Param("externalId") String externalId,
                             @Param("lastErrorCode") String lastErrorCode,
                             @Param("lastAttemptTime") LocalDateTime lastAttemptTime,
                             @Param("nextRetryTime") LocalDateTime nextRetryTime);

    List<SysMessageReceiver> selectFailedDeliveriesDue(@Param("tenantId") Long tenantId,
                                                       @Param("now") LocalDateTime now,
                                                       @Param("limitSize") int limitSize);
}
