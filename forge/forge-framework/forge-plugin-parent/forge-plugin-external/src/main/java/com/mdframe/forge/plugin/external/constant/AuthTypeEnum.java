package com.mdframe.forge.plugin.external.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthTypeEnum {

    NONE("none", "无认证"),
    
    BASIC("basic", "Basic认证"),
    
    TOKEN("token", "Token认证"),
    
    OAUTH2("oauth2", "OAuth2认证"),
    
    API_KEY("api_key", "API Key认证"),
    
    CUSTOM("custom", "自定义认证");

    private final String code;
    
    private final String desc;

    public static AuthTypeEnum fromCode(String code) {
        for (AuthTypeEnum authType : values()) {
            if (authType.getCode().equals(code)) {
                return authType;
            }
        }
        return NONE;
    }
}