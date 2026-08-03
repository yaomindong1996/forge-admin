package com.mdframe.forge.plugin.capability.opengateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开放网关统一响应（对外契约，不走 RespInfo）：
 * {@code {"code":"SUCCESS|错误码","message":"...","requestId":"...","timestamp":epochMillis,"data":{}}}。
 * httpStatus 仅供入口层设置响应状态码，不参与 JSON 序列化与幂等快照。
 */
public record OpenGatewayResponse(
        String code,
        String message,
        String requestId,
        long timestamp,
        Map<String, Object> data,
        @JsonIgnore Integer httpStatus) {

    public static final String CODE_SUCCESS = "SUCCESS";

    public static OpenGatewayResponse success(Map<String, Object> data, String requestId) {
        return new OpenGatewayResponse(CODE_SUCCESS, "成功", requestId,
                System.currentTimeMillis(), data == null ? Map.of() : data, 200);
    }

    public static OpenGatewayResponse error(
            String errorCode, String message, String requestId, int httpStatus) {
        return error(errorCode, message, requestId, httpStatus, Map.of());
    }

    public static OpenGatewayResponse error(
            String errorCode,
            String message,
            String requestId,
            int httpStatus,
            Map<String, Object> data) {
        return new OpenGatewayResponse(errorCode, message, requestId,
                System.currentTimeMillis(), data == null ? Map.of() : data, httpStatus);
    }

    /**
     * 基于历史快照重建幂等命中响应：沿用首次执行的业务数据，使用本次请求的 requestId/时间戳。
     */
    public OpenGatewayResponse asIdempotentHit(String currentRequestId) {
        Map<String, Object> hitData = new LinkedHashMap<>(data == null ? Map.of() : data);
        hitData.put("idempotentHit", true);
        return new OpenGatewayResponse(code, message, currentRequestId,
                System.currentTimeMillis(), hitData, 200);
    }

    public int status() {
        return httpStatus == null ? 200 : httpStatus;
    }
}
