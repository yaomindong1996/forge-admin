package com.mdframe.forge.plugin.generator.service.businessprocess;

import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessNode;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcess;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcessRun;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessMessageChannelService;
import com.mdframe.forge.plugin.message.service.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("BusinessProcessMessageExecutor")
class BusinessProcessMessageExecutorTest {

    @Test
    @DisplayName("template params fill FLOW_TODO_CARD placeholders from process name and node")
    void buildsTaskTitleAndProcessNameParams() {
        BusinessProcessMapper processMapper = mock(BusinessProcessMapper.class);
        BusinessProcessMessageExecutor executor = new BusinessProcessMessageExecutor(
                mock(BusinessMessageChannelService.class),
                mock(MessageService.class),
                processMapper);

        AiBusinessProcess process = new AiBusinessProcess();
        process.setProcessName("QA动作节点1115");
        when(processMapper.selectActiveById(1L, 88L)).thenReturn(process);

        AiBusinessProcessRun run = new AiBusinessProcessRun();
        run.setTenantId(1L);
        run.setProcessId(88L);
        run.setProcessCode("qa_1115_3752");
        run.setSubjectObjectCode("apply_kdcp");
        run.setSubjectRecordId("6");

        BusinessProcessNode node = new BusinessProcessNode();
        node.setName("发送站内信");

        Map<String, Object> params = executor.buildTemplateParams(run, node);

        assertEquals("发送站内信", params.get("taskTitle"));
        assertEquals("QA动作节点1115", params.get("processName"));
        assertEquals("qa_1115_3752", params.get("processCode"));
        assertEquals("apply_kdcp", params.get("objectCode"));
        assertEquals("6", params.get("recordId"));
        assertEquals("", params.get("startUserName"));
    }

    @Test
    @DisplayName("missing process name falls back to process code for taskTitle")
    void fallsBackToProcessCode() {
        BusinessProcessMessageExecutor executor = new BusinessProcessMessageExecutor(
                mock(BusinessMessageChannelService.class),
                mock(MessageService.class),
                mock(BusinessProcessMapper.class));

        AiBusinessProcessRun run = new AiBusinessProcessRun();
        run.setProcessCode("qa_code");
        BusinessProcessNode node = new BusinessProcessNode();

        Map<String, Object> params = executor.buildTemplateParams(run, node);

        assertEquals("qa_code", params.get("taskTitle"));
        assertEquals("qa_code", params.get("processName"));
    }
}
