package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowCc;

import java.util.List;

/**
 * 流程抄送服务接口
 */
public interface FlowCcService extends IService<FlowCc> {

    /**
     * 发送抄送
     *
     * @param processInstanceId 流程实例ID
     * @param taskId           任务ID
     * @param title            标题
     * @param content          内容摘要
     * @param businessKey      业务Key
     * @param ccUserIds        抄送人ID列表
     * @param ccUserNames      抄送人姓名列表
     * @param sendUserId       发送人ID
     * @param sendUserName     发送人姓名
     */
    void sendCc(String processInstanceId, String processDefKey, String taskId,
                String title, String content, String businessKey,
                List<String> ccUserIds, List<String> ccUserNames,
                String sendUserId, String sendUserName);

    /**
     * 我的抄送（抄送给我的）
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @param isRead 是否已读（可选）
     * @return 抄送列表
     */
    IPage<FlowCc> myCc(Page<FlowCc> page, String userId, Integer isRead);

    /**
     * 我发送的抄送
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 抄送列表
     */
    IPage<FlowCc> sentCc(Page<FlowCc> page, String userId);

    /**
     * 标记已读
     *
     * @param id 抄送ID
     */
    void markRead(String id);

    /**
     * 批量标记已读
     *
     * @param ids 抄送ID列表
     */
    void batchMarkRead(List<String> ids);

    /**
     * 获取未读数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    long countUnread(String userId);
}