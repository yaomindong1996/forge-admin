package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessStatsMetricQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessStatsService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStatsMetricVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务对象统计控制器。
 * <p>
 * 提供数据概览、分组统计、时间趋势接口，供前端报表引擎/仪表盘使用。
 */
@RestController
@RequestMapping("/ai/business/stats")
@RequiredArgsConstructor
public class BusinessStatsController {

    private final BusinessStatsService statsService;

    /**
     * 数据概览（总数/今日/本月）
     */
    @GetMapping("/{configKey}/overview")
    @SaCheckPermission("ai:businessStats:view")
    public RespInfo<Map<String, Object>> overview(@PathVariable String configKey) {
        return RespInfo.success(statsService.overview(configKey));
    }

    /**
     * 按字段分组计数
     */
    @GetMapping("/{configKey}/group")
    @SaCheckPermission("ai:businessStats:view")
    public RespInfo<List<Map<String, Object>>> groupCount(@PathVariable String configKey,
                                                           @RequestParam String field) {
        return RespInfo.success(statsService.groupCount(configKey, field));
    }

    /**
     * 时间趋势统计
     */
    @GetMapping("/{configKey}/trend")
    @SaCheckPermission("ai:businessStats:view")
    public RespInfo<List<Map<String, Object>>> trend(@PathVariable String configKey,
                                                      @RequestParam(defaultValue = "day") String period,
                                                      @RequestParam(defaultValue = "30") Integer days) {
        return RespInfo.success(statsService.trend(configKey, period, days));
    }

    /**
     * 可配置业务指标
     */
    @GetMapping("/{configKey}/metrics")
    @SaCheckPermission("ai:businessStats:view")
    public RespInfo<List<BusinessStatsMetricVO>> metrics(@PathVariable String configKey,
                                                         BusinessStatsMetricQueryDTO query) {
        return RespInfo.success(statsService.metrics(configKey, query));
    }
}
