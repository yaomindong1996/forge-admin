package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlowRecordParticipantServiceImplTest {

    @Test
    void parseSplitsObjectCodeAndRecordId() {
        var ref = FlowRecordParticipantServiceImpl.parse(null, "ORDER:88");
        assertEquals("ORDER", ref.businessType());
        assertEquals("88", ref.businessId());
    }

    @Test
    void parseFallsBackToBusinessTypeWhenKeyHasNoColon() {
        var ref = FlowRecordParticipantServiceImpl.parse("LEAVE", "88");
        assertEquals("LEAVE", ref.businessType());
        assertEquals("88", ref.businessId());
    }

    @Test
    void parseReturnsNullWhenKeyCannotBeResolved() {
        assertNull(FlowRecordParticipantServiceImpl.parse(null, "88"));
        assertNull(FlowRecordParticipantServiceImpl.parse("LEAVE", " "));
    }
}
