package com.mdframe.forge.plugin.ai.agent.engine.tool;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.provider.dashscope.DashScopeNativeChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentToolPromptFactoryTest {

    @Test
    void should_bindPerRequestTools_when_nativeAndOpenAiShareTheFactory() {
        for (ChatModel model : models()) {
            var defaults = (ToolCallingChatOptions) model.getOptions();
            var originalCallbacks = defaults.getToolCallbacks();
            ReactContext context = context(model, List.of(tool("lookup")));
            var prompt = AgentToolPromptFactory.create(context);
            var options = (ToolCallingChatOptions) prompt.getOptions();

            assertThat(options).isNotSameAs(defaults).isExactlyInstanceOf(defaults.getClass());
            assertThat(options.getTemperature()).isEqualTo(0.42);
            assertThat(options.getMaxTokens()).isEqualTo(64);
            assertThat(options.getModel()).isEqualTo("fixture-model");
            assertThat(options.getToolCallbacks()).extracting(callback -> callback.getToolDefinition().name())
                    .containsExactly("lookup");
            assertThatThrownBy(() -> options.getToolCallbacks().get(0).call("{}"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(defaults.getToolCallbacks()).isEqualTo(originalCallbacks);

            var second = (ToolCallingChatOptions) AgentToolPromptFactory.create(
                    context(model, List.of(tool("second_lookup")))).getOptions();
            assertThat(second.getToolCallbacks()).extracting(callback -> callback.getToolDefinition().name())
                    .containsExactly("second_lookup");
            assertThat(options.getToolCallbacks()).extracting(callback -> callback.getToolDefinition().name())
                    .containsExactly("lookup");
        }
    }

    @Test
    void should_declareNoTools_when_agentHasNoBindings() {
        for (ChatModel model : models()) {
            var options = (ToolCallingChatOptions) AgentToolPromptFactory.create(context(model, List.of()))
                    .getOptions();
            assertThat(options.getToolCallbacks()).isEmpty();
        }
    }

    @Test
    void should_rejectAmbiguousNames_when_differentSourcesBindTheSameToolKey() {
        assertThatThrownBy(() -> AgentToolPromptFactory.create(context(models().get(0),
                List.of(tool("duplicate"), tool("duplicate")))))
                .hasMessageContaining("重复");
    }

    @Test
    void should_failClosed_when_toolsAreBoundToAnUnsupportedModel() {
        ChatModel model = mock(ChatModel.class);
        assertThatThrownBy(() -> AgentToolPromptFactory.create(context(model, List.of(tool("lookup")))))
                .hasMessageContaining("不支持");
        assertThat(AgentToolPromptFactory.create(context(model, List.of())).getOptions()).isNull();
    }

    private List<ChatModel> models() {
        var nativeOptions = ToolCallingChatOptions.builder().model("fixture-model").temperature(0.42)
                .maxTokens(64).build();
        var openAiOptions = OpenAiChatOptions.builder().model("fixture-model").temperature(0.42)
                .maxTokens(64).apiKey("fixture-unusable-credential").baseUrl("https://fixture.invalid/v1").build();
        return List.of(new DashScopeNativeChatModel("https://fixture.invalid", "fixture-unusable-credential",
                nativeOptions), OpenAiChatModel.builder().options(openAiOptions).build());
    }

    private ReactContext context(ChatModel model, List<AgentTool> tools) {
        ReactContext context = new ReactContext();
        context.setChatModel(model);
        context.setUserMessage("fixture question");
        context.setBoundTools(tools);
        return context;
    }

    private AgentTool tool(String name) {
        AgentTool tool = mock(AgentTool.class);
        when(tool.getKey()).thenReturn(name);
        when(tool.getDescription()).thenReturn("fixture lookup");
        when(tool.getParametersSchema()).thenReturn("{\"type\":\"object\",\"properties\":{}}");
        return tool;
    }
}
