package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectDesignVersionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessObjectActionServicePublishedTest {

    private final BusinessObjectMapper objectMapper = mock(BusinessObjectMapper.class);
    private final BusinessObjectDesignVersionMapper versionMapper =
            mock(BusinessObjectDesignVersionMapper.class);
    private final BusinessObjectActionService service = new BusinessObjectActionService(
            new ObjectMapper(),
            mock(BusinessObjectDesignerService.class),
            mock(BusinessBindingMapper.class),
            objectMapper,
            versionMapper,
            mock(BusinessPermissionService.class));

    @AfterEach
    void clearIdentity() {
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldResolveActionFromPublishedSnapshotInsteadOfDraftOptions() {
        openIdentity();
        AiBusinessObject object = object();
        object.setDesignerOptions(actions("草稿动作"));
        AiBusinessObjectDesignVersion version = version(3, "已发布动作");
        when(objectMapper.selectByObjectCode(1L, "purchase", "order")).thenReturn(object);
        when(versionMapper.selectPublishedVersion(1L, 10L, null)).thenReturn(version);

        var resolved = service.resolvePublishedAction("purchase", "order", "confirm", null);

        assertThat(resolved.action().getActionName()).isEqualTo("已发布动作");
        assertThat(resolved.version().getPublishVersion()).isEqualTo(3);
    }

    @Test
    void shouldRequestTheSpecifiedPublishedVersion() {
        openIdentity();
        AiBusinessObject object = object();
        when(objectMapper.selectByObjectCode(1L, "purchase", "order")).thenReturn(object);
        when(versionMapper.selectPublishedVersion(1L, 10L, 2))
                .thenReturn(version(2, "版本二动作"));

        var resolved = service.resolvePublishedAction("purchase", "order", "confirm", 2);

        assertThat(resolved.version().getPublishVersion()).isEqualTo(2);
        verify(versionMapper).selectPublishedVersion(1L, 10L, 2);
    }

    @Test
    void shouldFailClosedWithoutTrustedTenant() {
        ExecutionIdentityContextHolder.clear();

        assertThatThrownBy(() -> service.resolvePublishedAction(
                "purchase", "order", "confirm", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("租户上下文");
        verify(objectMapper, never()).selectByObjectCode(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private void openIdentity() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        ExecutionIdentityContextHolder.open(new ExecutionIdentity(
                user, "USER", 101L, 999L, 301L,
                "agent_client", "token-1", Set.of("capability.invoke")));
    }

    private AiBusinessObject object() {
        AiBusinessObject object = new AiBusinessObject();
        object.setId(10L);
        object.setTenantId(1L);
        object.setSuiteCode("purchase");
        object.setObjectCode("order");
        object.setObjectName("订单");
        object.setStatus(1);
        return object;
    }

    private AiBusinessObjectDesignVersion version(int publishVersion, String actionName) {
        AiBusinessObjectDesignVersion version = new AiBusinessObjectDesignVersion();
        version.setPublishVersion(publishVersion);
        version.setDesignerOptionsSnapshot(actions(actionName));
        return version;
    }

    private String actions(String actionName) {
        return "{\"actions\":[{\"actionCode\":\"confirm\",\"actionName\":\""
                + actionName + "\",\"status\":1,\"actionConfig\":{\"steps\":[]}}]}";
    }
}
