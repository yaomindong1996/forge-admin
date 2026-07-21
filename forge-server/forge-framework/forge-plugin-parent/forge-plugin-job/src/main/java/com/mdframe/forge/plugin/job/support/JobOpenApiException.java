package com.mdframe.forge.plugin.job.support;

import lombok.Getter;

@Getter
public class JobOpenApiException extends RuntimeException {

    private final int status;
    private final String errorCode;

    public JobOpenApiException(int status, String errorCode) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static JobOpenApiException badRequest(String errorCode) {
        return new JobOpenApiException(400, errorCode);
    }

    public static JobOpenApiException unauthorized() {
        return new JobOpenApiException(401, "invalid_token");
    }

    public static JobOpenApiException forbidden(String errorCode) {
        return new JobOpenApiException(403, errorCode);
    }

    public static JobOpenApiException notFound(String errorCode) {
        return new JobOpenApiException(404, errorCode);
    }

    public static JobOpenApiException conflict(String errorCode) {
        return new JobOpenApiException(409, errorCode);
    }

    public static JobOpenApiException tooManyRequests() {
        return new JobOpenApiException(429, "rate_limit_exceeded");
    }

    public static JobOpenApiException unavailable() {
        return new JobOpenApiException(503, "service_unavailable");
    }
}
