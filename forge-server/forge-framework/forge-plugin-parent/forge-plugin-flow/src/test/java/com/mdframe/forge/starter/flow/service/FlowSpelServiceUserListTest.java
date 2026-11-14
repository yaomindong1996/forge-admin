package com.mdframe.forge.starter.flow.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowSpelServiceUserListTest {

    private final FlowSpelService service = new FlowSpelService();

    @Test
    void turnsCommaSeparatedUsersIntoCountersignCollection() {
        assertThat(service.toUserList("1, 70,,1")).containsExactly("1", "70");
    }

    @Test
    void acceptsCollectionsAndEmptyInput() {
        assertThat(service.toUserList(List.of("5", " 6 ", "5"))).containsExactly("5", "6");
        assertThat(service.toUserList(null)).isEmpty();
        assertThat(service.toUserList("")).isEmpty();
    }

    @Test
    void expandsRolesAndUserGroupsIntoCountersignUsers() {
        FlowOrgIntegrationService org = mock(FlowOrgIntegrationService.class);
        ReflectionTestUtils.setField(service, "flowOrgIntegrationService", org);
        when(org.getUserIdsByRoleCode("ZFMANA")).thenReturn(List.of("1", "70"));
        when(org.getUserIdsByRoleCode("qa_group")).thenReturn(List.of());
        when(org.getUserIdsByGroupCode("qa_group")).thenReturn(List.of("70", "88"));

        assertThat(service.toGroupUserList("ZFMANA,qa_group")).containsExactly("1", "70", "88");
    }
}
