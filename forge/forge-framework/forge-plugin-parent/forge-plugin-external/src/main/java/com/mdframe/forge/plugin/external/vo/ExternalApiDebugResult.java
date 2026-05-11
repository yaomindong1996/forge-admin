package com.mdframe.forge.plugin.external.vo;

import lombok.Data;

@Data
public class ExternalApiDebugResult {

    private Boolean success;

    private Integer httpStatusCode;

    private Long durationMs;

    private Object responseData;

    private String responseBody;

    private String errorMessage;
}
