package com.mdframe.forge.starter.message.channel;

import java.util.Map;

public interface MessageChannel {

    /**
     * 渠道唯一键，如：web、sms
     */
    String key();

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
    }
    class SendResult {
        public boolean success;
        public String msg;
        public String externalId; // 第三方返回ID
        public static SendResult ok(String id){ SendResult r=new SendResult(); r.success=true; r.externalId=id; return r; }
        public static SendResult fail(String m){ SendResult r=new SendResult(); r.success=false; r.msg=m; return r; }
    }
}
