package com.mdframe.forge.plugin.capability.highrisk.mcp;

import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityApproval;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityApprovalMapper;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HighRiskApprovalQueryServiceTest {

    @Test
    void shouldQueryOnlyWithFullOwnershipTuple() {
        CapabilityApprovalMapper mapper = mock(CapabilityApprovalMapper.class);
        AiCapabilityApproval approval = approval();
        when(mapper.selectOwned(1L, 99L, 30L, 10L, 20L, 40L)).thenReturn(approval);

        try (var ignored = ExecutionIdentityContextHolder.open(identity(true))) {
            var result = new HighRiskApprovalQueryService(mapper).get(99L);
            assertThat(result).containsEntry("approvalRequestId", "99")
                    .containsEntry("status", "PENDING_APPROVAL")
                    .doesNotContainKeys("payloadCiphertext", "keyId", "processInstanceId");
        }
        verify(mapper).selectOwned(1L, 99L, 30L, 10L, 20L, 40L);
    }

    @Test
    void shouldFailClosedWithoutQueryPermission() {
        CapabilityApprovalMapper mapper = mock(CapabilityApprovalMapper.class);
        try (var ignored = ExecutionIdentityContextHolder.open(identity(false))) {
            assertThatThrownBy(() -> new HighRiskApprovalQueryService(mapper).get(99L))
                    .hasMessage("FORBIDDEN");
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldHideExistenceWhenOwnershipDoesNotMatch() {
        CapabilityApprovalMapper mapper = mock(CapabilityApprovalMapper.class);
        try (var ignored = ExecutionIdentityContextHolder.open(identity(true))) {
            assertThatThrownBy(() -> new HighRiskApprovalQueryService(mapper).get(99L))
                    .hasMessage("APPROVAL_NOT_FOUND");
        }
    }

    private AiCapabilityApproval approval() {
        AiCapabilityApproval approval = new AiCapabilityApproval();
        approval.setId(99L);
        approval.setCapabilityCode("business.order.confirm");
        approval.setCapabilityVersion("1.0.0");
        approval.setExecuteStatus("PENDING_APPROVAL");
        approval.setResultCode("PENDING_APPROVAL");
        approval.setRequestId("request-1");
        approval.setCreateTime(LocalDateTime.now());
        approval.setExpiresAt(LocalDateTime.now().plusHours(1));
        return approval;
    }

    private ExecutionIdentity identity(boolean permitted) {
        LoginUser user = new LoginUser();
        user.setUserId(10L);
        user.setTenantId(1L);
        user.setActiveOrgId(40L);
        user.setPermissions(permitted ? Set.of("ai:capability:approval:query") : Set.of());
        return new ExecutionIdentity(user, "USER", 10L, 20L, 30L,
                "client-a", "token-1", Set.of());
    }
}
