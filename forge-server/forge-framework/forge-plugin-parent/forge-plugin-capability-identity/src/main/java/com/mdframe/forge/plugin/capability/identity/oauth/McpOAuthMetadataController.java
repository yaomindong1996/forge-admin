package com.mdframe.forge.plugin.capability.identity.oauth;

import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.capability.identity", name = "enabled", havingValue = "true")
public class McpOAuthMetadataController {

    private static final List<String> SCOPES = List.of(
            "capability:discover",
            "capability:invoke",
            "capability:discover:capability.ping",
            "capability:invoke:capability.ping");

    private final CapabilityIdentityProperties properties;

    @GetMapping("/.well-known/oauth-protected-resource")
    public ResponseEntity<Map<String, Object>> protectedResourceMetadata() {
        String issuer = properties.validatedIssuer();
        Map<String, Object> metadata = Map.of(
                "resource", properties.validatedResource(),
                "authorization_servers", List.of(issuer),
                "bearer_methods_supported", List.of("header"),
                "scopes_supported", SCOPES);
        return publicMetadata(metadata);
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public ResponseEntity<Map<String, Object>> authorizationServerMetadata() {
        String issuer = properties.validatedIssuer();
        Map<String, Object> metadata = Map.ofEntries(
                Map.entry("issuer", issuer),
                Map.entry("authorization_endpoint", issuer + "/mcp-authorize"),
                Map.entry("token_endpoint", issuer + "/oauth2/token"),
                Map.entry("revocation_endpoint", issuer + "/oauth2/revoke"),
                Map.entry("response_types_supported", List.of("code")),
                Map.entry("grant_types_supported", List.of("authorization_code", "client_credentials")),
                Map.entry("code_challenge_methods_supported", List.of("S256")),
                Map.entry("token_endpoint_auth_methods_supported",
                        List.of("client_secret_basic", "client_secret_post", "none")),
                Map.entry("scopes_supported", SCOPES));
        return publicMetadata(metadata);
    }

    private ResponseEntity<Map<String, Object>> publicMetadata(Map<String, Object> metadata) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(metadata);
    }
}
