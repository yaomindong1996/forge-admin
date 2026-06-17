package com.mdframe.forge.starter.flow.listener;

import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.delegate.event.impl.FlowableProcessCancelledEventImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FlowTaskEventListenerTest {

    @Test
    void shouldResolveProcessInstanceIdFromProcessCancelledEvent() {
        FlowableProcessCancelledEventImpl event = new FlowableProcessCancelledEventImpl();
        event.setExecutionId("process-001");
        event.setProcessInstanceId("process-001");
        event.setProcessDefinitionId("leave:1:definition-001");

        assertFalse(event instanceof FlowableEntityEvent);
        assertEquals("process-001", new FlowTaskEventListener().resolveProcessInstanceId(event));
    }
}
