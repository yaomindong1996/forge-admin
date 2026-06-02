package com.mdframe.forge.plugin.generator.service.businessapp;

import lombok.Data;

/**
 * 业务消息通道解析结果。
 */
@Data
public class BusinessMessageChannelStatus {

    private String channelCode;

    private String channelName;

    private String channelType;

    private String sendChannel;

    private Boolean enabled;

    private Boolean internalChannel;

    private Boolean thirdPartyChannel;

    private Boolean todo;

    private String todoCode;

    private String message;
}
