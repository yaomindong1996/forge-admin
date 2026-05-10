package com.mdframe.forge.plugin.external.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdapterTypeEnum {

    HTTP_CLIENT("http_client", "HttpClient适配器"),
    
    REST_TEMPLATE("rest_template", "RestTemplate适配器"),
    
    OK_HTTP("ok_http", "OkHttp适配器"),
    
    CUSTOM("custom", "自定义适配器");

    private final String code;
    
    private final String desc;

    public static AdapterTypeEnum fromCode(String code) {
        for (AdapterTypeEnum adapterType : values()) {
            if (adapterType.getCode().equals(code)) {
                return adapterType;
            }
        }
        return HTTP_CLIENT;
    }
}