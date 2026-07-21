package com.mdframe.forge.starter.outbound.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder(toBuilder = true)
public class OutboundRequest {

    private final String scene;
    private final String url;

    @Builder.Default
    private final String method = "POST";

    @Builder.Default
    private final Map<String, String> headers = Map.of();

    private final String contentType;

    @Builder.Default
    private final byte[] body = new byte[0];
}
