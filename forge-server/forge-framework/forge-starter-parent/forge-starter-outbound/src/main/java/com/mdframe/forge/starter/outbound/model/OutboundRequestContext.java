package com.mdframe.forge.starter.outbound.model;

import lombok.Getter;

@Getter
public class OutboundRequestContext {

    private final String scene;
    private final String url;

    public OutboundRequestContext(String scene, String url) {
        this.scene = scene;
        this.url = url;
    }
}
