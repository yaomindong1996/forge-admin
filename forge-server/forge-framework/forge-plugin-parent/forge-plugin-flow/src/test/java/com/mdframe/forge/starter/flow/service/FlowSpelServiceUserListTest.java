package com.mdframe.forge.starter.flow.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
