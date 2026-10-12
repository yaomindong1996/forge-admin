package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.knowledge.service.AiKnowledgeService;
import com.mdframe.forge.plugin.ai.multimodal.image.AiImageGenerationService;
import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.skill.mapper.AiSkillFileMapper;
import com.mdframe.forge.plugin.ai.skill.mapper.AiSkillMapper;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentBuiltinToolContractTest {

    @Test
    void should_useActualUserNotTenant_when_imageToolCreatesRecord() {
        AiImageGenerationService service = mock(AiImageGenerationService.class);
        when(service.generate(any())).thenReturn(101L);
        var tool = new ImageGenerateTool(service, selector());
        LoginUser user = new LoginUser();
        user.setUserId(31L);
        user.setTenantId(7L);
        try (var ignored = LoginUserContextHolder.open(user)) {
            assertThat(tool.execute(Map.of("prompt", "fixture"), ToolContext.of("fixture", 21L, 7L, 0))
                    .isSuccess()).isTrue();
        }
        ArgumentCaptor<AiImageGenerateRecord> record = ArgumentCaptor.forClass(AiImageGenerateRecord.class);
        verify(service).generate(record.capture());
        assertThat(record.getValue().getUserId()).isEqualTo(31L);
        assertThat(record.getValue().getModelId()).isEqualTo(89L);
    }

    @Test
    void should_notLeakBackendDiagnostics_when_builtinToolsReturnErrors() {
        AiKnowledgeService knowledge = mock(AiKnowledgeService.class);
        when(knowledge.search(any())).thenThrow(new IllegalStateException("fixture-secret-diagnostic"));
        AiSkillMapper skill = mock(AiSkillMapper.class);
        when(skill.selectEnabledByCode(anyString())).thenThrow(new IllegalStateException("fixture-secret-diagnostic"));
        AiImageGenerationService image = mock(AiImageGenerationService.class);
        when(image.generate(any())).thenThrow(new IllegalStateException("fixture-secret-diagnostic"));
        LoginUser user = new LoginUser();
        user.setUserId(31L);
        user.setTenantId(7L);
        String imageError;
        try (var ignored = LoginUserContextHolder.open(user)) {
            imageError = new ImageGenerateTool(image, selector())
                    .execute(Map.of("prompt", "fixture"), null).getError();
        }
        List<String> errors = List.of(new RagSearchTool(knowledge).execute(Map.of("query", "fixture"), null).getError(),
                new ReadSkillTool(skill, mock(AiSkillFileMapper.class))
                        .execute(Map.of("skill_code", "fixture"), null).getError(),
                imageError);
        assertThat(errors).allSatisfy(error -> assertThat(error).doesNotContain("fixture-secret-diagnostic"));
        verify(image).generate(any());
    }

    private AgentImageModelSelector selector() {
        var selector = mock(AgentImageModelSelector.class);
        var model = new AiModel();
        model.setId(89L);
        when(selector.requireModel(org.mockito.ArgumentMatchers.nullable(ToolContext.class))).thenReturn(model);
        return selector;
    }
}
