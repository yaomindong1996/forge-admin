package com.mdframe.forge.starter.websocket.security;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.AntPathMatcher;

import java.security.Principal;

/**
 * 强制 STOMP 会话认证并限制客户端可访问的消息目标。
 */
public class AuthenticatedWebSocketChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final WebSocketAuthenticationProvider authenticationProvider;
    private final WebSocketProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthenticatedWebSocketChannelInterceptor(WebSocketAuthenticationProvider authenticationProvider,
                                                     WebSocketProperties properties) {
        this.authenticationProvider = authenticationProvider;
        this.properties = properties;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 必须复用消息上已有的 accessor：StompSubProtocolHandler 会在 CONNECT 时注册
        // setUserChangeCallback，wrap() 再重建消息会丢掉回调，导致后续 SUBSCRIBE 无 Principal。
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        StompCommand command = accessor.getCommand();
        if (command == null || command == StompCommand.DISCONNECT) {
            return message;
        }
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            authenticate(message, accessor);
            return message;
        }

        requireAuthenticated(message, accessor);
        if (command == StompCommand.SEND) {
            requireApplicationDestination(message, accessor.getDestination());
        } else if (command == StompCommand.SUBSCRIBE) {
            requireAllowedSubscription(message, accessor.getDestination());
        }
        return message;
    }

    private void authenticate(Message<?> message, StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        String tokenValue = extractBearerToken(authorization);
        if (authenticationProvider == null || tokenValue == null) {
            throw new MessageDeliveryException(message, "WebSocket CONNECT 缺少有效 Bearer Token");
        }
        String loginId = authenticationProvider.authenticate(tokenValue);
        if (loginId == null || loginId.isBlank()) {
            throw new MessageDeliveryException(message, "WebSocket Token 无效或已过期");
        }
        accessor.setUser(new LoginPrincipal(loginId));
    }

    private void requireAuthenticated(Message<?> message, StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessageDeliveryException(message, "WebSocket 会话未认证");
        }
    }

    private void requireApplicationDestination(Message<?> message, String destination) {
        String prefix = properties.getApplicationDestinationPrefix();
        if (destination == null || prefix == null
                || !(destination.equals(prefix) || destination.startsWith(prefix + "/"))) {
            throw new MessageDeliveryException(message, "客户端禁止直接向消息 Broker 发送消息");
        }
    }

    private void requireAllowedSubscription(Message<?> message, String destination) {
        if (destination == null || properties.getAllowedSubscribeDestinations() == null
                || properties.getAllowedSubscribeDestinations().stream()
                .noneMatch(pattern -> pathMatcher.match(pattern, destination))) {
            throw new MessageDeliveryException(message, "无权订阅 WebSocket 目标: " + destination);
        }
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String tokenValue = authorization.substring(BEARER_PREFIX.length()).trim();
        return tokenValue.isEmpty() ? null : tokenValue;
    }

    private record LoginPrincipal(String name) implements Principal {

        @Override
        public String getName() {
            return name;
        }
    }
}
