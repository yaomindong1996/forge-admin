package com.mdframe.forge.plugin.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 消息接收对象解析服务（SPI扩展点）
 * 用于根据组织/租户等维度筛选用户ID
 */
public interface MessageReceiverResolver {
    
    /**
     * 根据发送范围批量处理用户ID（推荐使用，避免内存溢出）
     *
     * @param sendScope 发送范围（ALL-全员/ORG-指定组织/USERS-指定人员）
     * @param userIds 指定的用户ID列表（当sendScope=USERS时使用）
     * @param orgIds 指定的组织ID列表（当sendScope=ORG时使用）
     * @param tenantIds 指定的租户ID列表
     * @param batchConsumer 批量处理回调函数，每批次userIds会调用一次
     * @param batchSize 每批次大小
     */
    void processUsersByScope(String sendScope,
                            Set<Long> userIds,
                            Set<Long> orgIds,
                            Set<Long> tenantIds,
                            Consumer<List<Long>> batchConsumer,
                            int batchSize);
}
