package com.mdframe.forge.plugin.generator.codegen;

import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiPageTemplate;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于 AI 大模型的代码生成策略（适用于复杂组件：主从表、看板、工作流审批等）
 * <p>
 * 工作原理：
 * 1. 将 AiCrudConfig 配置信息 + AiPageTemplate.systemPrompt（组件规范说明）组合成 prompt
 * 2. 调用大模型，要求其返回结构化的代码文件列表（JSON 格式）
 * 3. 解析响应，打包成 zip 返回
 * <p>
 * 当前状态：骨架预留，待复杂组件类型接入时实现。
 * 通过在 ai_page_template.codegen_type 设置为 "AI" 来启用本策略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCodegenStrategy implements CodegenStrategy {

    @Override
    public boolean supports(String codegenType) {
        return "AI".equalsIgnoreCase(codegenType);
    }

    @Override
    public Map<String, String> generate(AiCrudConfig config, AiPageTemplate template) throws Exception {
        // TODO: 实现 AI 驱动的代码生成
        // 实现步骤：
        // 1. 构建 prompt = template.systemPrompt + config 的结构化信息（字段/接口/字典等）
        // 2. 调用 AiClientAdapter.call(prompt) 获取完整响应
        // 3. 解析响应 JSON，格式约定为：
        //    {
        //      "files": [
        //        { "path": "backend/src/.../Controller.java", "content": "..." },
        //        { "path": "frontend/src/views/.../index.vue", "content": "..." }
        //      ]
        //    }
        // 4. 将 files 列表转换为 Map<String, String> 返回
        //
        // 扩展点：
        // - 对于流式响应场景，可增加 generateStream 方法
        // - 可引入 AI 生成结果缓存（相同配置不重复调用）
        // - 可增加生成结果的代码质量检验（编译检查、格式化等）
        throw new BusinessException("AI 代码生成策略尚未实现，请联系开发者或使用 TEMPLATE 模式");
    }
}
