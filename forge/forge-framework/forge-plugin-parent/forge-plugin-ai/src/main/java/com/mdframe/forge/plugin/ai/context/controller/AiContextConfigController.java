package com.mdframe.forge.plugin.ai.context.controller;

import com.mdframe.forge.plugin.ai.context.domain.AiContextConfig;
import com.mdframe.forge.plugin.ai.context.service.AiContextConfigService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai/context")
@RequiredArgsConstructor
public class AiContextConfigController {

    private final AiContextConfigService contextConfigService;

    @GetMapping("/list")
    public RespInfo<List<AiContextConfig>> list(@RequestParam String agentCode) {
        return RespInfo.success(contextConfigService.listByAgentCode(agentCode));
    }

    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody AiContextConfig config) {
        contextConfigService.save(config);
        return RespInfo.success();
    }

    @PutMapping("/update")
    public RespInfo<Void> update(@RequestBody AiContextConfig config) {
        contextConfigService.updateById(config);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        contextConfigService.removeById(id);
        return RespInfo.success();
    }
}
