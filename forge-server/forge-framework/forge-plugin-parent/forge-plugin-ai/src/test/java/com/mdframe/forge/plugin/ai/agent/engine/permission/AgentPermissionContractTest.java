package com.mdframe.forge.plugin.ai.agent.engine.permission;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 既有权限策略的回归：本轮绑定收敛不能偷偷更改策略码或提高权限。 */
class AgentPermissionContractTest {

    private final AgentToolPermissionService service = mock(AgentToolPermissionService.class);
    private final PermissionEngine engine = new PermissionEngine(service);

    @Test
    void should_keepExplicitDecisionFirst_when_toolHasConfiguredPermission() {
        for (PermissionDecision decision : PermissionDecision.values()) {
            when(service.getDecision(21L, "submit_fixture")).thenReturn(decision);
            assertThat(engine.decide(21L, "submit_fixture", "all")).isEqualTo(decision);
        }
    }

    @Test
    void should_stillAskForRiskyNames_when_noExplicitPolicyExists() {
        assertThat(engine.decide(21L, "submit_fixture", "all")).isEqualTo(PermissionDecision.ASK);
        assertThat(engine.decide(21L, "delete_fixture", "skill")).isEqualTo(PermissionDecision.ASK);
    }

    @Test
    void should_preserveReadDefault_when_noExplicitPolicyExists() {
        assertThat(engine.decide(21L, "lookup", "all")).isEqualTo(PermissionDecision.ALLOW);
    }
}
