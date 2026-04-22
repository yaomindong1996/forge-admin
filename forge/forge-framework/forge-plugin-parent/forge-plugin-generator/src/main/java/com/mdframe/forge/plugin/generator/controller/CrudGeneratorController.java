package com.mdframe.forge.plugin.generator.controller;

import com.mdframe.forge.plugin.generator.dto.StreamGenerateRequest;
import com.mdframe.forge.plugin.generator.service.CrudGeneratorStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/ai/crud-generator")
@RequiredArgsConstructor
public class CrudGeneratorController {

    private final CrudGeneratorStreamService streamService;

    @PostMapping(value = "/stream-generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamGenerate(@RequestBody StreamGenerateRequest request) {
        return streamService.streamGenerate(request);
    }
}