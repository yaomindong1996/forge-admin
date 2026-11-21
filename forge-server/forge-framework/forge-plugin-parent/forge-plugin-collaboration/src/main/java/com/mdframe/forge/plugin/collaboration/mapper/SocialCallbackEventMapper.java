package com.mdframe.forge.plugin.collaboration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.collaboration.domain.entity.SocialCallbackEvent;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回调事件收件箱 Mapper
 * <p>
 * 收件箱为运行时表，claim/标记均通过 CAS 条件更新保证并发安全。
 */
public interface SocialCallbackEventMapper extends BaseMapper<SocialCallbackEvent> {

    /**
     * 查询待领取候选 id（PENDING 或到期重试的 FAILED），供两段式 claim 使用。
     */
    List<Long> selectClaimCandidateIds(@Param("tenantId") Long tenantId,
                                       @Param("batchSize") int batchSize);

    /**
     * CAS 领取指定 id：仍为可领取状态时置为 PROCESSING 并绑定 Worker。
     */
    int claimEventsByIds(@Param("tenantId") Long tenantId,
                         @Param("workerId") String workerId,
                         @Param("ids") List<Long> ids);

    /**
     * 查询指定 Worker 当前已领取的事件
     */
    List<SocialCallbackEvent> selectClaimedEvents(@Param("tenantId") Long tenantId,
                                                  @Param("workerId") String workerId);

    /**
     * CAS 标记事件处理成功
     */
    int markProcessed(@Param("id") Long id,
                      @Param("tenantId") Long tenantId,
                      @Param("workerId") String workerId);

    /**
     * CAS 标记事件处理失败；退避时间由调用方计算后传入，避免 Mapper 方言函数。
     */
    int markFailed(@Param("id") Long id,
                   @Param("tenantId") Long tenantId,
                   @Param("workerId") String workerId,
                   @Param("processStatus") String processStatus,
                   @Param("nextRetryTime") LocalDateTime nextRetryTime,
                   @Param("errorCode") String errorCode,
                   @Param("errorSummary") String errorSummary);

    /**
     * 回调事件元数据分页（不返回解密正文密文）
     */
    Page<SocialCallbackEvent> selectEventPage(Page<SocialCallbackEvent> page,
                                              @Param("tenantId") Long tenantId,
                                              @Param("connectionId") Long connectionId,
                                              @Param("eventType") String eventType,
                                              @Param("processStatus") String processStatus);
}
