package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.StreamGenerateRequest;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrudGeneratorStreamService {

    private final AiClientAdapter aiClientAdapter;
    private final GenTableColumnMapper genTableColumnMapper;
    private final SchemaGenerator schemaGenerator;
    private final ObjectMapper objectMapper;

    private static final String STAGE_SEARCH = "generating-search";
    private static final String STAGE_COLUMNS = "generating-columns";
    private static final String STAGE_EDIT = "generating-edit";
    private static final String STAGE_API = "generating-api";
    private static final String STAGE_META = "generating-meta";
    private static final String STAGE_SQL = "generating-sql";

    public Flux<ServerSentEvent<String>> streamGenerate(StreamGenerateRequest request) {
        if (StringUtils.isBlank(request.getConfigKey())) {
            return Flux.just(buildErrorEvent("configKey不能为空"));
        }

        String prompt = buildStreamPrompt(request);

        Map<String, String> contextVars = new HashMap<>();
        contextVars.put("configKey", request.getConfigKey());
        if (StringUtils.isNotBlank(request.getTableName())) {
            contextVars.put("tableName", request.getTableName());
        }

        return Flux.concat(
                Flux.just(buildProgressEvent("analyzing", "正在分析需求...")),
                aiClientAdapter.stream(request.getDescription(), request.getSessionId(), "crud_config_builder", prompt, contextVars)
                        .flatMap(this::processChunkToFlux)
                        .onErrorResume(e -> {
                            log.error("[CrudGeneratorStreamService] 流式生成异常", e);
                            return Flux.just(buildErrorEvent("生成失败: " + e.getMessage()));
                        }),
                Flux.just(buildCompleteEvent(request.getConfigKey()))
        );
    }

    /**
     * 将单个 AI chunk 转换为一个或多个 SSE 事件：
     * - 遇到阶段标记先发 progress 事件，再将标记后的剩余内容作为 chunk 事件发出
     * - 无阶段标记时直接发 chunk 事件
     */
    private Flux<ServerSentEvent<String>> processChunkToFlux(String chunk) {
        List<ServerSentEvent<String>> events = new ArrayList<>();

        // 按阶段标记顺序处理（一个 chunk 可能同时含标记和内容）
        if (chunk.contains("[STAGE:meta]")) {
            events.add(buildProgressEvent(STAGE_META, "正在推断元数据..."));
            String metaJson = extractMetaJson(chunk);
            if (metaJson != null) {
                events.add(buildMetaEvent(metaJson));
                chunk = removeMetaContent(chunk);
            } else {
                chunk = chunk.replace("[STAGE:meta]", "");
            }
        }
        if (chunk.contains("[STAGE:searchSchema]")) {
            events.add(buildProgressEvent(STAGE_SEARCH, "正在生成 searchSchema..."));
            chunk = chunk.replace("[STAGE:searchSchema]", "");
        }
        if (chunk.contains("[STAGE:columnsSchema]")) {
            events.add(buildProgressEvent(STAGE_COLUMNS, "正在生成 columnsSchema..."));
            chunk = chunk.replace("[STAGE:columnsSchema]", "");
        }
        if (chunk.contains("[STAGE:editSchema]")) {
            events.add(buildProgressEvent(STAGE_EDIT, "正在生成 editSchema..."));
            chunk = chunk.replace("[STAGE:editSchema]", "");
        }
        if (chunk.contains("[STAGE:apiConfig]")) {
            events.add(buildProgressEvent(STAGE_API, "正在生成 apiConfig..."));
            chunk = chunk.replace("[STAGE:apiConfig]", "");
        }
        if (chunk.contains("[STAGE:createTableSql]")) {
            events.add(buildProgressEvent(STAGE_SQL, "正在生成建表SQL..."));
            chunk = chunk.replace("[STAGE:createTableSql]", "");
        }

        // 去掉任何残余的其他阶段标记
        String cleaned = chunk.replaceAll("\\[STAGE:[^\\]]+\\]", "");
        if (StringUtils.isNotBlank(cleaned)) {
            events.add(buildChunkEvent(cleaned));
        }

        return events.isEmpty() ? Flux.empty() : Flux.fromIterable(events);
    }

    private String buildStreamPrompt(StreamGenerateRequest request) {
        StringBuilder sb = new StringBuilder();

        // === 动态部分：基本信息 ===
        sb.append("## 基本信息\n");
        sb.append("- configKey: ").append(request.getConfigKey()).append("\n");
        if (StringUtils.isNotBlank(request.getTableName())) {
            sb.append("- tableName: ").append(request.getTableName()).append("\n");
        }
        if (StringUtils.isNotBlank(request.getDescription())) {
            sb.append("- 需求描述: ").append(request.getDescription()).append("\n");
        }

        // === 动态部分：现有配置（迭代修改场景）===
        boolean hasExisting = StringUtils.isNotBlank(request.getExistingSearchSchema())
                || StringUtils.isNotBlank(request.getExistingColumnsSchema())
                || StringUtils.isNotBlank(request.getExistingEditSchema())
                || StringUtils.isNotBlank(request.getExistingApiConfig());
        if (hasExisting) {
            sb.append("\n## 当前已有配置\n");
            sb.append("请基于以下现有配置，根据用户的新需求做修改（只改需要变的部分）：\n");
            if (StringUtils.isNotBlank(request.getExistingSearchSchema())) {
                sb.append("searchSchema: ").append(request.getExistingSearchSchema()).append("\n");
            }
            if (StringUtils.isNotBlank(request.getExistingColumnsSchema())) {
                sb.append("columnsSchema: ").append(request.getExistingColumnsSchema()).append("\n");
            }
            if (StringUtils.isNotBlank(request.getExistingEditSchema())) {
                sb.append("editSchema: ").append(request.getExistingEditSchema()).append("\n");
            }
            if (StringUtils.isNotBlank(request.getExistingApiConfig())) {
                sb.append("apiConfig: ").append(request.getExistingApiConfig()).append("\n");
            }
        }

        // === 动态部分：表结构注入 ===
        if (StringUtils.isNotBlank(request.getTableName())) {
            injectTableContext(sb, request);
        }

        return sb.toString();
    }

    /**
     * 注入数据库表结构上下文和 SchemaGenerator 参考配置
     */
    private void injectTableContext(StringBuilder sb, StreamGenerateRequest request) {
        try {
            List<GenTableColumn> dbColumns = genTableColumnMapper.selectDbTableColumnsByName(request.getTableName());
            if (dbColumns == null || dbColumns.isEmpty()) {
                return;
            }
            sb.append("\n## 数据库表结构\n");
            sb.append("表名: ").append(request.getTableName()).append("\n");
            sb.append("| 字段名 | 类型 | 注释 | 主键 | 必填 |\n");
            sb.append("|--------|------|------|------|------|\n");
            for (GenTableColumn col : dbColumns) {
                sb.append("| ").append(col.getColumnName())
                  .append(" | ").append(col.getColumnType() != null ? col.getColumnType() : "")
                  .append(" | ").append(col.getColumnComment() != null ? col.getColumnComment() : "")
                  .append(" | ").append(col.getIsPk() != null && col.getIsPk() == 1 ? "是" : "")
                  .append(" | ").append(col.getIsRequired() != null && col.getIsRequired() == 1 ? "是" : "")
                  .append(" |\n");
            }

            // 尝试用 SchemaGenerator 生成参考配置
            try {
                List<GenTableColumn> configuredColumns = genTableColumnMapper.selectList(
                    new LambdaQueryWrapper<GenTableColumn>()
                        .inSql(GenTableColumn::getTableId,
                            "SELECT table_id FROM gen_table WHERE table_name = '" + request.getTableName() + "'"));
                if (configuredColumns != null && !configuredColumns.isEmpty()) {
                    var refResult = schemaGenerator.generate(
                        request.getConfigKey(), request.getTableName(), "", configuredColumns);
                    sb.append("\n## 参考配置（基于规则引擎生成，供你参考和优化）\n");
                    sb.append("请在此基础上优化：补充更好的 label、调整字段顺序、添加合适的字典类型、优化搜索条件等。\n");
                    sb.append("{\n");
                    sb.append("  \"searchSchema\": ").append(refResult.getSearchSchema()).append(",\n");
                    sb.append("  \"columnsSchema\": ").append(refResult.getColumnsSchema()).append(",\n");
                    sb.append("  \"editSchema\": ").append(refResult.getEditSchema()).append(",\n");
                    sb.append("  \"apiConfig\": ").append(refResult.getApiConfig()).append("\n");
                    sb.append("}\n");
                }
            } catch (Exception e) {
                log.debug("[CrudGeneratorStreamService] SchemaGenerator参考配置生成失败，跳过", e);
            }
        } catch (Exception e) {
            log.debug("[CrudGeneratorStreamService] 表结构查询失败，跳过", e);
        }
    }

    private ServerSentEvent<String> buildProgressEvent(String stage, String message) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("stage", stage);
            data.put("message", message);
            return ServerSentEvent.builder(objectMapper.writeValueAsString(data))
                    .event("progress")
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private ServerSentEvent<String> buildChunkEvent(String content) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("content", content);
            return ServerSentEvent.builder(objectMapper.writeValueAsString(data))
                    .event("chunk")
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private ServerSentEvent<String> buildMetaEvent(String metaJson) {
        try {
            return ServerSentEvent.builder(metaJson)
                    .event("meta")
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 尝试从 chunk 中提取 [STAGE:meta] 后的完整 JSON
     */
    private String extractMetaJson(String chunk) {
        String marker = "[STAGE:meta]";
        int metaPos = chunk.indexOf(marker);
        if (metaPos < 0) return null;
        int contentStart = metaPos + marker.length();
        int nextStagePos = chunk.indexOf("[STAGE:", contentStart);
        String jsonPart;
        if (nextStagePos > 0) {
            jsonPart = chunk.substring(contentStart, nextStagePos).trim();
        } else {
            jsonPart = chunk.substring(contentStart).trim();
        }
        if (jsonPart.startsWith("{") && jsonPart.endsWith("}")) {
            return jsonPart;
        }
        return null;
    }

    /**
     * 从 chunk 中移除 [STAGE:meta] 及其内容
     */
    private String removeMetaContent(String chunk) {
        String marker = "[STAGE:meta]";
        int metaPos = chunk.indexOf(marker);
        if (metaPos < 0) return chunk;
        int contentStart = metaPos + marker.length();
        int nextStagePos = chunk.indexOf("[STAGE:", contentStart);
        if (nextStagePos > 0) {
            return chunk.substring(0, metaPos) + chunk.substring(nextStagePos);
        } else {
            return chunk.substring(0, metaPos);
        }
    }

    private ServerSentEvent<String> buildCompleteEvent(String configKey) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("configKey", configKey);
            data.put("message", "生成完成！");
            return ServerSentEvent.builder(objectMapper.writeValueAsString(data))
                    .event("complete")
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private ServerSentEvent<String> buildErrorEvent(String message) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("message", message);
            return ServerSentEvent.builder(objectMapper.writeValueAsString(data))
                    .event("error")
                    .build();
        } catch (Exception e) {
            return ServerSentEvent.builder("{\"message\":\"生成失败\"}")
                    .event("error")
                    .build();
        }
    }
}
