package com.mdframe.forge.plugin.capability.highrisk.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityApproval;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityApprovalMapper;
import com.mdframe.forge.plugin.capability.highrisk.service.HighRiskApprovalSubmissionService;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HighRiskApprovalCodeFormProviderTest {

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldBuildReadonlyContextAndMaskSecretLikeFields() {
        CapabilityApprovalMapper mapper = mock(CapabilityApprovalMapper.class);
        HighRiskApprovalSubmissionService submission = mock(HighRiskApprovalSubmissionService.class);
        AiCapabilityApproval approval = new AiCapabilityApproval();
        approval.setId(99L);
        approval.setCapabilityCode("business.order.confirm");
        approval.setCapabilityVersion("1.0.0");
        approval.setActorUserId(10L);
        approval.setCreateTime(LocalDateTime.now());
        approval.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(mapper.selectTenantById(1L, 99L)).thenReturn(approval);
        when(submission.decryptAndVerify(approval)).thenReturn(Map.of(
                "recordId", "1001",
                "arguments", Map.of("status", "CONFIRMED", "apiToken", "plain-secret")));
        TenantContextHolder.setTenantId(1L);
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setBusinessKey("capability-approval:99");

        var context = new HighRiskApprovalCodeFormProvider(
                mapper, submission, new ObjectMapper()).buildContext(query, Map.of(), null);

        assertThat(context.getEditMode()).isEqualTo("readonly");
        assertThat(context.getFieldPermissions())
                .allSatisfy(permission -> assertThat(permission).containsEntry("writable", false));
        assertThat(String.valueOf(context.getRecordData().get("changeSummary")))
                .contains("CONFIRMED", "[已隐藏]")
                .doesNotContain("plain-secret");
        assertThat(context.getRecordData()).doesNotContainKeys(
                "keyId", "wrappedDek", "payloadCiphertext", "processInstanceId");
    }
}
