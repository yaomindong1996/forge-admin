package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai/crud/{configKey}")
@RequiredArgsConstructor
public class DynamicCrudController {

    private final DynamicCrudService dynamicCrudService;

    @GetMapping("/page")
    public RespInfo<Page<Map<String, Object>>> page(@PathVariable String configKey,
                                                     PageQuery pageQuery,
                                                     DynamicCrudQuery query) {
        return RespInfo.success(dynamicCrudService.selectPage(configKey, pageQuery, query));
    }

    @GetMapping("/{id}")
    public RespInfo<Map<String, Object>> getById(@PathVariable String configKey,
                                                  @PathVariable Long id) {
        return RespInfo.success(dynamicCrudService.selectById(configKey, id));
    }

    @PostMapping
    public RespInfo<Void> create(@PathVariable String configKey,
                                  @RequestBody Map<String, Object> data) {
        dynamicCrudService.insert(configKey, data);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> update(@PathVariable String configKey,
                                  @RequestBody Map<String, Object> data) {
        dynamicCrudService.updateById(configKey, data);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable String configKey,
                                  @PathVariable Long id) {
        dynamicCrudService.deleteById(configKey, id);
        return RespInfo.success();
    }
}
