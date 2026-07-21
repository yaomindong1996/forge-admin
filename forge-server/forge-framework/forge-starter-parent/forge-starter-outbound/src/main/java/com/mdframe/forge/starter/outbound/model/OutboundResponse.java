package com.mdframe.forge.starter.outbound.model;

import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Getter
public class OutboundResponse {

    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final byte[] body;

    public OutboundResponse(int statusCode, Map<String, List<String>> headers, byte[] body) {
        this.statusCode = statusCode;
        this.headers = Map.copyOf(headers);
        this.body = body.clone();
    }

    public String bodyAsUtf8() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public String firstHeader(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .filter(values -> !values.isEmpty())
                .map(values -> values.get(0))
                .findFirst()
                .orElse(null);
    }
}
