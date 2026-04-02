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
}
