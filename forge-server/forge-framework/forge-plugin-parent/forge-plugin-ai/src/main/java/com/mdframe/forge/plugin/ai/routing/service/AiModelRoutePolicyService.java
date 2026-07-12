package com.mdframe.forge.plugin.ai.routing.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelCapabilityCode;
import com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy;
import com.mdframe.forge.plugin.ai.routing.domain.AiModelRouteTarget;
import com.mdframe.forge.plugin.ai.routing.dto.AiModelRoutePolicySaveDTO;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRoutePolicyMapper;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRouteTargetMapper;
import com.mdframe.forge.plugin.ai.routing.vo.AiModelRoutePolicyVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiModelRoutePolicyService {
    private final AiModelRoutePolicyMapper policyMapper;
    private final AiModelRouteTargetMapper targetMapper;
    private final AiModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public Page<AiModelRoutePolicyVO> pagePolicy(int pageNum, int pageSize, String keyword, String status) {
        Page<AiModelRoutePolicy> page = policyMapper.selectPolicyPage(new Page<>(pageNum, pageSize), keyword, status);
        Page<AiModelRoutePolicyVO> result = new Page<>(pageNum, pageSize, page.getTotal());
        List<Long> policyIds = page.getRecords().stream().map(AiModelRoutePolicy::getId).toList();
        Map<Long, List<AiModelRoutePolicyVO.Target>> targetsByPolicyId = policyIds.isEmpty()
                ? Collections.emptyMap()
                : targetMapper.selectByPolicyIds(policyIds).stream()
                        .collect(Collectors.groupingBy(AiModelRoutePolicyVO.Target::getPolicyId));
        result.setRecords(page.getRecords().stream()
                .map(policy -> toView(policy, targetsByPolicyId.getOrDefault(policy.getId(), List.of())))
                .toList());
        return result;
    }

    public AiModelRoutePolicyVO getPolicy(Long id) {
        AiModelRoutePolicy policy = policyMapper.selectById(id);
        if (policy == null) throw new BusinessException("模型路由策略不存在");
        return toView(policy, targetMapper.selectByPolicyId(policy.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createPolicy(AiModelRoutePolicySaveDTO dto) {
        validate(dto, null);
        AiModelRoutePolicy policy = toEntity(dto);
        policyMapper.insert(policy);
        saveTargets(policy.getId(), dto.getTargets());
        return policy.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePolicy(AiModelRoutePolicySaveDTO dto) {
        if (dto == null || dto.getId() == null || policyMapper.selectById(dto.getId()) == null) throw new BusinessException("模型路由策略不存在");
        validate(dto, dto.getId());
        AiModelRoutePolicy policy = toEntity(dto);
        policyMapper.updateById(policy);
        targetMapper.logicallyDeleteByPolicyId(dto.getId());
        saveTargets(dto.getId(), dto.getTargets());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePolicy(Long id) {
        if (policyMapper.countPolicyAgents(id) > 0) throw new BusinessException("路由策略正在被 Agent 使用，不能删除");
        targetMapper.logicallyDeleteByPolicyId(id);
        policyMapper.deleteById(id);
    }

    private void validate(AiModelRoutePolicySaveDTO dto, Long excludeId) {
        if (dto == null || !StringUtils.hasText(dto.getPolicyCode()) || !StringUtils.hasText(dto.getPolicyName())) throw new BusinessException("策略编码和名称不能为空");
        if (policyMapper.countActiveCode(dto.getPolicyCode().trim(), excludeId) > 0) throw new BusinessException("策略编码已存在");
        if (dto.getTargets() == null || dto.getTargets().isEmpty()) throw new BusinessException("路由策略至少配置一个候选模型");
        if (dto.getRequiredCapabilities() != null) dto.getRequiredCapabilities().forEach(AiModelCapabilityCode::require);
        List<Long> ids = dto.getTargets().stream().map(AiModelRoutePolicySaveDTO.Target::getModelId).toList();
        if (ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size()) throw new BusinessException("候选模型不能为空或重复");
        List<AiModel> models = modelMapper.selectEnabledByIds(ids);
        if (models == null || models.size() != ids.size()) throw new BusinessException("候选模型不存在、已停用或不属于当前租户");
    }

    private AiModelRoutePolicy toEntity(AiModelRoutePolicySaveDTO dto) {
        AiModelRoutePolicy policy = new AiModelRoutePolicy();
        policy.setId(dto.getId()); policy.setPolicyCode(dto.getPolicyCode().trim()); policy.setPolicyName(dto.getPolicyName().trim());
        policy.setRequiredCapabilities(writeJson(dto.getRequiredCapabilities() == null ? List.of() : dto.getRequiredCapabilities().stream().distinct().toList()));
        policy.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "0"); policy.setRemark(dto.getRemark()); return policy;
    }
    private void saveTargets(Long policyId, List<AiModelRoutePolicySaveDTO.Target> targets) {
        for (AiModelRoutePolicySaveDTO.Target dto : targets) { AiModelRouteTarget target = new AiModelRouteTarget(); target.setPolicyId(policyId); target.setModelId(dto.getModelId()); target.setPriority(dto.getPriority() == null ? 100 : dto.getPriority()); target.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "0"); targetMapper.insert(target); }
    }
    private AiModelRoutePolicyVO toView(AiModelRoutePolicy policy, List<AiModelRoutePolicyVO.Target> targets) {
        AiModelRoutePolicyVO vo = new AiModelRoutePolicyVO();
        vo.setId(policy.getId());
        vo.setPolicyCode(policy.getPolicyCode());
        vo.setPolicyName(policy.getPolicyName());
        vo.setRequiredCapabilities(readJson(policy.getRequiredCapabilities()));
        vo.setStatus(policy.getStatus());
        vo.setRemark(policy.getRemark());
        vo.setCreateTime(policy.getCreateTime());
        vo.setTargets(targets);
        return vo;
    }
    private String writeJson(List<String> values) { try { return objectMapper.writeValueAsString(values); } catch (JsonProcessingException e) { throw new BusinessException("策略能力配置序列化失败"); } }
    private List<String> readJson(String value) { if (!StringUtils.hasText(value)) return List.of(); try { return objectMapper.readValue(value, new TypeReference<>() {}); } catch (Exception e) { return List.of(); } }
}
