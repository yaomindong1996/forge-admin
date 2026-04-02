package com.mdframe.forge.starter.message.channel;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.starter.message.config.ReadConfig;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SmsMessageChannel implements MessageChannel {

    @Override
    public ChannelType key() {
        return ChannelType.SMS;
    }

    @Override
    public void init(Map<String, String> configMap) {
    }

    @Override
    public SendResult send(SendRequest request) {
        SmsBlend smsBlend = SmsFactory.getSmsBlend();
        if (smsBlend == null) {
            return SendResult.fail("短信通道未初始化");
        }

        try {
            List<String> phones = request.getPhoneList();
            if (phones == null || phones.isEmpty()) {
                return SendResult.fail("未找到有效的手机号码");
            }
            
            LinkedHashMap<String, String> params = convertParams(request.params);
            SmsResponse response;
            if (phones.size() == 1) {
                if (StrUtil.isBlank(request.templateCode)) {
                    response = smsBlend.sendMessage(phones.get(0),request.content);
                } else {
                    response = smsBlend.sendMessage(phones.get(0), request.templateCode, params);
                }
                String resp = JSONObject.toJSONString(response);
                log.info("发送短信返回结果:{}", resp);
                if (response.isSuccess()) {
                    return SendResult.ok("SMS-" + System.currentTimeMillis());
                } else {
                    return SendResult.fail(resp);
                }
            } else {
                if (StrUtil.isBlank(request.templateCode)) {
                    response = smsBlend.massTexting(phones,request.content);
                } else {
                    response = smsBlend.massTexting(phones, request.templateCode, params);
                }
                if (response.isSuccess()) {
                    return SendResult.ok("SMS-" + System.currentTimeMillis());
                } else {
                    return SendResult.fail("发送短信失败");
                }
            }
        } catch (Exception e) {
            log.error("短信发送失败: {}", e.getMessage(), e);
            return SendResult.fail("短信发送异常: " + e.getMessage());
        }
    }

    

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, String> convertParams(Map<String, Object> params) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (params != null) {
            params.forEach((k, v) -> {
                if (v != null) {
                    result.put(k, String.valueOf(v));
                }
            });
        }
        return result;
    }
}
