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
     * 人工发送抄送。与流程回调发送分开，必须校验当前会话身份和流程参与关系。
     */
    void sendCcByCurrentUser(String processInstanceId, String processDefKey, String taskId,
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
    IPage<FlowCc> myCc(Page<FlowCc> page, String userId, Integer isRead, String title);

    /**
     * 我发送的抄送
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 抄送列表
     */
    IPage<FlowCc> sentCc(Page<FlowCc> page, String userId, String title);

    /**
     * 标记已读
     *
     * @param id 抄送ID
     */
    void markRead(String id);

    FlowCc getVisibleById(String id, String userId);

    /**
     * 批量标记已读
     *
     * @param ids 抄送ID列表
     */
    void batchMarkRead(List<String> ids);

    /** 将当前接收人的全部有效未读抄送标记为已读。 */
    int markAllRead();

    /** 发送人撤回一条仍有效的临时抄送关系。 */
    void revoke(String id, String reason);

    /**
     * 获取未读数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    long countUnread(String userId);
}
