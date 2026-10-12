package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolResult;
import com.mdframe.forge.plugin.ai.multimodal.image.AiImageGenerationService;
import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import com.mdframe.forge.plugin.ai.multimodal.image.enums.AiImageGenerateStatus;
import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 图片生成内置工具。
 * Agent 对话中 LLM 可调用此工具生成图片。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenerateTool implements AgentTool {

    private final AiImageGenerationService imageGenerationService;
    private final AgentImageModelSelector modelSelector;

    @Override
    public String getKey() {
        return "image_generate";
    }

    @Override
    public String getDescription() {
        return "根据文本提示词生成图片。当用户需要创建、绘制或生成图片时使用此工具。";
    }

    @Override
    public String getParametersSchema() {
        return """
        {
          "type": "object",
          "properties": {
            "prompt": {
              "type": "string",
              "description": "图片生成提示词，描述想要生成的图片内容"
            },
            "negative_prompt": {
              "type": "string",
              "description": "负面提示词，描述不希望出现在图片中的内容（可选）"
            },
            "size": {
              "type": "string",
              "description": "图片尺寸，如 1024x1024、512x512（可选，默认1024x1024）"
            }
          },
          "required": ["prompt"]
        }
        """;
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        try {
            String prompt = (String) args.get("prompt");
            if (prompt == null || prompt.isBlank()) {
                return ToolResult.error("提示词不能为空");
            }

            AiImageGenerateRecord record = new AiImageGenerateRecord();
            record.setPrompt(prompt);
            record.setNegativePrompt((String) args.get("negative_prompt"));
            record.setSize(args.get("size") != null ? (String) args.get("size") : "1024x1024");
            record.setModelId(modelSelector.requireModel(context).getId());
            // tenantId 不能当用户 ID；执行池中的受信用户 Scope 与发起用户一致。
            record.setUserId(SessionHelper.getUserId());

            Long recordId = imageGenerationService.generate(record);

            // 异步任务 ID 不是图片 fileId；只能在轮询成功后使用 resultFileId 显示图片。
            return ToolResult.data(JSON.toJSONString(Map.of("recordId", String.valueOf(recordId),
                    "status", AiImageGenerateStatus.PENDING.getCode(),
                    "resultEndpoint", "/ai/image-generate/" + recordId,
                    "message", "图片任务已提交，请查询结果，成功后使用 resultFileId")));
        } catch (Exception e) {
            log.error("[ImageGenerateTool] 图片生成失败: exceptionType={}", e.getClass().getSimpleName());
            return ToolResult.error("图片生成失败，请检查 Agent 图像模型配置或稍后重试");
        }
    }
}
