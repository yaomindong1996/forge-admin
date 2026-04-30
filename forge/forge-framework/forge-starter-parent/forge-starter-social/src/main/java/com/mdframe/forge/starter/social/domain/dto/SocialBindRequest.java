package com.mdframe.forge.starter.social.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SocialBindRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String platform;

    private String code;

    private String state;

    private Long tenantId;
}
