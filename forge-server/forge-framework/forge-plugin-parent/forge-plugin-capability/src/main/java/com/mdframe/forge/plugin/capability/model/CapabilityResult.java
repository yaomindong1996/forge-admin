package com.mdframe.forge.plugin.capability.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public record CapabilityResult(
        String requestId,
        String capabilityCode,
        CapabilityResultStatus status,
        JsonNode data,
        String errorCode,
        String message,
        long durationMs) {

    public CapabilityResult {
        requestId = requireText(requestId, "requestId");
        capabilityCode = requireText(capabilityCode, "capabilityCode");
        status = Objects.requireNonNull(status, "status 不能为空");
        data = data == null ? null : data.deepCopy();
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs 不能小于 0");
        }
        if (status == CapabilityResultStatus.SUCCESS && errorCode != null) {
            throw new IllegalArgumentException("成功结果不能包含 errorCode");
        }
        if (status == CapabilityResultStatus.ERROR && (errorCode == null || errorCode.isBlank())) {
            throw new IllegalArgumentException("失败结果必须包含 errorCode");
        }
    }

    @Override
    public JsonNode data() {
        return data == null ? null : data.deepCopy();
    }

    public static CapabilityResult success(
            String requestId, String capabilityCode, JsonNode data, long durationMs) {
        return new CapabilityResult(requestId, capabilityCode, CapabilityResultStatus.SUCCESS,
                data, null, null, durationMs);
    }

    public static CapabilityResult error(
            String requestId,
            String capabilityCode,
            CapabilityErrorCode errorCode,
            String message,
            long durationMs) {
        return new CapabilityResult(requestId, capabilityCode, CapabilityResultStatus.ERROR,
                null, errorCode.name(), message, durationMs);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.trim();
    }
}
