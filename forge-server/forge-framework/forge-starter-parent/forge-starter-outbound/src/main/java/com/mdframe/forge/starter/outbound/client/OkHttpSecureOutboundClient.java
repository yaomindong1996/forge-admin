package com.mdframe.forge.starter.outbound.client;

import com.mdframe.forge.starter.outbound.config.OutboundProperties;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundRequestContext;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import com.mdframe.forge.starter.outbound.model.ValidatedOutboundTarget;
import com.mdframe.forge.starter.outbound.security.OutboundPolicyService;
import com.mdframe.forge.starter.outbound.security.OutboundSecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.Proxy;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class OkHttpSecureOutboundClient implements SecureOutboundClient {

    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD");
    private static final Set<String> DANGEROUS_HEADERS = Set.of(
            "host", "content-length", "transfer-encoding", "connection", "keep-alive",
            "proxy-connection", "upgrade", "proxy-authorization", "proxy-authenticate",
            "te", "trailer", "x-inner-call");
    private static final Set<String> CREDENTIAL_HEADERS = Set.of(
            "authorization", "cookie", "cookie2", "proxy-authorization");
    private static final int BUFFER_SIZE = 8192;

    private final OutboundPolicyService policyService;
    private final OutboundProperties properties;

    @Override
    public OutboundResponse execute(OutboundRequest request) {
        RequestState state = validateRequest(request);
        long deadlineNanos = System.nanoTime() + positiveDuration(properties.getCallTimeout(), "整体超时").toNanos();
        int redirects = 0;

        while (true) {
            ensureBeforeDeadline(deadlineNanos);
            OutboundRequestContext context = new OutboundRequestContext(state.scene(), state.url());
            ValidatedOutboundTarget target = policyService.validate(context);
            OutboundResponse response = executeSingle(state, context, target, deadlineNanos);
            if (!isRedirect(response.getStatusCode())) {
                log.info("受控出站请求完成: scene={}, target={}://{}:{}, status={}",
                        state.scene(), target.getUri().getScheme(), target.getHost(), target.getPort(),
                        response.getStatusCode());
                return response;
            }
            if (!properties.isRedirectsEnabled()) {
                throw new OutboundSecurityException("出站请求不允许重定向");
            }
            if (redirects >= properties.getMaxRedirects()) {
                throw new OutboundSecurityException("出站请求重定向次数超过上限");
            }
            String location = response.firstHeader("Location");
            if (location == null || location.isBlank()) {
                throw new OutboundSecurityException("重定向响应缺少Location");
            }
            state = redirectState(state, target.getUri(), location, response.getStatusCode());
            redirects++;
        }
    }

    private OutboundResponse executeSingle(RequestState state,
                                           OutboundRequestContext context,
                                           ValidatedOutboundTarget initialTarget,
                                           long deadlineNanos) {
        OkHttpClient client = buildClient(context, initialTarget, deadlineNanos);
        Request httpRequest = buildRequest(state, initialTarget.getUri());
        try (Response response = client.newCall(httpRequest).execute()) {
            return new OutboundResponse(
                    response.code(),
                    response.headers().toMultimap(),
                    readBoundedBody(response.body()));
        } catch (IOException exception) {
            OutboundSecurityException securityException = findSecurityException(exception);
            if (securityException != null) {
                throw securityException;
            }
            if (isTimeout(exception)) {
                throw new OutboundSecurityException("出站请求超时");
            }
            throw new OutboundSecurityException("出站请求失败");
        } finally {
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdown();
        }
    }

    private OkHttpClient buildClient(OutboundRequestContext context,
                                     ValidatedOutboundTarget initialTarget,
                                     long deadlineNanos) {
        long remainingMillis = remainingMillis(deadlineNanos);
        return new OkHttpClient.Builder()
                .dns(hostname -> resolveForConnection(hostname, context, initialTarget))
                .proxy(Proxy.NO_PROXY)
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.MILLISECONDS))
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(cappedMillis(properties.getConnectTimeout(), remainingMillis, "连接超时"),
                        TimeUnit.MILLISECONDS)
                .readTimeout(cappedMillis(properties.getReadTimeout(), remainingMillis, "读取超时"),
                        TimeUnit.MILLISECONDS)
                .writeTimeout(cappedMillis(properties.getWriteTimeout(), remainingMillis, "写入超时"),
                        TimeUnit.MILLISECONDS)
                .callTimeout(remainingMillis, TimeUnit.MILLISECONDS)
                .build();
    }

    private java.util.List<java.net.InetAddress> resolveForConnection(
            String hostname,
            OutboundRequestContext context,
            ValidatedOutboundTarget initialTarget) throws UnknownHostException {
        if (!hostname.equalsIgnoreCase(initialTarget.getHost())) {
            throw new UnknownHostException("连接主机与已校验主机不一致");
        }
        try {
            ValidatedOutboundTarget refreshed = policyService.validate(context);
            if (!refreshed.getHost().equalsIgnoreCase(initialTarget.getHost())
                    || refreshed.getPort() != initialTarget.getPort()) {
                throw new OutboundSecurityException("连接目标与已校验目标不一致");
            }
            return refreshed.getAddresses();
        } catch (OutboundSecurityException exception) {
            UnknownHostException wrapped = new UnknownHostException("连接前DNS安全校验失败");
            wrapped.initCause(exception);
            throw wrapped;
        }
    }

    private Request buildRequest(RequestState state, URI uri) {
        Request.Builder builder = new Request.Builder().url(uri.toString());
        state.headers().forEach(builder::header);
        byte[] body = state.body();
        RequestBody requestBody = null;
        if (body.length > 0 || requiresRequestBody(state.method())) {
            MediaType mediaType = state.contentType() == null || state.contentType().isBlank()
                    ? null : MediaType.parse(state.contentType());
            requestBody = RequestBody.create(body, mediaType);
        }
        builder.method(state.method(), requestBody);
        return builder.build();
    }

    private byte[] readBoundedBody(ResponseBody body) throws IOException {
        if (body == null) {
            return new byte[0];
        }
        long maximum = positiveLimit(properties.getMaxResponseBytes(), "响应大小");
        if (body.contentLength() > maximum) {
            throw new OutboundSecurityException("出站响应超过大小上限");
        }
        try (InputStream input = body.byteStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maximum) {
                    throw new OutboundSecurityException("出站响应超过大小上限");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private RequestState validateRequest(OutboundRequest request) {
        if (request == null || request.getScene() == null || request.getUrl() == null) {
            throw new OutboundSecurityException("出站请求参数不完整");
        }
        String method = request.getMethod() == null ? "POST" : request.getMethod().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_METHODS.contains(method)) {
            throw new OutboundSecurityException("不支持的出站HTTP方法");
        }
        byte[] body = request.getBody() == null ? new byte[0] : request.getBody().clone();
        if (body.length > positiveLimit(properties.getMaxRequestBytes(), "请求大小")) {
            throw new OutboundSecurityException("出站请求超过大小上限");
        }
        if (("GET".equals(method) || "HEAD".equals(method)) && body.length > 0) {
            throw new OutboundSecurityException(method + "请求不能携带正文");
        }
        Map<String, String> headers = validateHeaders(request.getHeaders());
        return new RequestState(
                request.getScene(), request.getUrl(), method, headers, request.getContentType(), body);
    }

    private Map<String, String> validateHeaders(Map<String, String> requestHeaders) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (requestHeaders == null) {
            return headers;
        }
        requestHeaders.forEach((name, value) -> {
            if (name == null || value == null || name.isBlank() || containsLineBreak(name) || containsLineBreak(value)
                    || DANGEROUS_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                throw new OutboundSecurityException("出站请求包含不安全的Header");
            }
            headers.put(name, value);
        });
        return headers;
    }

    private RequestState redirectState(RequestState current, URI currentUri, String location, int statusCode) {
        URI nextUri;
        try {
            nextUri = currentUri.resolve(location);
        } catch (IllegalArgumentException exception) {
            throw new OutboundSecurityException("重定向目标格式不合法");
        }
        Map<String, String> nextHeaders = new LinkedHashMap<>(current.headers());
        if (!sameOrigin(currentUri, nextUri)) {
            nextHeaders.entrySet().removeIf(
                    entry -> CREDENTIAL_HEADERS.contains(entry.getKey().toLowerCase(Locale.ROOT)));
        }
        String method = current.method();
        byte[] body = current.body();
        String contentType = current.contentType();
        if (statusCode == 303 || ((statusCode == 301 || statusCode == 302) && "POST".equals(method))) {
            method = "GET";
            body = new byte[0];
            contentType = null;
        }
        return new RequestState(current.scene(), nextUri.toString(), method, nextHeaders, contentType, body);
    }

    private boolean sameOrigin(URI first, URI second) {
        return first.getScheme() != null && second.getScheme() != null
                && first.getScheme().equalsIgnoreCase(second.getScheme())
                && first.getHost() != null && second.getHost() != null
                && first.getHost().equalsIgnoreCase(second.getHost())
                && effectivePort(first) == effectivePort(second);
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private boolean requiresRequestBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303
                || statusCode == 307 || statusCode == 308;
    }

    private long remainingMillis(long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            throw new OutboundSecurityException("出站请求超过整体超时");
        }
        return Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
    }

    private void ensureBeforeDeadline(long deadlineNanos) {
        remainingMillis(deadlineNanos);
    }

    private long cappedMillis(Duration configured, long remainingMillis, String name) {
        return Math.max(1, Math.min(positiveDuration(configured, name).toMillis(), remainingMillis));
    }

    private Duration positiveDuration(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new OutboundSecurityException(name + "配置必须大于0");
        }
        return duration;
    }

    private long positiveLimit(long value, String name) {
        if (value <= 0) {
            throw new OutboundSecurityException(name + "上限必须大于0");
        }
        return value;
    }

    private boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }

    private boolean isTimeout(IOException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.io.InterruptedIOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private OutboundSecurityException findSecurityException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof OutboundSecurityException exception) {
                return exception;
            }
            current = current.getCause();
        }
        return null;
    }

    private record RequestState(String scene, String url, String method,
                                Map<String, String> headers, String contentType, byte[] body) {
    }
}
