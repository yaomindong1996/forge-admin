package com.mdframe.forge.report.ai.provider.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.report.ai.provider.domain.AiProvider;
import com.mdframe.forge.report.ai.provider.service.AiProviderService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/goview/ai/provider")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderService providerService;

    @GetMapping("/page")
    public RespInfo<Page<AiProvider>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(providerService.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AiProvider>().orderByDesc(AiProvider::getCreateTime)));
    }

    @GetMapping("/{id}")
    public RespInfo<AiProvider> getById(@PathVariable Long id) {
        return RespInfo.success(providerService.getById(id));
    }

    @PostMapping
    public RespInfo<Void> create(@RequestBody AiProvider provider) {
        providerService.save(provider);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> update(@RequestBody AiProvider provider) {
        providerService.updateById(provider);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        providerService.removeById(id);
        return RespInfo.success();
    }
}
