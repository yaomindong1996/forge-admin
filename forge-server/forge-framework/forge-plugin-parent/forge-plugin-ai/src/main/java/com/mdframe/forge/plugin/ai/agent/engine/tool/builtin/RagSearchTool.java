package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolContributor;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolResult;
import com.mdframe.forge.plugin.ai.knowledge.service.AiKnowledgeService;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchRequest;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索内置工具
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RagSearchTool implements AgentTool {

    private final AiKnowledgeService knowledgeService;

    @Override
    public String getKey() {
        return "rag_search";
    }

    @Override
    public String getDescription() {
        return "在知识库中检索与查询相关的文档片段。当需要查找特定信息、文档内容或数据时使用此工具。";
    }

    @Override
    public String getParametersSchema() {
        return """
        {
          "type": "object",
          "properties": {
            "query": {
              "type": "string",
              "description": "检索查询文本"
            },
            "knowledge_id": {
              "type": "integer",
              "description": "知识库ID（可选，不传则使用Agent绑定的知识库）"
            },
            "top_k": {
              "type": "integer",
              "description": "返回结果数量（默认5）"
            }
          },
          "required": ["query"]
        }
        """;
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        try {
            String query = (String) args.get("query");
            if (query == null || query.isBlank()) {
                return ToolResult.error("查询文本不能为空");
            }

            Integer topK = args.get("top_k") != null ? toInt(args.get("top_k")) : null;

            // 目标知识库：优先模型显式传入的 knowledge_id，其次回退到 Agent 绑定的知识库（ToolContext 注入）
            List<Long> targetIds = new ArrayList<>();
            if (args.get("knowledge_id") != null) {
                targetIds.add(toLong(args.get("knowledge_id")));
            } else if (context != null && context.getKnowledgeIds() != null && !context.getKnowledgeIds().isEmpty()) {
                targetIds.addAll(context.getKnowledgeIds());
            }

            List<KnowledgeSearchResult> results;
            if (targetIds.isEmpty()) {
                // 既未指定也无绑定：保持原行为（不限定知识库）
                results = knowledgeService.search(buildRequest(query, null, topK));
            } else if (targetIds.size() == 1) {
                results = knowledgeService.search(buildRequest(query, targetIds.get(0), topK));
            } else {
                // 多个绑定知识库：逐库检索后按相似度降序合并，再按 topK 截断
                results = new ArrayList<>();
                for (Long kid : targetIds) {
                    List<KnowledgeSearchResult> part = knowledgeService.search(buildRequest(query, kid, topK));
                    if (part != null) {
                        results.addAll(part);
                    }
                }
                results.sort(Comparator.comparingDouble(KnowledgeSearchResult::getScore).reversed());
                int limit = topK != null ? topK : 5;
                if (results.size() > limit) {
                    results = new ArrayList<>(results.subList(0, limit));
                }
            }

            if (results == null || results.isEmpty()) {
                return ToolResult.text("未找到相关文档。");
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                KnowledgeSearchResult r = results.get(i);
                sb.append("[").append(i + 1).append("] ");
                if (r.getTitle() != null) sb.append(r.getTitle()).append(": ");
                sb.append(r.getContent());
                sb.append("\n\n");
            }
            return ToolResult.text(sb.toString());
        } catch (Exception e) {
            log.warn("[AgentRag] 检索失败: exceptionType={}", e.getClass().getSimpleName());
            return ToolResult.error("RAG 检索失败，请稍后重试");
        }
    }

    private KnowledgeSearchRequest buildRequest(String query, Long knowledgeId, Integer topK) {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest();
        request.setQuery(query);
        if (knowledgeId != null) {
            request.setKnowledgeId(knowledgeId);
        }
        if (topK != null) {
            request.setTopK(topK);
        }
        return request;
    }

    private Long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }
}
