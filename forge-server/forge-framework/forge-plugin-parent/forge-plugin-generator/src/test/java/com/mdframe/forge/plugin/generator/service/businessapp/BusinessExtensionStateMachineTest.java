package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessExtensionStateMachine")
class BusinessExtensionStateMachineTest {

    private final BusinessExtensionStateMachine stateMachine = new BusinessExtensionStateMachine();

    @Test
    @DisplayName("untested draft cannot be enabled")
    void untestedDraftCannotBeEnabled() {
        AiBusinessExtension extension = extension(BusinessExtensionStatus.DRAFT);
        AiBusinessExtensionVersion version = version(true, false);

        BusinessException error = assertThrows(BusinessException.class,
                () -> stateMachine.assertCanEnable(extension, version));

        assertTrue(error.getMessage().contains("测试"));
    }

    @Test
    @DisplayName("tested draft can be enabled")
    void testedDraftCanBeEnabled() {
        AiBusinessExtension extension = extension(BusinessExtensionStatus.TESTED);
        AiBusinessExtensionVersion version = version(true, true);

        stateMachine.assertCanEnable(extension, version);
    }

    @Test
    @DisplayName("content changes always return governance state to draft")
    void contentChangeReturnsToDraft() {
        assertEquals(BusinessExtensionStatus.DRAFT,
                stateMachine.statusAfterContentChange(BusinessExtensionStatus.ENABLED));
        assertEquals(BusinessExtensionStatus.DRAFT,
                stateMachine.statusAfterContentChange(BusinessExtensionStatus.TESTED));
    }

    @Test
    @DisplayName("high risk before hook only permits block")
    void highRiskBeforeHookOnlyPermitsBlock() {
        assertThrows(BusinessException.class,
                () -> stateMachine.validateFailurePolicy("BEFORE_UPDATE", "HIGH", "WARN"));
        stateMachine.validateFailurePolicy("BEFORE_UPDATE", "HIGH", "BLOCK");
    }

    @Test
    @DisplayName("ignore is limited to low risk after hooks")
    void ignoreIsLimitedToLowRiskAfterHooks() {
        assertThrows(BusinessException.class,
                () -> stateMachine.validateFailurePolicy("BEFORE_LIST", "LOW", "IGNORE"));
        assertThrows(BusinessException.class,
                () -> stateMachine.validateFailurePolicy("AFTER_UPDATE", "MEDIUM", "IGNORE"));
        stateMachine.validateFailurePolicy("AFTER_UPDATE", "LOW", "IGNORE");
    }

    private AiBusinessExtension extension(String status) {
        AiBusinessExtension extension = new AiBusinessExtension();
        extension.setStatus(status);
        extension.setDraftVersion(2);
        return extension;
    }

    private AiBusinessExtensionVersion version(boolean validated, boolean tested) {
        AiBusinessExtensionVersion version = new AiBusinessExtensionVersion();
        version.setVersionNo(2);
        version.setValidationPassed(validated ? 1 : 0);
        version.setTestPassed(tested ? 1 : 0);
        return version;
    }
}
