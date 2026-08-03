package com.mdframe.forge.plugin.capability.controlplane.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityGrant;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityGrantCreateDTO;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityGrantUpdateDTO;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityGrantMapper;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityVersionMapper;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityGrantDecision;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityGrantPolicy;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CapabilityGrantService {

    private static final Set<String> VERSION_STRATEGIES = Set.of("PINNED", "FOLLOW_MAJOR");

    private final AiCapabilityGrantMapper grantMapper;
    private final AiCapabilityVersionMapper versionMapper;
    private final CapabilityCatalogService catalogService;
    private final CapabilityClientService clientService;
    private final CapabilityGrantPolicy grantPolicy;
    private final ObjectMapper objectMapper;
    private final Clock capabilityClock;

    public Page<AiCapabilityGrant> page(
            Long tenantId,
            PageQuery pageQuery,
            Long clientId,
            Long capabilityId,
            String status) {
        return grantMapper.selectPage(
                pageQuery.toPage(), requireTenant(tenantId), clientId, capabilityId, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long grant(Long tenantId, CapabilityGrantCreateDTO dto) {
        AiCapabilityClient client = clientService.requireClient(tenantId, dto.clientId());
        AiCapability capability = catalogService.getById(tenantId, dto.capabilityId());
        if (!client.getTenantId().equals(capability.getTenantId())) {
            throw new BusinessException("客户端与能力不属于同一租户");
        }
        validateGrantConfiguration(
                tenantId, capability, dto.versionStrategy(), dto.fixedVersion(), dto.fieldPolicy());
        if (grantMapper.selectActiveGrant(tenantId, client.getId(), capability.getId()) != null) {
            throw new BusinessException("客户端已存在该能力授权");
        }
        AiCapabilityGrant grant = new AiCapabilityGrant();
        grant.setTenantId(tenantId);
        grant.setClientId(client.getId());
        grant.setCapabilityId(capability.getId());
        grant.setVersionStrategy(dto.versionStrategy());
        grant.setFixedVersion(dto.fixedVersion());
        grant.setFieldPolicy(writeFieldPolicy(dto.fieldPolicy()));
        grant.setStatus("ENABLED");
        grant.setExpiresAt(dto.expiresAt());
        grant.setDelFlag(0L);
        grantMapper.insert(grant);
        return grant.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long tenantId, Long grantId, CapabilityGrantUpdateDTO dto) {
        Long safeTenantId = requireTenant(tenantId);
        AiCapabilityGrant grant = grantMapper.selectTenantById(safeTenantId, grantId);
        if (grant == null || !"ENABLED".equals(grant.getStatus())) {
            throw new BusinessException("有效授权不存在或无权访问");
        }
        AiCapability capability = catalogService.getById(safeTenantId, grant.getCapabilityId());
        validateGrantConfiguration(
                safeTenantId, capability, dto.versionStrategy(), dto.fixedVersion(), dto.fieldPolicy());
        grant.setVersionStrategy(dto.versionStrategy());
        grant.setFixedVersion(dto.fixedVersion());
        grant.setFieldPolicy(writeFieldPolicy(dto.fieldPolicy()));
        grant.setExpiresAt(dto.expiresAt());
        grantMapper.updateById(grant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void useCurrentVersion(Long tenantId, Long grantId) {
        Long safeTenantId = requireTenant(tenantId);
        AiCapabilityGrant grant = grantMapper.selectTenantById(safeTenantId, grantId);
        if (grant == null || !"ENABLED".equals(grant.getStatus())) {
            throw new BusinessException("有效授权不存在或无权访问");
        }
        AiCapability capability = catalogService.getById(safeTenantId, grant.getCapabilityId());
        String currentVersion = capability.getCurrentVersion();
        if (currentVersion == null || currentVersion.isBlank()) {
            throw new BusinessException("能力没有可切换的当前版本");
        }
        JsonNode fieldPolicy = readStoredFieldPolicy(grant.getFieldPolicy());
        validateGrantConfiguration(
                safeTenantId, capability, grant.getVersionStrategy(), currentVersion, fieldPolicy);
        if (currentVersion.equals(grant.getFixedVersion())) {
            return;
        }
        grant.setFixedVersion(currentVersion);
        if (grantMapper.updateById(grant) == 0) {
            throw new BusinessException("授权版本更新失败，请刷新后重试");
        }
    }

    public void revoke(Long tenantId, Long grantId) {
        if (grantMapper.logicallyRevoke(requireTenant(tenantId), grantId) == 0) {
            throw new BusinessException("授权不存在或无权访问");
        }
    }

    public CapabilityGrantDecision evaluate(
            Long tenantId,
            Long activeOrgId,
            Long clientId,
            String capabilityCode,
            String requestedVersion) {
        AiCapabilityClient client = clientService.requireClient(tenantId, clientId);
        AiCapability capability = catalogService.getByCode(tenantId, capabilityCode);
        AiCapabilityGrant grant = capability == null ? null : grantMapper.selectActiveGrant(
                tenantId, clientId, capability.getId());
        CapabilityGrantDecision decision = grantPolicy.evaluate(
                client, capability, grant, tenantId, activeOrgId,
                requestedVersion, LocalDateTime.now(capabilityClock));
        if (decision.allowed()
                && versionMapper.selectVersion(tenantId, capability.getId(), decision.resolvedVersion()) == null) {
            return CapabilityGrantDecision.deny("VERSION_NOT_FOUND");
        }
        return decision;
    }

    private String writeFieldPolicy(JsonNode fieldPolicy) {
        if (fieldPolicy == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(fieldPolicy);
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException("字段策略格式无效");
        }
    }

    private JsonNode readStoredFieldPolicy(String fieldPolicy) {
        if (fieldPolicy == null || fieldPolicy.isBlank()) {
            return null;
        }
        return readJson(fieldPolicy, "授权字段策略格式无效");
    }

    private void validateGrantableCapability(
            AiCapability capability,
            JsonNode fieldPolicy) {
        if ("HIGH".equals(capability.getRiskLevel())) {
            throw new BusinessException("当前阶段禁止授权高风险能力");
        }
        if ("READ_ONLY".equals(capability.getBehavior())) {
            return;
        }
        if ("ACTION".equals(capability.getBehavior())
                && "MEDIUM".equals(capability.getRiskLevel())
                && "SYSTEM_SERVICE".equals(capability.getSourceType())) {
            return;
        }
        if (!"ACTION".equals(capability.getBehavior())
                || !"MEDIUM".equals(capability.getRiskLevel())
                || !"BUSINESS_ACTION".equals(capability.getSourceType())
                || fieldPolicy == null) {
            if ("FLOW".equals(capability.getBehavior())
                    && "MEDIUM".equals(capability.getRiskLevel())
                    && "FLOW_ACTION".equals(capability.getSourceType())
                    && fieldPolicy != null) {
                return;
            }
            throw new BusinessException("当前阶段只允许授权只读能力、受控中风险业务动作或受控中风险流程动作");
        }
    }

    private void validateGrantConfiguration(
            Long tenantId,
            AiCapability capability,
            String versionStrategy,
            String fixedVersion,
            JsonNode fieldPolicy) {
        validateGrantableCapability(capability, fieldPolicy);
        if (!VERSION_STRATEGIES.contains(versionStrategy)) {
            throw new BusinessException("不支持的授权版本策略");
        }
        var version = versionMapper.selectVersion(tenantId, capability.getId(), fixedVersion);
        if (version == null || !"PUBLISHED".equals(version.getStatus())) {
            throw new BusinessException("授权基准版本不存在或未发布");
        }
        if ("ACTION".equals(capability.getBehavior())
                && "BUSINESS_ACTION".equals(capability.getSourceType())) {
            validateActionFieldPolicy(version.getPolicySnapshot(), fieldPolicy);
        }
        else if ("ACTION".equals(capability.getBehavior())
                && "SYSTEM_SERVICE".equals(capability.getSourceType())) {
            validateSystemServicePolicy(fieldPolicy);
        }
        else if ("FLOW".equals(capability.getBehavior())) {
            validateFlowOperationPolicy(version.getPolicySnapshot(), fieldPolicy);
        }
    }

    private void validateActionFieldPolicy(String policySnapshot, JsonNode grantPolicy) {
        Set<String> versionFields = readAllowedFields(readJson(policySnapshot, "能力版本字段策略无效"));
        Set<String> grantFields = readAllowedFields(grantPolicy);
        if (versionFields.isEmpty() || grantFields.isEmpty()) {
            throw new BusinessException("受控业务动作授权必须配置非空字段白名单");
        }
        if (!versionFields.containsAll(grantFields)) {
            throw new BusinessException("授权字段不能超出能力版本字段白名单");
        }
        JsonNode confirmationMode = readJson(policySnapshot, "能力版本字段策略无效")
                .path("confirmationMode");
        if (!"MCP_ELICITATION".equals(confirmationMode.asText())) {
            throw new BusinessException("受控业务动作缺少 MCP elicitation 确认策略");
        }
    }

    private void validateFlowOperationPolicy(String policySnapshot, JsonNode grantPolicy) {
        JsonNode versionPolicy = readJson(policySnapshot, "流程能力版本策略无效");
        Set<String> versionOperations = readTextSet(versionPolicy.path("allowedOperations"));
        Set<String> grantOperations = readTextSet(
                grantPolicy == null ? null : grantPolicy.path("allowedOperations"));
        if (versionOperations.isEmpty() || grantOperations.isEmpty()) {
            throw new BusinessException("受控流程动作授权必须配置非空操作白名单");
        }
        if (!versionOperations.containsAll(grantOperations)) {
            throw new BusinessException("授权操作不能超出流程能力版本操作白名单");
        }
        if (grantOperations.contains("SUBMIT")) {
            Set<String> versionFields = readAllowedFields(versionPolicy);
            Set<String> versionRequiredFields = readTextSet(versionPolicy.path("requiredFields"));
            Set<String> grantFields = readAllowedFields(grantPolicy);
            if (versionFields.isEmpty() || grantFields.isEmpty()) {
                throw new BusinessException("提交业务申请授权必须配置非空字段白名单");
            }
            if (!versionFields.containsAll(grantFields)) {
                throw new BusinessException("授权字段不能超出申请能力版本字段白名单");
            }
            if (!grantFields.containsAll(versionRequiredFields)) {
                throw new BusinessException("授权字段必须包含申请能力版本的全部必填字段");
            }
        }
        if (!"MCP_ELICITATION".equals(versionPolicy.path("confirmationMode").asText())) {
            throw new BusinessException("受控流程动作缺少 MCP elicitation 确认策略");
        }
    }

    private void validateSystemServicePolicy(JsonNode grantPolicy) {
        if (grantPolicy != null && (!grantPolicy.isObject() || !grantPolicy.isEmpty())) {
            throw new BusinessException("系统服务不接受客户端自定义字段或操作策略");
        }
    }

    private JsonNode readJson(String json, String message) {
        try {
            return objectMapper.readTree(json);
        }
        catch (Exception exception) {
            throw new BusinessException(message);
        }
    }

    private Set<String> readAllowedFields(JsonNode policy) {
        Set<String> result = new LinkedHashSet<>();
        JsonNode fields = policy == null ? null : policy.path("allowedFields");
        if (fields != null && fields.isArray()) {
            fields.forEach(item -> {
                if (item.isTextual() && !item.asText().isBlank()) {
                    result.add(item.asText());
                }
            });
        }
        return result;
    }

    private Set<String> readTextSet(JsonNode values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null && values.isArray()) {
            values.forEach(item -> {
                if (item.isTextual() && !item.asText().isBlank()) {
                    result.add(item.asText());
                }
            });
        }
        return result;
    }

    private Long requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }
}
