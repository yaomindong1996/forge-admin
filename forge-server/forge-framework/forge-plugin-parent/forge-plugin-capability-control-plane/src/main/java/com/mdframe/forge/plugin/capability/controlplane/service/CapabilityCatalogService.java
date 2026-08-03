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
import com.mdframe.forge.plugin.capability.controlplane.mapper.model.CapabilityGrantOptionRow;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityGrantCapabilityVO;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityVersionDraftVO;
import com.mdframe.forge.plugin.capability.naming.CapabilityToolNameMapper;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class CapabilityCatalogService {

    private static final Set<String> BEHAVIORS = Set.of(
            "READ_ONLY", "ACTION", "FLOW", "MESSAGE", "EXTERNAL");
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> VISIBILITIES = Set.of("PRIVATE", "DISCOVERABLE");
    private static final Set<String> ACTOR_TYPES = Set.of("SERVICE", "USER", "BOTH");

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

    public CapabilityVersionDraftVO versionDraft(Long tenantId, Long id) {
        Long safeTenantId = requireTenant(tenantId);
        AiCapability capability = getById(safeTenantId, id);
        String currentVersion = capability.getCurrentVersion();
        if (currentVersion == null || currentVersion.isBlank()) {
            throw new BusinessException("能力没有可升级的当前版本");
        }
        AiCapabilityVersion version = versionMapper.selectVersion(
                safeTenantId, capability.getId(), currentVersion);
        if (version == null || !"PUBLISHED".equals(version.getStatus())) {
            throw new BusinessException("当前能力版本不存在或未发布，无法创建新版本");
        }
        return new CapabilityVersionDraftVO(
                capability.getId(), capability.getCapabilityCode(), version.getSourceType(),
                version.getSourceKey(), version.getSourceVersion(), currentVersion,
                nextPatchVersion(currentVersion), capability.getDescription(),
                readRequiredJson(version.getInputSchema(), "当前能力入参契约损坏，无法创建新版本"),
                readOptionalObject(version.getPolicySnapshot()));
    }

    public List<CapabilityGrantCapabilityVO> listGrantOptions(Long tenantId) {
        return capabilityMapper.selectGrantOptions(requireTenant(tenantId)).stream()
                .map(this::toGrantOption)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long tenantId, CapabilityPublishDTO dto) {
        if ("BUSINESS_ACTION".equals(dto.sourceType()) || "FLOW_ACTION".equals(dto.sourceType())
                || "SYSTEM_SERVICE".equals(dto.sourceType())) {
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

    @Transactional(rollbackFor = Exception.class)
    public Long publishSystemService(Long tenantId, CapabilityPublishDTO dto) {
        if (!"SYSTEM_SERVICE".equals(dto.sourceType())
                || !"ACTION".equals(dto.behavior())
                || !"MEDIUM".equals(dto.riskLevel())) {
            throw new BusinessException("受控系统服务能力元数据无效");
        }
        return publishInternal(tenantId, dto);
    }

    private Long publishInternal(Long tenantId, CapabilityPublishDTO dto) {
        Long safeTenantId = requireTenant(tenantId);
        validateEnums(dto);
        String requiredActorType = resolveRequiredActorType(dto);
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
        boolean newCapability = capability == null;
        if (newCapability) {
            capability = new AiCapability();
            capability.setTenantId(safeTenantId);
            capability.setCapabilityCode(dto.capabilityCode());
            capability.setProtocolToolName(dto.protocolToolName());
            capability.setDelFlag(0L);
        }
        else {
            assertStableCapabilityIdentity(capability, dto);
        }

        AiCapabilityVersion existingVersion = newCapability ? null : versionMapper.selectVersion(
                safeTenantId, capability.getId(), dto.version());
        assertImmutableVersion(existingVersion, dto, checksum, requiredActorType);
        if (!newCapability && existingVersion == null) {
            assertVersionIncreases(capability.getCurrentVersion(), dto.version());
        }

        applyMetadata(capability, dto, checksum);
        capability.setRequiredActorType(requiredActorType);
        if (newCapability) {
            capabilityMapper.insert(capability);
        }
        if (existingVersion == null) {
            AiCapabilityVersion version = buildVersion(
                    safeTenantId, capability.getId(), dto, checksum, inputSchema, outputSchema);
            version.setRequiredActorType(requiredActorType);
            versionMapper.insert(version);
        }
        if (!newCapability) {
            capabilityMapper.updateById(capability);
        }
        return capability.getId();
    }

    public void disable(Long tenantId, Long id) {
        AiCapability capability = getById(tenantId, id);
        capability.setEnabled(0);
        capability.setPublishStatus("DISABLED");
        capabilityMapper.updateById(capability);
    }

    public void enable(Long tenantId, Long id) {
        Long safeTenantId = requireTenant(tenantId);
        AiCapability capability = getById(safeTenantId, id);
        String currentVersion = capability.getCurrentVersion();
        if (currentVersion == null || currentVersion.isBlank()) {
            throw new BusinessException("能力没有可启用的当前版本，请先发布能力版本");
        }
        AiCapabilityVersion version = versionMapper.selectVersion(
                safeTenantId, capability.getId(), currentVersion);
        if (version == null || !"PUBLISHED".equals(version.getStatus())) {
            throw new BusinessException("当前能力版本不存在或未发布，无法重新启用");
        }
        capability.setEnabled(1);
        capability.setPublishStatus("PUBLISHED");
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
        version.setDelFlag(0L);
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

    private CapabilityGrantCapabilityVO toGrantOption(CapabilityGrantOptionRow row) {
        JsonNode policy = readPolicy(row.getPolicySnapshot());
        return new CapabilityGrantCapabilityVO(
                row.getId(), row.getCapabilityCode(), row.getCapabilityName(),
                row.getCurrentVersion(), row.getSourceType(), row.getBehavior(),
                row.getRiskLevel(), row.getRequiredActorType(), row.getPublishStatus(),
                row.getEnabled(), textValues(policy.path("allowedFields")),
                textValues(policy.path("requiredFields")),
                textValues(policy.path("allowedOperations")));
    }

    private JsonNode readPolicy(String policySnapshot) {
        if (policySnapshot == null || policySnapshot.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode policy = objectMapper.readTree(policySnapshot);
            return policy != null && policy.isObject()
                    ? policy : objectMapper.createObjectNode();
        }
        catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode();
        }
    }

    private List<String> textValues(JsonNode values) {
        if (values == null || !values.isArray()) {
            return List.of();
        }
        return StreamSupport.stream(values.spliterator(), false)
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
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

    private String resolveRequiredActorType(CapabilityPublishDTO dto) {
        boolean flowAction = "FLOW_ACTION".equals(dto.sourceType());
        String actorType = dto.requiredActorType();
        if (actorType == null || actorType.isBlank()) {
            return flowAction ? "USER" : "SERVICE";
        }
        String normalized = actorType.trim().toUpperCase();
        if (!ACTOR_TYPES.contains(normalized)) {
            throw new BusinessException("不支持的能力调用身份类型");
        }
        if (flowAction && !"USER".equals(normalized)) {
            throw new BusinessException("受控流程动作能力仅支持用户委托身份调用");
        }
        return normalized;
    }

    private void assertImmutableVersion(
            AiCapabilityVersion existingVersion,
            CapabilityPublishDTO dto,
            String checksum,
            String requiredActorType) {
        if (existingVersion == null) {
            return;
        }
        if (!checksum.equals(existingVersion.getSchemaChecksum())
                || !dto.sourceType().equals(existingVersion.getSourceType())
                || !dto.sourceKey().equals(existingVersion.getSourceKey())
                || !dto.sourceVersion().equals(existingVersion.getSourceVersion())
                || !dto.behavior().equals(existingVersion.getBehavior())
                || !dto.riskLevel().equals(existingVersion.getRiskLevel())
                || !requiredActorType.equals(existingVersion.getRequiredActorType())
                || !dto.visibility().equals(existingVersion.getVisibility())) {
            throw new BusinessException("已发布能力版本不可修改，请创建新版本");
        }
    }

    private void assertStableCapabilityIdentity(AiCapability capability, CapabilityPublishDTO dto) {
        if (!Objects.equals(capability.getSourceType(), dto.sourceType())
                || !Objects.equals(capability.getSourceKey(), dto.sourceKey())) {
            throw new BusinessException("已注册能力来源不可修改，请注册新的能力编码");
        }
    }

    private void assertVersionIncreases(String currentVersion, String requestedVersion) {
        if (currentVersion == null || currentVersion.isBlank()) {
            return;
        }
        if (compareSemanticVersions(requestedVersion, currentVersion) <= 0) {
            throw new BusinessException("新版本必须高于当前版本 " + currentVersion
                    + "，建议使用 " + nextPatchVersion(currentVersion));
        }
    }

    private int compareSemanticVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        for (int index = 0; index < 3; index++) {
            int compared = new BigInteger(leftParts[index]).compareTo(new BigInteger(rightParts[index]));
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private String nextPatchVersion(String version) {
        if (version == null || !version.matches("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")) {
            throw new BusinessException("当前能力版本格式无效，无法生成下一版本");
        }
        String[] parts = version.split("\\.");
        return parts[0] + "." + parts[1] + "."
                + new BigInteger(parts[2]).add(BigInteger.ONE);
    }

    private JsonNode readRequiredJson(String json, String message) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(message);
        }
        try {
            return objectMapper.readTree(json);
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException(message);
        }
    }

    private JsonNode readOptionalObject(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        JsonNode value = readRequiredJson(json, "当前能力策略快照损坏，无法创建新版本");
        if (!value.isObject()) {
            throw new BusinessException("当前能力策略快照损坏，无法创建新版本");
        }
        return value;
    }

    private Long requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }
}
