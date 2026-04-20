package com.mdframe.forge.plugin.ai.client;

import com.mdframe.forge.plugin.ai.client.dto.AiClientRequest;
import com.mdframe.forge.plugin.ai.client.dto.AiClientResponse;
import reactor.core.publisher.Flux;

public interface AiClient {

    AiClientResponse call(AiClientRequest request);

    Flux<String> stream(AiClientRequest request);
}
