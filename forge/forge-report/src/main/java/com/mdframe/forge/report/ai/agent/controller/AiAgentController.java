package com.mdframe.forge.report.ai.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.report.ai.agent.domain.AiAgent;
import com.mdframe.forge.report.ai.agent.service.AiAgentService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/goview/ai/agent")
@RequiredArgsConstructor
public class AiAgentController {

    private final AiAgentService agentService;

    @GetMapping("/page")
    public RespInfo<Page<AiAgent>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(agentService.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AiAgent>().orderByDesc(AiAgent::getCreateTime)));
    }

    @GetMapping("/list")
    public RespInfo<List<AiAgent>> list() {
        return RespInfo.success(agentService.list(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getStatus, "0")));
    }

    @GetMapping("/{id}")
    public RespInfo<AiAgent> getById(@PathVariable Long id) {
        return RespInfo.success(agentService.getById(id));
    }

    @PostMapping
    public RespInfo<Void> create(@RequestBody AiAgent agent) {
        agentService.save(agent);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> update(@RequestBody AiAgent agent) {
        agentService.updateById(agent);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        agentService.removeById(id);
        return RespInfo.success();
    }
}
