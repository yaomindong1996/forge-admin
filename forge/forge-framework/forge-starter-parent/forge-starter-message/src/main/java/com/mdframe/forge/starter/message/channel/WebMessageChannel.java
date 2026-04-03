package com.mdframe.forge.starter.message.channel;

import java.util.Map;

public class WebMessageChannel implements MessageChannel {
    @Override
    public ChannelType key() {
        return ChannelType.WEB;
    }
    @Override
    public void init(Map<String, String> config) { /* no-op for now */ }
    @Override
    public SendResult send(SendRequest request) {
        // 这里实现系统通知入库/推送（由插件模块完成），starter仅返回成功占位
        return SendResult.ok("WEB-" + System.currentTimeMillis());
    }
}
