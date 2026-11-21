package com.mdframe.forge.plugin.ai.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.plugin.ai.resource.domain.McpConnection;
import com.mdframe.forge.starter.core.exception.BusinessException;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * MCP Streamable HTTP 客户端网关（单次操作短会话，凭据绝不进入异常或返回体）。
 */
@Component
@RequiredArgsConstructor
public class McpGateway {

    private static final int MAX_PAGES = 20;
    private static final int MAX_TOOLS = 200;
    private static final int MAX_PAYLOAD_BYTES = 65536;
    private static final int MAX_RESPONSE_BYTES = 262144;

    private final McpDestinationPolicy destinations;
    private final AiSecretCrypto secrets;
    private final ObjectMapper json;

    public McpSyncClient connect(McpConnection connection) {
        URI uri = destinations.require(connection.getEndpoint());
        Duration timeout = Duration.ofSeconds(connection.getTimeoutSeconds());
        var transport = HttpClientStreamableHttpTransport.builder(uri.getScheme() + "://" + uri.getRawAuthority())
                .endpoint(uri.getRawPath().isBlank() ? "/" : uri.getRawPath())
                .jsonMapper(new JacksonMcpJsonMapper(JsonMapper.builder().build()))
                .clientBuilder(HttpClient.newBuilder()
                        .connectTimeout(timeout)
                        .followRedirects(HttpClient.Redirect.NEVER))
                .httpRequestCustomizer((request, method, endpoint, body, context) -> {
                    request.timeout(timeout);
                    if (connection.getSecretCipher() != null && !connection.getSecretCipher().isBlank()) {
                        request.header("Authorization", "Bearer " + secrets.decrypt(connection.getSecretCipher()));
                    }
                })
                .openConnectionOnStartup(false)
                .resumableStreams(false)
                .build();
        return McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("ForgeAdmin", "1.0"))
                .requestTimeout(timeout)
                .initializationTimeout(timeout)
                .build();
    }

    public List<McpSchema.Tool> discover(McpConnection connection) {
        var client = connect(connection);
        try {
            client.initialize();
            return list(client);
        } finally {
            client.closeGracefully();
        }
    }

    List<McpSchema.Tool> list(McpSyncClient client) {
        List<McpSchema.Tool> rows = new ArrayList<>();
        Set<String> cursors = new HashSet<>();
        Set<String> names = new HashSet<>();
        String cursor = null;
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        for (int page = 0; page < MAX_PAGES; page++) {
            if (System.nanoTime() > deadline) {
                throw new BusinessException("目录读取超过总时限，请收敛服务端目录后重试");
            }
            var result = client.listTools(cursor);
            if (result == null || result.tools() == null) {
                throw new BusinessException("服务未返回有效工具目录");
            }
            for (var tool : result.tools()) {
                if (tool.name() == null || !tool.name().matches("[a-zA-Z0-9_.-]{1,128}") || !names.add(tool.name())
                        || tool.inputSchema() == null || encode(tool).length() > MAX_PAYLOAD_BYTES) {
                    throw new BusinessException("工具目录存在重复名称或无效结构");
                }
                rows.add(tool);
                if (rows.size() > MAX_TOOLS) {
                    throw new BusinessException("工具超过200项，请在MCP服务端收敛目录");
                }
            }
            cursor = result.nextCursor();
            if (cursor == null || cursor.isBlank()) {
                return rows;
            }
            if (!cursors.add(cursor)) {
                throw new BusinessException("工具目录分页游标重复");
            }
        }
        throw new BusinessException("工具目录分页超过20页");
    }

    public String schema(McpSchema.Tool tool) {
        return encode(tool.inputSchema());
    }

    public String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("工具结构无法解析");
        }
    }

    public String canonical(Object value) {
        try {
            return json.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(json.convertValue(value, TreeMap.class));
        } catch (Exception e) {
            throw new BusinessException("工具结构无法解析");
        }
    }

    public String call(McpConnection connection, String name, String expectedHash, Map<String, Object> args) {
        if (encode(args).length() > MAX_PAYLOAD_BYTES) {
            throw new BusinessException("工具参数过大");
        }
        var client = connect(connection);
        try {
            client.initialize();
            var tool = list(client).stream()
                    .filter(t -> name.equals(t.name()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("远端工具已移除，请重新同步并核对绑定"));
            if (!McpResourceManager.contract(connection, tool, this).equals(expectedHash)) {
                throw new BusinessException("远端工具契约已改变，请重新同步并核对绑定");
            }
            var result = client.callTool(new McpSchema.CallToolRequest(name, args));
            if (result == null || Boolean.TRUE.equals(result.isError())) {
                throw new BusinessException("MCP工具执行未成功，请检查服务状态");
            }
            String text = encode(result.structuredContent() != null
                    ? result.structuredContent() : result.content());
            if (text.length() > MAX_RESPONSE_BYTES) {
                throw new BusinessException("工具返回内容过大，请在服务端缩小结果");
            }
            return text;
        } finally {
            client.closeGracefully();
        }
    }
}
