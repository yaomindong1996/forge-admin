package com.mdframe.forge.plugin.ai.routing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy;
import com.mdframe.forge.plugin.ai.routing.domain.AiModelRouteTarget;
import com.mdframe.forge.plugin.ai.routing.dto.AiModelRoutePolicySaveDTO;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRoutePolicyMapper;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRouteTargetMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiModelRoutePolicyServiceTest {

    @Mock
    private AiModelRoutePolicyMapper policyMapper;

    @Mock
    private AiModelRouteTargetMapper targetMapper;

    @Mock
    private AiModelMapper modelMapper;

    private AiModelRoutePolicyService service;

    @BeforeEach
    void setUp() {
        service = new AiModelRoutePolicyService(
                policyMapper, targetMapper, modelMapper, new ObjectMapper());
    }

    @Test
    void createShouldPersistExplicitValidCandidates() {
        AiModelRoutePolicySaveDTO dto = policy(10L, 20L);
        when(modelMapper.selectEnabledByIds(List.of(10L, 20L)))
                .thenReturn(List.of(model(10L), model(20L)));
        when(policyMapper.insert(any(AiModelRoutePolicy.class))).thenAnswer(invocation -> {
            invocation.<AiModelRoutePolicy>getArgument(0).setId(30L);
            return 1;
        });

        service.createPolicy(dto);

        verify(targetMapper, org.mockito.Mockito.times(2)).insert(any(AiModelRouteTarget.class));
    }

    @Test
    void createShouldRejectEmptyOrDuplicateCandidatesBeforeWriting() {
        AiModelRoutePolicySaveDTO empty = policy();
        assertThrows(BusinessException.class, () -> service.createPolicy(empty));

        AiModelRoutePolicySaveDTO duplicate = policy(10L, 10L);
        assertThrows(BusinessException.class, () -> service.createPolicy(duplicate));

        verify(policyMapper, never()).insert(any(AiModelRoutePolicy.class));
        verify(modelMapper, never()).selectEnabledByIds(anyList());
    }

    @Test
    void createShouldRejectUnavailableOrCrossTenantCandidate() {
        AiModelRoutePolicySaveDTO dto = policy(10L, 20L);
        when(modelMapper.selectEnabledByIds(List.of(10L, 20L)))
                .thenReturn(List.of(model(10L)));

        assertThrows(BusinessException.class, () -> service.createPolicy(dto));

        verify(policyMapper, never()).insert(any(AiModelRoutePolicy.class));
    }

    @Test
    void deleteShouldFailWhenPolicyIsUsedByAgent() {
        when(policyMapper.countPolicyAgents(30L)).thenReturn(1);

        assertThrows(BusinessException.class, () -> service.deletePolicy(30L));

        verify(targetMapper, never()).logicallyDeleteByPolicyId(any());
        verify(policyMapper, never()).deleteById(any());
    }

    private AiModelRoutePolicySaveDTO policy(Long... modelIds) {
        AiModelRoutePolicySaveDTO dto = new AiModelRoutePolicySaveDTO();
        dto.setPolicyCode("primary_route");
        dto.setPolicyName("主路由");
        dto.setRequiredCapabilities(List.of("reasoning"));
        dto.setStatus("0");
        dto.setTargets(java.util.Arrays.stream(modelIds).map(id -> {
            AiModelRoutePolicySaveDTO.Target target = new AiModelRoutePolicySaveDTO.Target();
            target.setModelId(id);
            target.setPriority(100);
            target.setStatus("0");
            return target;
        }).toList());
        return dto;
    }

    private AiModel model(Long id) {
        AiModel model = new AiModel();
        model.setId(id);
        model.setTenantId(1L);
        model.setStatus("0");
        return model;
    }
}
