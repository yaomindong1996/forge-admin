package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.config.config.LoginConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 登录页微信交流群看板代理。
 *
 * <p>配置了官网看板地址时，由服务端拉取开放群/历史群，避免浏览器跨域；
 * 未配置时回退为本系统登录配置中的本地群二维码字段。</p>
 */
@Slf4j
@RestController
@RequestMapping("/auth/groupQrcodeBoard")
@RequiredArgsConstructor
@IgnoreTenant
@SaIgnore
@ApiPermissionIgnore
public class LoginGroupQrcodeBoardController {

    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 3000;

    private final ConfigManagerService configManagerService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public RespInfo<Map<String, Object>> board() {
        LoginConfig config = configManagerService.getLoginConfig();
        if (config == null || !Boolean.TRUE.equals(config.getGroupQrcodeEnabled())) {
            return RespInfo.success(emptyBoard());
        }
        String boardUrl = StringUtils.trimToEmpty(config.getGroupQrcodeBoardUrl());
        if (StringUtils.isNotBlank(boardUrl)) {
            Map<String, Object> remote = fetchRemoteBoard(boardUrl);
            if (remote != null) {
                return RespInfo.success(remote);
            }
            log.warn("拉取官网交流群看板失败，回退本地群二维码: {}", boardUrl);
        }
        return RespInfo.success(localBoard(config));
    }

    private Map<String, Object> fetchRemoteBoard(String boardUrl) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
            factory.setReadTimeout(READ_TIMEOUT_MS);
            RestClient client = RestClient.builder().requestFactory(factory).build();
            String body = client.get().uri(URI.create(boardUrl)).retrieve().body(String.class);
            if (StringUtils.isBlank(body)) {
                return null;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data == null || data.isNull()) {
                return null;
            }
            Map<String, Object> board = new LinkedHashMap<>();
            board.put("openGroup", normalizeGroup(data.get("openGroup"), boardUrl, true));
            List<Map<String, Object>> history = new ArrayList<>();
            JsonNode historyNode = data.get("historyGroups");
            if (historyNode != null && historyNode.isArray()) {
                for (JsonNode item : historyNode) {
                    Map<String, Object> group = normalizeGroup(item, boardUrl, false);
                    if (group != null) {
                        history.add(group);
                    }
                }
            }
            board.put("historyGroups", history);
            return board;
        }
        catch (Exception ex) {
            log.warn("拉取官网交流群看板异常: {}", ex.toString());
            return null;
        }
    }

    private Map<String, Object> localBoard(LoginConfig config) {
        Map<String, Object> board = emptyBoard();
        if (StringUtils.isBlank(config.getGroupQrcodeImage())) {
            return board;
        }
        Map<String, Object> open = new LinkedHashMap<>();
        open.put("id", null);
        open.put("groupName", StringUtils.defaultIfBlank(config.getGroupQrcodeName(), "用户交流群"));
        open.put("memberCount", null);
        open.put("entryStatus", "OPEN");
        open.put("hint", StringUtils.defaultIfBlank(config.getGroupQrcodeHint(), "扫码加入用户群，获取验证码并完成登录"));
        open.put("qrcodeUrl", resolveLocalQrcodeUrl(config.getGroupQrcodeImage()));
        open.put("source", "local");
        board.put("openGroup", open);
        return board;
    }

    private Map<String, Object> emptyBoard() {
        Map<String, Object> board = new LinkedHashMap<>();
        board.put("openGroup", null);
        board.put("historyGroups", List.of());
        return board;
    }

    private Map<String, Object> normalizeGroup(JsonNode node, String boardUrl, boolean keepQrcode) {
        if (node == null || node.isNull() || !node.isObject()) {
            return null;
        }
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("id", textOrNull(node.get("id")));
        group.put("groupName", StringUtils.defaultIfBlank(textOrNull(node.get("groupName")), "用户交流群"));
        group.put("memberCount", node.hasNonNull("memberCount") ? node.get("memberCount").asInt() : null);
        group.put("entryStatus", textOrNull(node.get("entryStatus")));
        group.put("hint", textOrNull(node.get("hint")));
        group.put("source", "remote");
        if (keepQrcode) {
            String qrcodeUrl = firstNonBlank(textOrNull(node.get("qrcodeUrl")), textOrNull(node.get("qrcodeImage")));
            group.put("qrcodeUrl", absolutize(boardUrl, qrcodeUrl));
        }
        else {
            group.put("qrcodeUrl", null);
        }
        return group;
    }

    private String resolveLocalQrcodeUrl(String asset) {
        String value = StringUtils.trimToEmpty(asset);
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("data:") || lower.startsWith("blob:")) {
            return value;
        }
        return "/auth/loginQrcode?v=" + value;
    }

    private String absolutize(String boardUrl, String asset) {
        if (StringUtils.isBlank(asset)) {
            return null;
        }
        String value = asset.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("data:") || lower.startsWith("blob:")) {
            return value;
        }
        try {
            URI base = URI.create(boardUrl);
            URI origin = new URI(base.getScheme(), base.getAuthority(), null, null, null);
            if (value.startsWith("/")) {
                return origin + value;
            }
            return origin + "/" + value;
        }
        catch (Exception ex) {
            return value;
        }
    }

    private String firstNonBlank(String a, String b) {
        if (StringUtils.isNotBlank(a)) {
            return a;
        }
        return StringUtils.isNotBlank(b) ? b : null;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.isBlank(text) ? null : text;
    }
}
