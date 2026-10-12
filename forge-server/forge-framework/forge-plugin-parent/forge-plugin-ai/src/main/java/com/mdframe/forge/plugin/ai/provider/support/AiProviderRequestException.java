package com.mdframe.forge.plugin.ai.provider.support;

import com.mdframe.forge.starter.core.exception.BusinessException;

/** 原生供应商异常只携带脱敏的状态/错误码，不保留响应正文、凭据或原始异常链。 */
public final class AiProviderRequestException extends BusinessException {

    private final Integer httpStatus;
    private final String errorCode;

    public AiProviderRequestException(String message, Integer httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = new AiProviderFailureDiagnostics(httpStatus, errorCode).errorCode();
    }

    public Integer httpStatus() {
        return httpStatus;
    }

    public String errorCode() {
        return errorCode;
    }
}
