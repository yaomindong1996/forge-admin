package com.mdframe.forge.starter.message.channel;

import java.util.Map;

public class SmsMessageChannel implements MessageChannel {
    @Override
    public String key() { return "sms"; }
    @Override
    public void init(Map<String, String> config) { /* TODO integrate SMS provider via SPI */ }
    @Override
    public SendResult send(SendRequest request) {
        // 这里可对接第三方短信网关（如阿里、华为），此处先占位
        return SendResult.ok("SMS-" + System.currentTimeMillis());
    }
}
