package com.mdframe.forge.starter.message.channel;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.starter.message.config.SmsConfigProvider;
import com.mdframe.forge.starter.message.config.SmsConfigProvider.SmsConfig;
import com.mdframe.forge.starter.message.config.SmsConfigProvider.SmsRoutePolicy;
import com.mdframe.forge.starter.message.sms.SmsRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class SmsMessageChannel implements MessageChannel {

    private final SmsRouter smsRouter;
    private final SmsConfigProvider smsConfigProvider;

    @Override
    public ChannelType key() {
        return ChannelType.SMS;
    }

    @Override
    public void init(Map<String, String> configMap) {
    }

    @Override
    public SendResult send(SendRequest request) {
        List<String> phones = request.getPhoneList();
        if (phones == null || phones.isEmpty()) {
            return SendResult.fail("未找到有效的手机号码");
        }

        List<SmsConfig> attemptOrder;
        if (StrUtil.isNotBlank(request.getPreferredSmsConfigId())) {
            attemptOrder = smsConfigProvider.listEnabledSmsConfigs().stream()
                    .filter(item -> request.getPreferredSmsConfigId().equals(item.getConfigId()))
                    .toList();
            if (attemptOrder.isEmpty()) {
                return SendResult.fail("指定短信通道不可用: " + request.getPreferredSmsConfigId());
            }
        } else {
            attemptOrder = smsRouter.resolveAttemptOrder(request.getScene(), null);
        }
        if (attemptOrder.isEmpty()) {
            return SendResult.fail("短信通道未配置或未启用");
        }

        SmsRoutePolicy policy = smsConfigProvider.getRoutePolicy();
        int maxAttempts = smsRouter.maxAttempts(policy, attemptOrder.size());
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < Math.min(maxAttempts, attemptOrder.size()); i++) {
            SmsConfig config = attemptOrder.get(i);
            SendResult result = sendWithConfig(request, phones, config);
            if (result.success) {
                result.externalId = (result.externalId == null ? "SMS" : result.externalId)
                        + "@" + config.getConfigId();
                return result;
            }
            errors.add(config.getConfigId() + ": " + result.msg);
            log.warn("短信通道发送失败，尝试 failover: configId={}, supplier={}, err={}",
                    config.getConfigId(), config.getSupplier(), result.msg);
        }
        return SendResult.fail("短信发送失败: " + String.join(" | ", errors));
    }

    private SendResult sendWithConfig(SendRequest request, List<String> phones, SmsConfig config) {
        SmsBlend smsBlend = resolveBlend(config.getConfigId());
        if (smsBlend == null) {
            return SendResult.fail("短信通道未初始化: " + config.getConfigId());
        }

        try {
            String templateCode = smsRouter.resolveTemplateId(config, request.getScene(), request.templateCode);
            LinkedHashMap<String, String> params = convertParams(request.params);
            SmsResponse response;
            if (phones.size() == 1) {
                if (StrUtil.isBlank(templateCode)) {
                    response = smsBlend.sendMessage(phones.get(0), request.content);
                } else {
                    response = smsBlend.sendMessage(phones.get(0), templateCode, params);
                }
            } else if (StrUtil.isBlank(templateCode)) {
                response = smsBlend.massTexting(phones, request.content);
            } else {
                response = smsBlend.massTexting(phones, templateCode, params);
            }

            String resp = JSONObject.toJSONString(response);
            log.info("发送短信返回结果: configId={}, resp={}", config.getConfigId(), resp);
            if (response != null && response.isSuccess()) {
                return SendResult.ok("SMS-" + System.currentTimeMillis());
            }
            return SendResult.fail(resp);
        } catch (Exception e) {
            log.error("短信发送异常: configId={}, err={}", config.getConfigId(), e.getMessage(), e);
            return SendResult.fail("短信发送异常: " + e.getMessage());
        }
    }

    private SmsBlend resolveBlend(String configId) {
        if (StrUtil.isNotBlank(configId)) {
            SmsBlend blend = SmsFactory.getSmsBlend(configId);
            if (blend != null) {
                return blend;
            }
        }
        return SmsFactory.getSmsBlend();
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
