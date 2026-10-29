package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsConfig;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsPingLog;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsRoutePolicy;
import com.mdframe.forge.plugin.message.enums.SmsChannelRole;
import com.mdframe.forge.plugin.message.enums.SmsRouteMode;
import com.mdframe.forge.plugin.message.mapper.SysSmsConfigMapper;
import com.mdframe.forge.plugin.message.mapper.SysSmsPingLogMapper;
import com.mdframe.forge.plugin.message.mapper.SysSmsRoutePolicyMapper;
import com.mdframe.forge.plugin.message.service.SmsConfigService;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import com.mdframe.forge.starter.message.channel.ChannelType;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.config.SmsBlendRefresher;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import com.mdframe.forge.starter.message.sms.SmsScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsConfigServiceImpl extends ServiceImpl<SysSmsConfigMapper, SysSmsConfig>
        implements SmsConfigService {

    private final SysSmsRoutePolicyMapper routePolicyMapper;
    private final SysSmsPingLogMapper pingLogMapper;
    private final ObjectProvider<SmsBlendRefresher> blendRefresherProvider;
    private final ObjectProvider<MessageClient> messageClientProvider;

    @Override
    public SysSmsConfig getEnabledConfig(Long tenantId) {
        return baseMapper.selectEnabledConfig(MessageChannelConfigSupport.resolveTenantId(tenantId));
    }

    @Override
    public List<SysSmsConfig> listByTenant(Long tenantId) {
        return baseMapper.selectByTenantId(MessageChannelConfigSupport.resolveTenantId(tenantId));
    }

    @Override
    public SysSmsConfig getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysSmsConfig config) {
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(config.getTenantId());
        config.setTenantId(tenantId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        normalizeDefaults(config);
        ensurePrimaryExclusive(config, tenantId);
        boolean ok = super.save(config);
        refreshBlend();
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SysSmsConfig config) {
        preserveMaskedSecret(config);
        SysSmsConfig existing = config.getId() == null ? null : getById(config.getId());
        if (existing != null) {
            assertLastEnabledProtected(existing, config.getStatus());
        }
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(
                config.getTenantId() != null ? config.getTenantId()
                        : (existing == null ? null : existing.getTenantId()));
        config.setTenantId(tenantId);
        normalizeDefaults(config);
        ensurePrimaryExclusive(config, tenantId);
        config.setUpdateTime(LocalDateTime.now());
        boolean ok = super.updateById(config);
        refreshBlend();
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        SysSmsConfig existing = getById(id);
        if (existing == null) {
            throw new BusinessException("短信配置不存在");
        }
        assertLastEnabledProtected(existing, status);
        SysSmsConfig config = new SysSmsConfig();
        config.setId(id);
        config.setStatus(status);
        config.setUpdateTime(LocalDateTime.now());
        boolean ok = super.updateById(config);
        refreshBlend();
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        SysSmsConfig existing = getById(id);
        if (existing == null) {
            throw new BusinessException("短信配置不存在");
        }
        if (EnableStatus.ENABLED.matches(existing.getStatus())) {
            Long tenantId = MessageChannelConfigSupport.resolveTenantId(existing.getTenantId());
            long otherEnabled = count(new LambdaQueryWrapper<SysSmsConfig>()
                    .eq(SysSmsConfig::getTenantId, tenantId)
                    .eq(SysSmsConfig::getStatus, EnableStatus.ENABLED.getCode())
                    .ne(SysSmsConfig::getId, existing.getId()));
            MessageChannelConfigSupport.assertCanDisable(true, otherEnabled, "短信");
        }
        boolean ok = removeById(id);
        refreshBlend();
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setPrimary(Long id) {
        SysSmsConfig existing = getById(id);
        if (existing == null) {
            throw new BusinessException("短信配置不存在");
        }
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(existing.getTenantId());
        update(new LambdaUpdateWrapper<SysSmsConfig>()
                .eq(SysSmsConfig::getTenantId, tenantId)
                .set(SysSmsConfig::getChannelRole, SmsChannelRole.SECONDARY.getCode())
                .set(SysSmsConfig::getUpdateTime, LocalDateTime.now()));
        SysSmsConfig patch = new SysSmsConfig();
        patch.setId(id);
        patch.setChannelRole(SmsChannelRole.PRIMARY.getCode());
        if (!EnableStatus.ENABLED.matches(existing.getStatus())) {
            patch.setStatus(EnableStatus.ENABLED.getCode());
        }
        patch.setUpdateTime(LocalDateTime.now());
        boolean ok = super.updateById(patch);
        refreshBlend();
        return ok;
    }

    @Override
    public SysSmsRoutePolicy getRoutePolicy(Long tenantId) {
        Long resolved = MessageChannelConfigSupport.resolveTenantId(tenantId);
        SysSmsRoutePolicy policy = routePolicyMapper.selectByTenantId(resolved);
        if (policy != null) {
            return policy;
        }
        SysSmsRoutePolicy defaults = new SysSmsRoutePolicy();
        defaults.setTenantId(resolved);
        defaults.setRouteMode(SmsRouteMode.WEIGHTED_RR.getCode());
        defaults.setCaptchaLockPrimary(1);
        defaults.setFailoverEnabled(1);
        defaults.setFailoverMaxAttempts(2);
        defaults.setPingEnabled(0);
        defaults.setPingCron("0 0 */6 * * ?");
        defaults.setPingMode("PER_CHANNEL");
        return defaults;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysSmsRoutePolicy saveRoutePolicy(SysSmsRoutePolicy policy) {
        if (policy == null) {
            throw new BusinessException("路由策略不能为空");
        }
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(policy.getTenantId());
        policy.setTenantId(tenantId);
        policy.setRouteMode(SmsRouteMode.from(policy.getRouteMode()).getCode());
        if (policy.getCaptchaLockPrimary() == null) {
            policy.setCaptchaLockPrimary(1);
        }
        if (policy.getFailoverEnabled() == null) {
            policy.setFailoverEnabled(1);
        }
        if (policy.getFailoverMaxAttempts() == null || policy.getFailoverMaxAttempts() < 1) {
            policy.setFailoverMaxAttempts(2);
        }
        if (policy.getPingEnabled() == null) {
            policy.setPingEnabled(0);
        }
        if (StringUtils.isBlank(policy.getPingCron())) {
            policy.setPingCron("0 0 */6 * * ?");
        }
        if (StringUtils.isBlank(policy.getPingMode())) {
            policy.setPingMode("PER_CHANNEL");
        }
        SysSmsRoutePolicy existing = routePolicyMapper.selectByTenantId(tenantId);
        if (existing == null) {
            policy.setCreateTime(LocalDateTime.now());
            policy.setUpdateTime(LocalDateTime.now());
            routePolicyMapper.insert(policy);
        } else {
            policy.setUpdateTime(LocalDateTime.now());
            routePolicyMapper.updateById(policy);
        }
        return getRoutePolicy(tenantId);
    }

    @Override
    public List<SysSmsPingLog> listPingLogs(Long tenantId, Integer limit) {
        int size = limit == null || limit < 1 ? 50 : Math.min(limit, 200);
        return pingLogMapper.selectRecent(MessageChannelConfigSupport.resolveTenantId(tenantId), size);
    }

    @Override
    public int runPingNow(Long tenantId) {
        Long resolved = MessageChannelConfigSupport.resolveTenantId(tenantId);
        SysSmsRoutePolicy policy = getRoutePolicy(resolved);
        List<String> phones = parsePhones(policy.getPingPhones());
        if (phones.isEmpty()) {
            throw new BusinessException("请先配置运维 Ping 手机号");
        }
        MessageClient client = messageClientProvider.getIfAvailable();
        if (client == null) {
            throw new BusinessException("消息客户端未就绪");
        }
        List<SysSmsConfig> channels = baseMapper.selectEnabledConfigs(resolved);
        if (channels == null || channels.isEmpty()) {
            throw new BusinessException("没有启用的短信通道");
        }
        int success = 0;
        for (SysSmsConfig channel : channels) {
            for (String phone : phones) {
                boolean ok = sendPing(client, channel, phone);
                writePingLog(resolved, channel, phone, ok, ok ? null : "ping failed");
                if (ok) {
                    success++;
                }
            }
        }
        return success;
    }

    private boolean sendPing(MessageClient client, SysSmsConfig channel, String phone) {
        MessageChannel.SendRequest request = new MessageChannel.SendRequest();
        request.setChannel(ChannelType.SMS.name());
        request.setType(ChannelType.SMS.name());
        request.setScene(SmsScene.SMS_PING);
        request.setPreferredSmsConfigId(channel.getConfigId());
        request.setPhoneList(List.of(phone));
        request.setContent("Forge SMS Ping ${time}");
        request.setParams(java.util.Map.of("time", LocalDateTime.now().toString()));
        try {
            MessageChannel.SendResult result = client.send(request);
            return result != null && result.success;
        } catch (Exception ex) {
            log.warn("短信 Ping 失败: configId={}, err={}", channel.getConfigId(), ex.getMessage());
            return false;
        }
    }

    private void writePingLog(Long tenantId, SysSmsConfig channel, String phone, boolean ok, String error) {
        SysSmsPingLog logRow = new SysSmsPingLog();
        logRow.setTenantId(tenantId);
        logRow.setConfigId(channel.getConfigId());
        logRow.setSupplier(channel.getSupplier());
        logRow.setPhone(SensitiveDataUtil.maskPhone(phone));
        logRow.setStatus(ok ? 1 : 0);
        logRow.setErrorMsg(error);
        logRow.setSendTime(LocalDateTime.now());
        logRow.setCreateTime(LocalDateTime.now());
        pingLogMapper.insert(logRow);
    }

    private List<String> parsePhones(String raw) {
        if (StringUtils.isBlank(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void normalizeDefaults(SysSmsConfig config) {
        if (config.getWeight() == null) {
            config.setWeight(1);
        }
        if (config.getRetryInterval() == null) {
            config.setRetryInterval(5);
        }
        if (config.getMaxRetries() == null) {
            config.setMaxRetries(0);
        }
        if (config.getStatus() == null) {
            config.setStatus(EnableStatus.DISABLED.getCode());
        }
        config.setChannelRole(SmsChannelRole.from(config.getChannelRole()).getCode());
    }

    private void ensurePrimaryExclusive(SysSmsConfig config, Long tenantId) {
        if (!SmsChannelRole.PRIMARY.matches(config.getChannelRole())) {
            return;
        }
        LambdaUpdateWrapper<SysSmsConfig> wrapper = new LambdaUpdateWrapper<SysSmsConfig>()
                .eq(SysSmsConfig::getTenantId, tenantId)
                .set(SysSmsConfig::getChannelRole, SmsChannelRole.SECONDARY.getCode())
                .set(SysSmsConfig::getUpdateTime, LocalDateTime.now());
        if (config.getId() != null) {
            wrapper.ne(SysSmsConfig::getId, config.getId());
        }
        update(wrapper);
    }

    private void preserveMaskedSecret(SysSmsConfig config) {
        if (config == null || config.getId() == null) {
            return;
        }
        if (!SensitiveDataUtil.isMaskedValue(config.getAccessKeySecret())) {
            return;
        }
        SysSmsConfig existing = getById(config.getId());
        if (existing != null) {
            config.setAccessKeySecret(existing.getAccessKeySecret());
        }
    }

    private void assertLastEnabledProtected(SysSmsConfig existing, Integer nextStatus) {
        if (!MessageChannelConfigSupport.isDisabling(existing.getStatus(), nextStatus)) {
            return;
        }
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(existing.getTenantId());
        long otherEnabled = count(new LambdaQueryWrapper<SysSmsConfig>()
                .eq(SysSmsConfig::getTenantId, tenantId)
                .eq(SysSmsConfig::getStatus, EnableStatus.ENABLED.getCode())
                .ne(SysSmsConfig::getId, existing.getId()));
        MessageChannelConfigSupport.assertCanDisable(true, otherEnabled, "短信");
    }

    private void refreshBlend() {
        SmsBlendRefresher refresher = blendRefresherProvider.getIfAvailable();
        if (refresher != null) {
            refresher.refresh();
        }
    }
}
