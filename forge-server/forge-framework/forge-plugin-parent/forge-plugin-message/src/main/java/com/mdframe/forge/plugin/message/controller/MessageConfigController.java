package com.mdframe.forge.plugin.message.controller;

import com.mdframe.forge.plugin.message.domain.entity.SysEmailConfig;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsConfig;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsPingLog;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsRoutePolicy;
import com.mdframe.forge.plugin.message.service.EmailConfigService;
import com.mdframe.forge.plugin.message.service.SmsConfigService;
import com.mdframe.forge.plugin.message.service.impl.MessageChannelConfigSupport;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/message-config")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class MessageConfigController {

    private final SmsConfigService smsConfigService;
    private final EmailConfigService emailConfigService;

    @ModelAttribute
    public void assertPlatformAdmin() {
        SessionHelper.assertAdmin("只有超级管理员可以管理短信和邮件通道");
    }

    @GetMapping("/sms")
    @OperationLog(module = "消息配置管理", type = OperationType.QUERY, desc = "获取短信主配置")
    public RespInfo<SysSmsConfig> getSmsConfig() {
        SysSmsConfig config = smsConfigService.getEnabledConfig(SessionHelper.getTenantId());
        return RespInfo.success(maskSms(config));
    }

    @GetMapping("/sms/list")
    @OperationLog(module = "消息配置管理", type = OperationType.QUERY, desc = "短信配置列表")
    public RespInfo<List<SysSmsConfig>> listSmsConfigs() {
        List<SysSmsConfig> list = smsConfigService.listByTenant(SessionHelper.getTenantId());
        if (list != null) {
            list.forEach(this::maskSms);
        }
        return RespInfo.success(list);
    }

    @GetMapping("/sms/{id}")
    @OperationLog(module = "消息配置管理", type = OperationType.QUERY, desc = "短信配置详情")
    public RespInfo<SysSmsConfig> getSmsConfigById(@PathVariable Long id) {
        return RespInfo.success(maskSms(smsConfigService.getById(id)));
    }

    @PostMapping("/sms/save")
    @OperationLog(module = "消息配置管理", type = OperationType.ADD, desc = "保存短信配置")
    public RespInfo<Void> saveSmsConfig(@RequestBody SysSmsConfig config) {
        boolean result = config.getId() == null
                ? smsConfigService.save(config)
                : smsConfigService.updateById(config);
        return result ? RespInfo.success() : RespInfo.error("保存失败");
    }

    @DeleteMapping("/sms/{id}")
    @OperationLog(module = "消息配置管理", type = OperationType.DELETE, desc = "删除短信配置")
    public RespInfo<Void> deleteSmsConfig(@PathVariable Long id) {
        return smsConfigService.deleteById(id) ? RespInfo.success() : RespInfo.error("删除失败");
    }

    @PostMapping("/sms/updateStatus")
    @OperationLog(module = "消息配置管理", type = OperationType.UPDATE, desc = "更新短信配置状态")
    public RespInfo<Void> updateSmsStatus(@RequestParam Long id, @RequestParam Integer status) {
        boolean result = smsConfigService.updateStatus(id, status);
        return result ? RespInfo.success() : RespInfo.error("更新状态失败");
    }

    @PostMapping("/sms/set-primary")
    @OperationLog(module = "消息配置管理", type = OperationType.UPDATE, desc = "设置主短信通道")
    public RespInfo<Void> setSmsPrimary(@RequestParam Long id) {
        return smsConfigService.setPrimary(id) ? RespInfo.success() : RespInfo.error("设置失败");
    }

    @GetMapping("/sms/route-policy")
    @OperationLog(module = "消息配置管理", type = OperationType.QUERY, desc = "获取短信路由策略")
    public RespInfo<SysSmsRoutePolicy> getSmsRoutePolicy() {
        return RespInfo.success(smsConfigService.getRoutePolicy(SessionHelper.getTenantId()));
    }

    @PutMapping("/sms/route-policy")
    @OperationLog(module = "消息配置管理", type = OperationType.UPDATE, desc = "保存短信路由策略")
    public RespInfo<SysSmsRoutePolicy> saveSmsRoutePolicy(@RequestBody SysSmsRoutePolicy policy) {
        policy.setTenantId(SessionHelper.getTenantId());
        return RespInfo.success(smsConfigService.saveRoutePolicy(policy));
    }

    @PostMapping("/sms/ping/test")
    @OperationLog(module = "消息配置管理", type = OperationType.OTHER, desc = "立即短信 Ping")
    public RespInfo<Map<String, Object>> pingSmsNow() {
        int success = smsConfigService.runPingNow(SessionHelper.getTenantId());
        Map<String, Object> data = new HashMap<>();
        data.put("successCount", success);
        return RespInfo.success(data);
    }

    @GetMapping("/sms/ping/logs")
    @OperationLog(module = "消息配置管理", type = OperationType.QUERY, desc = "短信 Ping 日志")
    public RespInfo<List<SysSmsPingLog>> listSmsPingLogs(
            @RequestParam(defaultValue = "50") Integer limit) {
        return RespInfo.success(smsConfigService.listPingLogs(SessionHelper.getTenantId(), limit));
    }

    @GetMapping("/email")
    @OperationLog(module = "消息配置管理", type = OperationType.QUERY, desc = "获取邮件配置")
    public RespInfo<SysEmailConfig> getEmailConfig() {
        SysEmailConfig config = emailConfigService.getEnabledConfig(SessionHelper.getTenantId());
        return RespInfo.success(maskEmail(config));
    }

    @PostMapping("/email/save")
    @OperationLog(module = "消息配置管理", type = OperationType.ADD, desc = "保存邮件配置")
    public RespInfo<Void> saveEmailConfig(@RequestBody SysEmailConfig config) {
        boolean result = config.getId() == null
                ? emailConfigService.save(config)
                : emailConfigService.updateById(config);
        return result ? RespInfo.success() : RespInfo.error("保存失败");
    }

    @PostMapping("/email/updateStatus")
    @OperationLog(module = "消息配置管理", type = OperationType.UPDATE, desc = "更新邮件配置状态")
    public RespInfo<Void> updateEmailStatus(@RequestParam Long id, @RequestParam Integer status) {
        boolean result = emailConfigService.updateStatus(id, status);
        return result ? RespInfo.success() : RespInfo.error("更新状态失败");
    }

    @GetMapping("/sms/suppliers")
    public RespInfo<Map<String, String>> getSmsSuppliers() {
        Map<String, String> suppliers = new HashMap<>();
        suppliers.put("alibaba", "阿里云");
        suppliers.put("tencent", "腾讯云");
        suppliers.put("huawei", "华为云");
        suppliers.put("jdcloud", "京东云");
        suppliers.put("cloopen", "容联云");
        suppliers.put("netease", "网易云信");
        suppliers.put("ctyun", "天翼云");
        suppliers.put("qiniu", "七牛云");
        suppliers.put("unisms", "合一短信");
        suppliers.put("yunpian", "云片短信");
        suppliers.put("zhutong", "助通短信");
        suppliers.put("lianlu", "联麓短信");
        suppliers.put("dingzhong", "鼎众短信");
        suppliers.put("chuanglan", "创蓝短信");
        suppliers.put("jiguang", "极光短信");
        suppliers.put("buding_v2", "布丁云");
        suppliers.put("mas", "中国移动云MAS");
        suppliers.put("baidu", "百度云");
        suppliers.put("luosimao", "螺丝帽");
        suppliers.put("mysubmail", "SUBMAIL");
        suppliers.put("danmi", "单米短信");
        suppliers.put("emay", "亿美软通");
        return RespInfo.success(suppliers);
    }

    private SysSmsConfig maskSms(SysSmsConfig config) {
        if (config == null) {
            return null;
        }
        config.setAccessKeySecret(MessageChannelConfigSupport.maskSecret(config.getAccessKeySecret()));
        return config;
    }

    private static SysEmailConfig maskEmail(SysEmailConfig config) {
        if (config == null) {
            return null;
        }
        config.setPassword(MessageChannelConfigSupport.maskSecret(config.getPassword()));
        return config;
    }
}
