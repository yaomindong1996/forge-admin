package com.mdframe.forge.flow.controller;

import com.mdframe.forge.flow.dto.FlowGenerateRequest;
import com.mdframe.forge.flow.service.FlowBpmnGenerateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/flow/ai-generator")
@RequiredArgsConstructor
public class FlowBpmnGenerateController {

    private final FlowBpmnGenerateService generateService;

    @PostMapping(value = "/stream-generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamGenerate(@RequestBody FlowGenerateRequest request) {
        return generateService.streamGenerate(request);
    }
}
