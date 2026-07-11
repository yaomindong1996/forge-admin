package com.mdframe.forge.plugin.ai.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/agent")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class AiAgentController {

    private final AiAgentService agentService;

    @GetMapping("/page")
    public RespInfo<Page<AiAgent>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<AiAgent> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String searchKeyword = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(AiAgent::getAgentName, searchKeyword)
                    .or()
                    .like(AiAgent::getAgentCode, searchKeyword)
                    .or()
                    .like(AiAgent::getDescription, searchKeyword));
        }
        if (StringUtils.hasText(status) && !"all".equals(status)) {
            queryWrapper.eq(AiAgent::getStatus, status);
        }
        queryWrapper.orderByDesc(AiAgent::getCreateTime);
        return RespInfo.success(agentService.page(new Page<>(pageNum, pageSize), queryWrapper));
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
        agentService.createAgent(agent);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> update(@RequestBody AiAgent agent) {
        agentService.updateAgent(agent);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        agentService.removeById(id);
        return RespInfo.success();
    }
}
