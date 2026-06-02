package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessMessageChannel;
import com.mdframe.forge.plugin.generator.mapper.BusinessMessageChannelMapper;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 业务消息通道服务。
 * <p>
 * 站内信走现有消息中心；企业微信、飞书、钉钉、Webhook 只返回 TODO 状态，不触发外部网络调用。
 */
@Service
@RequiredArgsConstructor
public class BusinessMessageChannelService {

    private static final String DEFAULT_INTERNAL_CHANNEL_CODE = "internal_websocket";
    private static final Set<String> THIRD_PARTY_TYPES = Set.of("WECHAT_WORK", "FEISHU", "DINGTALK", "WEBHOOK");

    private final BusinessMessageChannelMapper channelMapper;

    @Autowired(required = false)
    private MessageService messageService;

    public BusinessMessageChannelStatus resolveChannel(String channelCode) {
        String normalizedCode = normalizeChannelCode(channelCode);
        if (isInternalAlias(normalizedCode)) {
            return internalStatus(DEFAULT_INTERNAL_CHANNEL_CODE, "站内信");
        }

        AiBusinessMessageChannel channel = channelMapper.selectByChannelCode(resolveTenantId(), normalizedCode);
        if (channel == null) {
            if (isThirdPartyAlias(normalizedCode)) {
                return thirdPartyTodoStatus(normalizedCode.toUpperCase(Locale.ROOT), normalizedCode);
            }
            return internalStatus(DEFAULT_INTERNAL_CHANNEL_CODE, "站内信");
        }
        String channelType = StringUtils.defaultIfBlank(channel.getChannelType(), "INTERNAL")
                .toUpperCase(Locale.ROOT);
        if ("INTERNAL".equals(channelType)) {
            BusinessMessageChannelStatus status = internalStatus(channel.getChannelCode(), channel.getChannelName());
            status.setEnabled(Integer.valueOf(1).equals(channel.getStatus()));
            if (!Boolean.TRUE.equals(status.getEnabled())) {
                status.setTodo(true);
                status.setTodoCode("INTERNAL_CHANNEL_DISABLED");
                status.setMessage("站内信通道未启用");
            }
            return status;
        }
        return thirdPartyTodoStatus(channelType, channel.getChannelCode());
    }

    public SysMessage sendInternalMessage(MessageSendRequestDTO req) {
        if (messageService == null) {
            throw new BusinessException("MessageService 未注入，无法发送站内消息");
        }
        req.setChannel(StringUtils.defaultIfBlank(req.getChannel(), "WEB"));
        return messageService.send(req);
    }

    public JSONObject buildThirdPartyTodoResult(String channelType, String channelCode) {
        String type = StringUtils.defaultIfBlank(channelType, "THIRD_PARTY").toUpperCase(Locale.ROOT);
        JSONObject result = new JSONObject();
        result.put("status", "TODO");
        result.put("todoCode", type + "_NOT_IMPLEMENTED");
        result.put("channelType", type);
        result.put("channelCode", channelCode);
        result.put("message", "第三方消息通道待实现");
        return result;
    }

    public List<Long> selectUserIdsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return channelMapper.selectUserIdsByRoleIds(resolveTenantId(), roleIds);
    }

    public List<Long> selectUserIdsByOrgIds(List<Long> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return List.of();
        }
        return channelMapper.selectUserIdsByOrgIds(resolveTenantId(), orgIds);
    }

    public Set<Long> toUserIdSet(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(userIds);
    }

    private BusinessMessageChannelStatus internalStatus(String channelCode, String channelName) {
        BusinessMessageChannelStatus status = new BusinessMessageChannelStatus();
        status.setChannelCode(channelCode);
        status.setChannelName(channelName);
        status.setChannelType("INTERNAL");
        status.setSendChannel("WEB");
        status.setEnabled(true);
        status.setInternalChannel(true);
        status.setThirdPartyChannel(false);
        status.setTodo(false);
        status.setMessage("站内信通道可用");
        return status;
    }

    private BusinessMessageChannelStatus thirdPartyTodoStatus(String channelType, String channelCode) {
        BusinessMessageChannelStatus status = new BusinessMessageChannelStatus();
        status.setChannelCode(channelCode);
        status.setChannelName(channelCode);
        status.setChannelType(channelType);
        status.setSendChannel(channelType);
        status.setEnabled(false);
        status.setInternalChannel(false);
        status.setThirdPartyChannel(true);
        status.setTodo(true);
        status.setTodoCode(channelType + "_NOT_IMPLEMENTED");
        status.setMessage("第三方消息通道待实现");
        return status;
    }

    private String normalizeChannelCode(String channelCode) {
        String normalized = StringUtils.defaultIfBlank(channelCode, DEFAULT_INTERNAL_CHANNEL_CODE)
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "web", "internal" -> DEFAULT_INTERNAL_CHANNEL_CODE;
            case "wechat", "wechatwork", "wechat_work" -> "wechat_work";
            case "ding", "ding_talk", "dingtalk" -> "dingtalk";
            default -> normalized;
        };
    }

    private boolean isInternalAlias(String channelCode) {
        return DEFAULT_INTERNAL_CHANNEL_CODE.equals(channelCode);
    }

    private boolean isThirdPartyAlias(String channelCode) {
        String type = channelCode.toUpperCase(Locale.ROOT);
        return THIRD_PARTY_TYPES.contains(type)
                || Set.of("WECHAT_WORK", "FEISHU", "DINGTALK").contains(type);
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId == null ? 1L : tenantId;
    }
}
