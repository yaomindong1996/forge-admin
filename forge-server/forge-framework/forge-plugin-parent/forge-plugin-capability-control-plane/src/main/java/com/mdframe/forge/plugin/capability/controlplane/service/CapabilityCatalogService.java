package com.mdframe.forge.plugin.capability.controlplane.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityVersion;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityMapper;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityVersionMapper;
import com.mdframe.forge.plugin.capability.naming.CapabilityToolNameMapper;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CapabilityCatalogService {

    private static final Set<String> BEHAVIORS = Set.of(
            "READ_ONLY", "ACTION", "FLOW", "MESSAGE", "EXTERNAL");
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> VISIBILITIES = Set.of("PRIVATE", "DISCOVERABLE");

    private final AiCapabilityMapper capabilityMapper;
    private final AiCapabilityVersionMapper versionMapper;
    private final CapabilityToolNameMapper toolNameMapper;
    private final CapabilitySchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;

    public Page<AiCapability> page(
            Long tenantId,
            PageQuery pageQuery,
            String keyword,
            String publishStatus) {
        return capabilityMapper.selectPage(
                pageQuery.toPage(), requireTenant(tenantId), keyword, publishStatus);
    }

    public AiCapability getById(Long tenantId, Long id) {
        AiCapability capability = capabilityMapper.selectTenantById(requireTenant(tenantId), id);
        if (capability == null) {
            throw new BusinessException("能力不存在或无权访问");
        }
        return capability;
    }

    public AiCapability getByCode(Long tenantId, String capabilityCode) {
        return capabilityMapper.selectByCode(requireTenant(tenantId), capabilityCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long tenantId, CapabilityPublishDTO dto) {
        if ("BUSINESS_ACTION".equals(dto.sourceType()) || "FLOW_ACTION".equals(dto.sourceType())) {
            throw new BusinessException(dto.sourceType() + " 必须通过对应的受控能力发布接口创建");
        }
        return publishInternal(tenantId, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publishBusinessAction(Long tenantId, CapabilityPublishDTO dto) {
        if (!"BUSINESS_ACTION".equals(dto.sourceType())
                || !"ACTION".equals(dto.behavior())
                || !"MEDIUM".equals(dto.riskLevel())) {
            throw new BusinessException("受控业务动作能力元数据无效");
        }
        return publishInternal(tenantId, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publishHighRiskBusinessAction(Long tenantId, CapabilityPublishDTO dto) {
        if (!"BUSINESS_ACTION".equals(dto.sourceType())
                || !"ACTION".equals(dto.behavior())
                || !"HIGH".equals(dto.riskLevel())) {
            throw new BusinessException("高风险业务动作能力元数据无效");
        }
        return publishInternal(tenantId, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publishFlowAction(Long tenantId, CapabilityPublishDTO dto) {
        if (!"FLOW_ACTION".equals(dto.sourceType())
                || !"FLOW".equals(dto.behavior())
                || !"MEDIUM".equals(dto.riskLevel())) {
            throw new BusinessException("受控流程动作能力元数据无效");
        }
        return publishInternal(tenantId, dto);
    }

    private Long publishInternal(Long tenantId, CapabilityPublishDTO dto) {
        Long safeTenantId = requireTenant(tenantId);
        validateEnums(dto);
        String expectedToolName = toolNameMapper.toProtocolToolName(dto.capabilityCode());
        if (!expectedToolName.equals(dto.protocolToolName())) {
            throw new BusinessException("阶段 1 protocolToolName 必须与 capabilityCode 保持一致");
        }
        JsonNode inputSchema = schemaValidator.validateDefinition(dto.inputSchema()).schema();
        JsonNode outputSchema = schemaValidator.validateDefinition(dto.outputSchema()).schema();
        CapabilityVersionFingerprint fingerprint = CapabilityVersionFingerprint.from(dto);
        String checksum = new CapabilitySchemaChecksum(objectMapper)
                .calculate(inputSchema, outputSchema, dto.policySnapshot(), fingerprint);

        AiCapability capability = capabilityMapper.selectByCode(safeTenantId, dto.capabilityCode());
        AiCapability toolOwner = capabilityMapper.selectByToolName(safeTenantId, dto.protocolToolName());
        if (toolOwner != null && (capability == null || !toolOwner.getId().equals(capability.getId()))) {
            throw new BusinessException("协议工具名已被其它能力占用");
        }
        if (capability == null) {
            capability = new AiCapability();
            capability.setTenantId(safeTenantId);
            capability.setCapabilityCode(dto.capabilityCode());
            capability.setProtocolToolName(dto.protocolToolName());
            capability.setDelFlag(0);
        }
        applyMetadata(capability, dto, checksum);
        if (capability.getId() == null) {
            capabilityMapper.insert(capability);
        }

        AiCapabilityVersion existingVersion = versionMapper.selectVersion(
                safeTenantId, capability.getId(), dto.version());
        assertImmutableVersion(existingVersion, dto, checksum);
        if (existingVersion == null) {
            versionMapper.insert(buildVersion(
                    safeTenantId, capability.getId(), dto, checksum, inputSchema, outputSchema));
        }
        capabilityMapper.updateById(capability);
        return capability.getId();
    }

    public void disable(Long tenantId, Long id) {
        AiCapability capability = getById(tenantId, id);
        capability.setEnabled(0);
        capability.setPublishStatus("DISABLED");
        capabilityMapper.updateById(capability);
    }

    private void applyMetadata(AiCapability capability, CapabilityPublishDTO dto, String checksum) {
        capability.setCapabilityName(dto.capabilityName());
        capability.setDescription(dto.description());
        capability.setSourceType(dto.sourceType());
        capability.setSourceKey(dto.sourceKey());
        capability.setSourceVersion(dto.sourceVersion());
        capability.setCurrentVersion(dto.version());
        capability.setSchemaChecksum(checksum);
        capability.setBehavior(dto.behavior());
        capability.setRiskLevel(dto.riskLevel());
        capability.setVisibility(dto.visibility());
        capability.setPublishStatus("PUBLISHED");
        capability.setEnabled(1);
    }

    private AiCapabilityVersion buildVersion(
            Long tenantId,
            Long capabilityId,
            CapabilityPublishDTO dto,
            String checksum,
            JsonNode inputSchema,
            JsonNode outputSchema) {
        AiCapabilityVersion version = new AiCapabilityVersion();
        version.setTenantId(tenantId);
        version.setCapabilityId(capabilityId);
        version.setVersion(dto.version());
        version.setInputSchema(writeJson(inputSchema));
        version.setOutputSchema(writeJson(outputSchema));
        version.setSourceType(dto.sourceType());
        version.setSourceKey(dto.sourceKey());
        version.setSourceVersion(dto.sourceVersion());
        version.setBehavior(dto.behavior());
        version.setRiskLevel(dto.riskLevel());
        version.setVisibility(dto.visibility());
        version.setPolicySnapshot(dto.policySnapshot() == null ? null : writeJson(dto.policySnapshot()));
        version.setSchemaChecksum(checksum);
        version.setStatus("PUBLISHED");
        version.setDelFlag(0);
        return version;
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException("能力 Schema 无法序列化");
        }
    }

    private void validateEnums(CapabilityPublishDTO dto) {
        if (!BEHAVIORS.contains(dto.behavior())) {
            throw new BusinessException("不支持的能力行为");
        }
        if (!RISK_LEVELS.contains(dto.riskLevel())) {
            throw new BusinessException("不支持的能力风险等级");
        }
        if (!VISIBILITIES.contains(dto.visibility())) {
            throw new BusinessException("不支持的能力可见性");
        }
        if (!dto.version().matches("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")) {
            throw new BusinessException("能力版本必须使用三段语义版本");
        }
    }

    private void assertImmutableVersion(
            AiCapabilityVersion existingVersion,
            CapabilityPublishDTO dto,
            String checksum) {
        if (existingVersion == null) {
            return;
        }
        if (!checksum.equals(existingVersion.getSchemaChecksum())
                || !dto.sourceType().equals(existingVersion.getSourceType())
                || !dto.sourceKey().equals(existingVersion.getSourceKey())
                || !dto.sourceVersion().equals(existingVersion.getSourceVersion())
                || !dto.behavior().equals(existingVersion.getBehavior())
                || !dto.riskLevel().equals(existingVersion.getRiskLevel())
                || !dto.visibility().equals(existingVersion.getVisibility())) {
            throw new BusinessException("已发布能力版本不可修改，请创建新版本");
        }
    }

    private Long requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }
}
