package com.mdframe.forge.starter.message.channel;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MessageChannel {

    /**
     * 渠道唯一键，如：web、sms
     */
    ChannelType key();

    /**
     * 初始化渠道配置
     */
    void init(Map<String, String> config);

    /**
     * 发送消息
     */
    SendResult send(SendRequest request);

    class SendRequest {
        public String title;
        public String content;
        public String templateCode;
        public Map<String, Object> params;
        public java.util.Set<Long> userIds;
        public java.util.Set<Long> orgIds;
        public java.util.Set<Long> tenantIds;
        public String channel; // 指定渠道
        public String type;    // 系统消息/短信/自定义
        /** 短信业务场景，如 LOGIN_CAPTCHA / SMS_PING */
        public String scene;
        /** 指定短信通道 configId（探活等场景绕过选路） */
        public String preferredSmsConfigId;
        public Long messageId;
        private List<String> phoneList;
        private List<String> emailList;
        
        public Map<String, Object> getParams() {
            return params;
        }
        
        public void setParams(Map<String, Object> params) {
            this.params = params;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getTemplateCode() {
            return templateCode;
        }
        
        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }
        
        public Set<Long> getUserIds() {
            return userIds;
        }
        
        public void setUserIds(Set<Long> userIds) {
            this.userIds = userIds;
        }
        
        public Set<Long> getOrgIds() {
            return orgIds;
        }
        
        public void setOrgIds(Set<Long> orgIds) {
            this.orgIds = orgIds;
        }
        
        public Set<Long> getTenantIds() {
            return tenantIds;
        }
        
        public void setTenantIds(Set<Long> tenantIds) {
            this.tenantIds = tenantIds;
        }
        
        public String getChannel() {
            return channel;
        }
        
        public void setChannel(String channel) {
            this.channel = channel;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }

        public String getScene() {
            return scene;
        }

        public void setScene(String scene) {
            this.scene = scene;
        }

        public String getPreferredSmsConfigId() {
            return preferredSmsConfigId;
        }

        public void setPreferredSmsConfigId(String preferredSmsConfigId) {
            this.preferredSmsConfigId = preferredSmsConfigId;
        }
        
        public Long getMessageId() {
            return messageId;
        }
        
        public void setMessageId(Long messageId) {
            this.messageId = messageId;
        }
        
        public List<String> getPhoneList() {
            return phoneList;
        }
        
        public void setPhoneList(List<String> phoneList) {
            this.phoneList = phoneList;
        }
        
        public List<String> getEmailList() {
            return emailList;
        }
        
        public void setEmailList(List<String> emailList) {
            this.emailList = emailList;
        }
    }
    class SendResult {
        public boolean success;
        public String msg;
        public String externalId; // 第三方返回ID
        public static SendResult ok(String id){ SendResult r=new SendResult(); r.success=true; r.externalId=id; return r; }
        public static SendResult fail(String m){ SendResult r=new SendResult(); r.success=false; r.msg=m; return r; }
    }

    // ==================== 企业协同逐人投递合同（Task 12） ====================

    /**
     * 是否支持带连接上下文的逐人投递（旧渠道默认不支持，保持兼容）
     */
    default boolean supportsRecipientDelivery() {
        return false;
    }

    /**
     * 按连接上下文向明确接收人列表投递，返回逐人结果；
     * 仅 {@link #supportsRecipientDelivery()} 为 true 的渠道实现
     */
    default ChannelSendResult sendToRecipients(ChannelSendRequest request) {
        throw new UnsupportedOperationException("channel does not support recipient delivery: " + key());
    }

    /**
     * 渠道逐人投递请求
     *
     * @param tenantId       租户ID
     * @param connectionId   企业协同连接ID（非协同渠道可为空）
     * @param messageId      逻辑消息ID
     * @param idempotencyKey 本次渠道投递幂等键
     * @param recipients     Forge 接收人列表
     * @param title          标题
     * @param content        正文
     * @param params         渠道扩展参数（模板卡片字段、跳转URL等）
     */
    record ChannelSendRequest(Long tenantId, Long connectionId, Long messageId,
                              String idempotencyKey, List<ChannelRecipient> recipients,
                              String title, String content, Map<String, Object> params) {}

    /**
     * Forge 接收人（渠道内部负责映射为外部账号）
     *
     * @param userId Forge 用户ID
     * @param phone  手机号（短信类渠道使用，可为空）
     * @param email  邮箱（邮件类渠道使用，可为空）
     */
    record ChannelRecipient(Long userId, String phone, String email) {

        public static ChannelRecipient of(Long userId) {
            return new ChannelRecipient(userId, null, null);
        }
    }

    /**
     * 渠道逐人投递结果
     *
     * @param providerRequestId 供应商请求ID（用于排障，不含敏感内容）
     * @param deliveries        逐人投递结果
     * @param platform          企业协同平台编码（非协同渠道或无法解析时为空）
     */
    record ChannelSendResult(String providerRequestId, List<RecipientDeliveryResult> deliveries, String platform) {}

    /**
     * 单个接收人的投递结果
     *
     * @param userId       Forge 用户ID
     * @param status       投递状态：SENT/FAILED/SKIPPED
     * @param externalId   外部渠道逐人消息ID（可为空）
     * @param errorCode    失败错误码（成功时为空）
     * @param errorMessage 失败摘要（不含个人资料与 Secret）
     */
    record RecipientDeliveryResult(Long userId, String status, String externalId,
                                   String errorCode, String errorMessage) {

        public static final String STATUS_SENT = "SENT";
        public static final String STATUS_FAILED = "FAILED";
        public static final String STATUS_SKIPPED = "SKIPPED";

        public static RecipientDeliveryResult sent(Long userId, String externalId) {
            return new RecipientDeliveryResult(userId, STATUS_SENT, externalId, null, null);
        }

        public static RecipientDeliveryResult failed(Long userId, String errorCode, String errorMessage) {
            return new RecipientDeliveryResult(userId, STATUS_FAILED, null, errorCode, errorMessage);
        }

        public static RecipientDeliveryResult skipped(Long userId, String errorCode, String errorMessage) {
            return new RecipientDeliveryResult(userId, STATUS_SKIPPED, null, errorCode, errorMessage);
        }
    }
}
