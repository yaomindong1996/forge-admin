package com.mdframe.forge.admin.integration.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.admin.integration.dto.ApplicationCapabilityCandidate;
import com.mdframe.forge.admin.integration.dto.ApplicationConnectionOption;
import com.mdframe.forge.admin.integration.dto.ApplicationIntegrationConfig;
import com.mdframe.forge.admin.integration.mapper.ApplicationIntegrationMapper;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessMessageChannel;
import com.mdframe.forge.plugin.generator.mapper.BusinessMessageChannelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationRuntimeService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.starter.collaboration.CollaborationCapability;
import com.mdframe.forge.starter.collaboration.provider.CollaborationProviderRegistry;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.orm.support.DuplicateKeyActions;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import com.mdframe.forge.starter.social.enums.SocialPlatform;
import com.mdframe.forge.starter.social.service.ISocialAppConfigService;
import com.mdframe.forge.starter.social.service.ISocialConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Admin composition boundary; the underlying plugins do not acquire circular dependencies. */
@Service
@RequiredArgsConstructor
public class ApplicationIntegrationService {
    private final ApplicationIntegrationMapper mapper;
    private final BusinessApplicationService applications;
    private final BusinessApplicationRuntimeService runtime;
    private final BusinessObjectMapper objects;
    private final AiCapabilityMapper capabilities;
    private final BusinessMessageChannelMapper channels;
    private final ISocialConfigService connections;
    private final ISocialAppConfigService socialApps;
    private final CollaborationProviderRegistry providers;
    private final ObjectMapper objectMapper;

    public Long tenant() {
        Long id = SessionHelper.getTenantId();
        if (id == null || id <= 0) throw new BusinessException("缺少可信租户上下文");
        return id;
    }

    public BusinessApplicationVO application(Long appId) {
        tenant();
        return applications.detail(appId);
    }

    public ApplicationIntegrationConfig config(Long appId) {
        application(appId);
        ApplicationIntegrationConfig config = mapper.config(tenant(), appId);
        return config == null ? new ApplicationIntegrationConfig() : config;
    }

    public List<ApplicationConnectionOption> connections(Long appId) {
        application(appId);
        SysSocialConfig query = new SysSocialConfig();
        query.setTenantId(tenant());
        query.setStatus(EnableStatus.ENABLED.getCode());
        return connections.selectConfigList(query).stream()
                .filter(c -> Objects.equals(c.getTenantId(), tenant()) && EnableStatus.ENABLED.matches(c.getStatus()))
                .map(this::option).toList();
    }

    private ApplicationConnectionOption option(SysSocialConfig c) {
        return new ApplicationConnectionOption(c.getId(), c.getConnectionCode(), c.getConnectionName(),
                c.getPlatformName(), available(c, CollaborationCapability.LOGIN)
                && EnableStatus.ENABLED.matches(c.getSsoWorkbenchEnabled()), available(c, CollaborationCapability.MESSAGE));
    }

    private boolean available(SysSocialConfig c, CollaborationCapability capability) {
        if (!providers.supports(c.getPlatform(), capability)) return false;
        try {
            var app = socialApps.requireEnabledApp(tenant(), c.getId(), capability);
            if (app == null) return false;
            var secret = socialApps.secretSummary(app);
            if (secret == null || !secret.configured()) return false;
            // WeCom login and messaging both consume CorpID + AgentId at execution.
            return !SocialPlatform.WECHAT_ENTERPRISE.getCode().equals(c.getPlatform())
                    || (StringUtils.hasText(c.getEnterpriseId()) && StringUtils.hasText(app.getAgentId()));
        } catch (BusinessException unavailable) { return false; }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplicationIntegrationConfig save(Long appId, ApplicationIntegrationConfig config) {
        BusinessApplicationVO app = application(appId);
        ApplicationConnectionOption selected = null;
        if (config.getConnectionId() != null) {
            SysSocialConfig connection = connections.selectConfigById(config.getConnectionId());
            if (connection == null || !Objects.equals(connection.getTenantId(), tenant())
                    || !EnableStatus.ENABLED.matches(connection.getStatus()))
                throw new BusinessException("连接不存在、已停用或不属于当前租户");
            selected = option(connection);
            if (!selected.loginAvailable() && !selected.messageAvailable())
                throw new BusinessException("请先在企业协同中配置可用的免登或消息应用");
        }
        DuplicateKeyActions.insertIgnoreConflict(() -> mapper.initialize(IdWorker.getId(), tenant(), appId, SessionHelper.getUserId(), SessionHelper.getActiveOrgId()));
        if (mapper.save(tenant(), appId, config, SessionHelper.getUserId()) != 1)
            throw new BusinessException("配置已被其他人修改，请刷新后重试");
        syncChannel(app, selected);
        return mapper.config(tenant(), appId);
    }

    private void syncChannel(BusinessApplicationVO app, ApplicationConnectionOption connection) {
        String code = "app_" + app.getId() + "_collaboration";
        AiBusinessMessageChannel channel = channels.selectByChannelCode(tenant(), code);
        boolean create = channel == null;
        if (create && connection == null) return;
        if (create) {
            channel = new AiBusinessMessageChannel();
            channel.setTenantId(tenant());
            channel.setChannelCode(code);
        }
        channel.setChannelName(app.getApplicationName() + " · 企业协同");
        channel.setChannelType(com.mdframe.forge.plugin.generator.enums.BusinessMessageChannelType.COLLABORATION.getCode());
        channel.setChannelConfigRef(connection == null ? null : connection.id().toString());
        // This flag represents the application binding, not a cached platform readiness snapshot.
        // CollaborationMessageChannel revalidates the current connection and MESSAGE app per delivery.
        channel.setStatus(connection != null
                ? EnableStatus.ENABLED.getCode() : EnableStatus.DISABLED.getCode());
        channel.setDescription("由应用集成管理；在业务流程发送消息步骤中选择此通道。取消绑定后禁止发送。");
        if (create) channels.insert(channel); else channels.updateById(channel);
    }

    public void requireSource(Long appId, String suiteCode, String objectCode) {
        application(appId);
        var published = runtime.runtimeById(appId);
        if (!StringUtils.hasText(suiteCode) || !StringUtils.hasText(objectCode)) {
            throw new BusinessException("请选择有效的已发布业务对象");
        }
        var source = objects.selectByObjectCode(tenant(), suiteCode, objectCode);
        // Legacy snapshots omitted suiteCode. Resolve the tenant-scoped identity, never draft membership
        // or the application's suite: referenced objects may belong to a different suite.
        boolean member = source != null && source.getId() != null && source.getId() > 0
                && published.getObjects().stream().anyMatch(o -> Objects.equals(o.getObjectId(), source.getId())
                && Objects.equals(o.getObjectCode(), objectCode)
                && (!StringUtils.hasText(o.getSuiteCode()) || Objects.equals(o.getSuiteCode(), suiteCode)));
        if (!member) throw new BusinessException("所选业务对象不在当前应用的已发布版本中，请确认来源或重新发布应用");
    }

    public void requirePublish(Long appId, String code) {
        syncCapabilities(appId);
        AiCapability existing = capabilities.selectByCode(tenant(), code);
        if (existing != null && !Objects.equals(mapper.member(tenant(), appId, existing.getId()), existing.getId()))
            throw new BusinessException("能力编码已被平台或其他应用占用，请使用新的编码");
    }

    public void requireUniqueSource(
            Long appId,
            String capabilityCode,
            String sourceType,
            String sourceKey,
            JsonNode parameters) {
        List<ApplicationCapabilityCandidate> existing = mapper.applicationSourceCapabilities(
                tenant(), appId, sourceType, sourceKey);
        if (existing == null) {
            return;
        }
        for (ApplicationCapabilityCandidate candidate : existing) {
            if (!Objects.equals(candidate.getCapabilityCode(), capabilityCode)
                    && sameRegistrationSource(candidate, sourceType, sourceKey, parameters)) {
                throw new BusinessException("该来源已注册为能力 " + candidate.getCapabilityCode() + "，无需重复注册");
            }
        }
    }

    private boolean sameRegistrationSource(
            ApplicationCapabilityCandidate candidate,
            String sourceType,
            String sourceKey,
            JsonNode parameters) {
        if ("BUSINESS_ACTION".equals(sourceType) || "FLOW_ACTION".equals(sourceType)) {
            return true;
        }
        if (!"SYSTEM_SERVICE".equals(sourceType)) {
            return false;
        }
        JsonNode policy = readObject(candidate.getPolicySnapshot());
        if (policy == null || parameters == null || !parameters.isObject()) {
            return false;
        }
        JsonNode registered = policy.path("registrationParameters");
        if (!registered.isObject()) {
            registered = policy;
        }
        if ("lowcode.form.create".equals(sourceKey)) {
            return sameText(registered, parameters, "suiteCode")
                    && sameText(registered, parameters, "objectCode");
        }
        if ("lowcode.business-process.start".equals(sourceKey)) {
            return sameText(registered, parameters, "applicationId")
                    && sameText(registered, parameters, "objectId")
                    && sameText(registered, parameters, "processCode");
        }
        if ("system.rest.invoke".equals(sourceKey)) {
            return sameText(registered, parameters, "endpointId");
        }
        return false;
    }

    private boolean sameText(JsonNode left, JsonNode right, String field) {
        return StringUtils.hasText(left.path(field).asText())
                && Objects.equals(left.path(field).asText(), right.path(field).asText());
    }

    public void attach(Long appId, Long capabilityId) {
        DuplicateKeyActions.insertIgnoreConflict(() -> mapper.attach(IdWorker.getId(), tenant(), appId, capabilityId, SessionHelper.getUserId(), SessionHelper.getActiveOrgId()));
    }

    public AiCapability requireCapability(Long appId, Long capabilityId) {
        application(appId);
        if (capabilityId == null || capabilityId <= 0 || !Objects.equals(mapper.member(tenant(), appId, capabilityId), capabilityId))
            throw new BusinessException("该能力不属于当前应用");
        AiCapability capability = capabilities.selectTenantById(tenant(), capabilityId);
        if (capability == null) throw new BusinessException("能力不存在");
        return capability;
    }

    public Page<AiCapability> capabilities(Long appId, PageQuery page, String keyword) {
        application(appId);
        return mapper.capabilities(page.toPage(), tenant(), appId, keyword);
    }

    /**
     * Reconciles catalog capabilities against the immutable published application snapshot.
     * This is an explicit write operation: ordinary list queries remain read-only.
     */
    @Transactional(rollbackFor = Exception.class)
    public int syncCapabilities(Long appId) {
        application(appId);
        Long tenantId = tenant();
        var published = runtime.runtimeById(appId);
        List<ApplicationCapabilityCandidate> candidates = mapper.unattachedPublishedCandidates(tenantId, appId);
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }
        List<ApplicationCapabilityCandidate> alreadyClaimed = mapper.applicationPublishedCandidates(tenantId, appId);
        List<ApplicationCapabilityCandidate> claimed = alreadyClaimed == null
                ? new ArrayList<>() : new ArrayList<>(alreadyClaimed);
        int attached = 0;
        for (ApplicationCapabilityCandidate candidate : candidates) {
            if (!belongsToPublishedApplication(appId, published.getObjects(), candidate)
                    || claimed.stream().anyMatch(existing -> sameRegistrationSource(existing, candidate))) {
                continue;
            }
            attached += DuplicateKeyActions.insertIgnoreConflict(() -> mapper.attach(IdWorker.getId(), tenantId, appId, candidate.getCapabilityId(),
                    SessionHelper.getUserId(), SessionHelper.getActiveOrgId()));
            claimed.add(candidate);
        }
        return attached;
    }

    private boolean sameRegistrationSource(
            ApplicationCapabilityCandidate left,
            ApplicationCapabilityCandidate right) {
        if (left == null || right == null
                || !Objects.equals(left.getSourceType(), right.getSourceType())
                || !Objects.equals(left.getSourceKey(), right.getSourceKey())) {
            return false;
        }
        if ("BUSINESS_ACTION".equals(right.getSourceType()) || "FLOW_ACTION".equals(right.getSourceType())) {
            return true;
        }
        JsonNode policy = readObject(right.getPolicySnapshot());
        if (policy == null) {
            return false;
        }
        JsonNode parameters = policy.path("registrationParameters");
        return sameRegistrationSource(left, right.getSourceType(), right.getSourceKey(),
                parameters.isObject() ? parameters : policy);
    }

    private boolean belongsToPublishedApplication(
            Long appId,
            List<com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO> publishedObjects,
            ApplicationCapabilityCandidate candidate) {
        if (candidate == null || candidate.getCapabilityId() == null) {
            return false;
        }
        if ("BUSINESS_ACTION".equals(candidate.getSourceType())
                || "FLOW_ACTION".equals(candidate.getSourceType())) {
            String[] source = Objects.toString(candidate.getSourceKey(), "").split("/", -1);
            return source.length >= 3 && containsPublishedObject(publishedObjects, null, source[0], source[1]);
        }
        if (!"SYSTEM_SERVICE".equals(candidate.getSourceType())) {
            return false;
        }
        JsonNode policy = readObject(candidate.getPolicySnapshot());
        if (policy == null) {
            return false;
        }
        if ("lowcode.form.create".equals(candidate.getSourceKey())) {
            JsonNode parameters = policy.path("registrationParameters");
            String suiteCode = text(parameters, "suiteCode", text(policy, "suiteCode", null));
            String objectCode = text(parameters, "objectCode", text(policy, "objectCode", null));
            return containsPublishedObject(publishedObjects, null, suiteCode, objectCode);
        }
        if ("lowcode.business-process.start".equals(candidate.getSourceKey())) {
            JsonNode parameters = policy.path("registrationParameters");
            String sourceAppId = text(parameters, "applicationId", text(policy, "applicationId", null));
            String objectId = text(parameters, "objectId", text(policy, "objectId", null));
            return appId.toString().equals(sourceAppId)
                    && containsPublishedObject(publishedObjects, objectId, null, null);
        }
        // Generic REST and unrelated system services are platform capabilities, not application assets.
        return false;
    }

    private boolean containsPublishedObject(
            List<com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO> publishedObjects,
            String objectId,
            String suiteCode,
            String objectCode) {
        if (publishedObjects == null) {
            return false;
        }
        return publishedObjects.stream().anyMatch(item -> {
            if (StringUtils.hasText(objectId)) {
                return item.getObjectId() != null && objectId.equals(item.getObjectId().toString());
            }
            return StringUtils.hasText(objectCode)
                    && objectCode.equals(item.getObjectCode())
                    && (!StringUtils.hasText(item.getSuiteCode()) || Objects.equals(suiteCode, item.getSuiteCode()));
        });
    }

    private JsonNode readObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node != null && node.isObject() ? node : null;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private String text(JsonNode node, String field, String fallback) {
        if (node != null && node.isObject() && node.path(field).isValueNode()
                && StringUtils.hasText(node.path(field).asText())) {
            return node.path(field).asText();
        }
        return fallback;
    }
}
