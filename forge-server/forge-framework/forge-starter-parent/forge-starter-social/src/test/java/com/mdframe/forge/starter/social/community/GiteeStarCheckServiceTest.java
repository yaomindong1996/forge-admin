package com.mdframe.forge.starter.social.community;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.config.config.LoginConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GiteeStarCheckServiceTest {

    private GiteeStarCheckService service;

    @BeforeEach
    void setUp() {
        service = new GiteeStarCheckService(newSupport(true));
    }

    private GiteeCommunityLoginSupport newSupport(boolean requireStar) {
        LoginConfig loginConfig = new LoginConfig();
        loginConfig.setGiteeCommunityEnabled(true);
        loginConfig.setGiteeCommunityRequireStar(requireStar);
        loginConfig.setGiteeCommunityRepoUrl("https://gitee.com/ForgeLab/forge-admin");
        ConfigManagerService configManagerService = mock(ConfigManagerService.class);
        when(configManagerService.getLoginConfig()).thenReturn(loginConfig);
        return new GiteeCommunityLoginSupport(configManagerService);
    }

    @Test
    void starredListPageReturnsReposArray() {
        String body = "[{\"full_name\":\"ForgeLab/forge-admin\"},{\"full_name\":\"other/repo\"}]";
        assertThat(service.interpret(200, body)).isNotNull();
    }

    @Test
    void emptyStarredListPageMeansExhausted() {
        assertNull(service.interpret(200, "[]"));
    }

    @Test
    void deprecatedStyleObjectBodyFailsWithLoggedBody() {
        assertThatThrownBy(() -> service.interpret(200, "{\"message\":\"为避免滥用，star 接口已标记为废弃\"}"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Gitee Star 校验失败");
    }

    @Test
    void unauthorizedScopeFailsWithPermissionHint() {
        assertThatThrownBy(() -> service.interpret(403, "{\"message\":\"403\"}"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(GiteeStarCheckService.ERR_SCOPE_INSUFFICIENT)
                .hasMessageContaining("projects");
    }

    @Test
    void unexpectedStatusCodeCarriesCode() {
        assertThatThrownBy(() -> service.interpret(500, "{\"message\":\"internal error\"}"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(GiteeStarCheckService.ERR_FAILED)
                .hasMessageContaining("状态码 500");
    }

    @Test
    void containsRepoMatchesTargetFullName() {
        assertThat(GiteeStarCheckService.containsRepo(
                JSONUtil.parseArray("[{\"full_name\":\"other/repo\"},{\"full_name\":\"ForgeLab/forge-admin\"}]"),
                "ForgeLab/forge-admin")).isTrue();
        assertThat(GiteeStarCheckService.containsRepo(
                JSONUtil.parseArray("[{\"full_name\":\"other/repo\"}]"),
                "ForgeLab/forge-admin")).isFalse();
    }

    @Test
    void containsRepoMatchesCaseInsensitively() {
        assertThat(GiteeStarCheckService.containsRepo(
                JSONUtil.parseArray("[{\"full_name\":\"ForgeLab/Forge-admin\"}]"),
                "forgelab/forge-admin")).isTrue();
    }

    @Test
    void extractFullNamesCollectsRepoNames() {
        assertThat(GiteeStarCheckService.extractFullNames(
                JSONUtil.parseArray("[{\"full_name\":\"a/b\"},{\"full_name\":\"c/d\"}]")))
                .containsExactly("a/b", "c/d");
    }

    @Test
    void repoOwnerBypassesStarCheck() {
        // 仓库创建者无需 Star 自己的仓库（Gitee 不把自 Star 计入个人 Star 列表）
        GiteeStarCheckService ownerService =
                new GiteeStarCheckService(newSupport(true), stubHttpClient(200, "{\"owner\":{\"login\":\"yaomd\"}}"));

        assertDoesNotThrow(() -> ownerService.assertStarred("token", "yaomd"));
    }

    @Test
    void nonOwnerFallsThroughToStarListCheck() {
        // 非创建者继续走 Star 列表校验：stub 对 starred 请求同样返回对象体（非数组）→ 按接口异常拦截
        GiteeStarCheckService ownerService =
                new GiteeStarCheckService(newSupport(true), stubHttpClient(200, "{\"owner\":{\"login\":\"someone-else\"}}"));

        assertThatThrownBy(() -> ownerService.assertStarred("token", "yaomd"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Gitee Star 校验失败");
    }

    @Test
    void repoOwnerProbeFailureFallsBackToStarListCheck() {
        // repos 接口不可用（如私有仓库 404）时返回 null 不缓存，回退正常 Star 校验
        GiteeStarCheckService ownerService =
                new GiteeStarCheckService(newSupport(true), stubHttpClient(404, "{\"message\":\"404 Not Found\"}"));

        assertThatThrownBy(() -> ownerService.assertStarred("token", "yaomd"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态码 404");
    }

    @Test
    void missingTokenFailsClosed() {
        assertThatThrownBy(() -> service.assertStarred(" ", "someone"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("授权");
    }

    @Test
    void missingLoginFailsClosed() {
        assertThatThrownBy(() -> service.assertStarred("token", " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名");
    }

    @Test
    void springCreatesServiceThroughProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(ConfigManagerService.class, () -> mock(ConfigManagerService.class));
            context.register(GiteeCommunityLoginSupport.class, GiteeStarCheckService.class);

            context.refresh();

            assertThat(context.getBeansOfType(GiteeStarCheckService.class)).hasSize(1);
        }
    }

    @Test
    void starredPathDoesNotThrowWhenRequireStarDisabled() {
        GiteeStarCheckService disabled = new GiteeStarCheckService(newSupport(false));

        assertDoesNotThrow(() -> disabled.assertStarred(" ", " "));
    }

    /**
     * 固定响应的 HttpClient 桩：所有请求（repos/starrted）返回同一份 statusCode + body。
     */
    private static HttpClient stubHttpClient(int statusCode, String body) {
        return new HttpClient() {
            @Override
            public Optional<java.net.CookieHandler> cookieHandler() {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> connectTimeout() {
                return Optional.empty();
            }

            @Override
            public Optional<Executor> executor() {
                return Optional.empty();
            }

            @Override
            public Redirect followRedirects() {
                return Redirect.NEVER;
            }

            @Override
            public Version version() {
                return Version.HTTP_1_1;
            }

            @Override
            public Optional<ProxySelector> proxy() {
                return Optional.empty();
            }

            @Override
            public SSLContext sslContext() {
                return null;
            }

            @Override
            public SSLParameters sslParameters() {
                return null;
            }

            @Override
            public Optional<Authenticator> authenticator() {
                return Optional.empty();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                    throws IOException, InterruptedException {
                HttpResponse<String> fixed = new HttpResponse<String>() {
                    @Override
                    public int statusCode() {
                        return statusCode;
                    }

                    @Override
                    public String body() {
                        return body;
                    }

                    @Override
                    public HttpHeaders headers() {
                        return HttpHeaders.of(Map.of(), (name, value) -> true);
                    }

                    @Override
                    public Optional<HttpResponse<String>> previousResponse() {
                        return Optional.empty();
                    }

                    @Override
                    public URI uri() {
                        return URI.create("https://gitee.com/stub");
                    }

                    @Override
                    public Optional<SSLSession> sslSession() {
                        return Optional.empty();
                    }

                    @Override
                    public Version version() {
                        return Version.HTTP_1_1;
                    }

                    @Override
                    public HttpRequest request() {
                        return request;
                    }
                };
                return (HttpResponse<T>) fixed;
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                     HttpResponse.BodyHandler<T> responseBodyHandler) {
                return CompletableFuture.failedFuture(new UnsupportedOperationException("stub"));
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                     HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                     HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                return CompletableFuture.failedFuture(new UnsupportedOperationException("stub"));
            }
        };
    }
}
