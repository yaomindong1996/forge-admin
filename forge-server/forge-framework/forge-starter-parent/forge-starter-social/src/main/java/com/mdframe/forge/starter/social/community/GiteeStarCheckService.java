package com.mdframe.forge.starter.social.community;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 用当前用户 Gitee access_token 检查是否已 Star 指定仓库。
 * 不落库 Token，仅在 OAuth 换票当下调用。
 * <p>
 * 单仓库 Star 查询接口 GET /user/starred/{owner}/{repo} 已被 Gitee 废弃（返回 200 + 废弃提示），
 * 改用 GET /users/{login}/starred 分页拉取当前用户 Star 列表，本地匹配 full_name。
 * 已 Star 结论本地缓存短周期，降低对 Gitee 的调用频率。
 * <p>
 * 失败文案前缀约定（前端可区分展示）：
 * {@code [GITEE_STAR_REQUIRED]} / {@code [GITEE_SCOPE_INSUFFICIENT]} /
 * {@code [GITEE_STAR_CHECK_TIMEOUT]} / {@code [GITEE_STAR_CHECK_FAILED]}
 */
@Slf4j
@Service
public class GiteeStarCheckService {

    static final String ERR_STAR_REQUIRED = "[GITEE_STAR_REQUIRED]";
    static final String ERR_SCOPE_INSUFFICIENT = "[GITEE_SCOPE_INSUFFICIENT]";
    static final String ERR_TIMEOUT = "[GITEE_STAR_CHECK_TIMEOUT]";
    static final String ERR_FAILED = "[GITEE_STAR_CHECK_FAILED]";

    private static final String STARRED_LIST_URL = "https://gitee.com/api/v5/users/";
    private static final String REPO_API_URL = "https://gitee.com/api/v5/repos/";
    private static final int PER_PAGE = 100;
    /** 分页安全上限：防止 Gitee 异常返回造成无限翻页；正常用户 Star 数远达不到 */
    private static final int MAX_PAGES = 30;
    /** 分页拉取总耗时预算：校验在登录线程上同步执行，防止 Gitee 慢响应长时间占住容器 worker，超预算按校验超时处理 */
    private static final long TOTAL_BUDGET_MS = 20_000;
    /** 已 Star 结果本地缓存分钟数：登录高峰降低对 Gitee 的调用频率，规避限流 */
    private static final long STAR_CACHE_MINUTES = 10;
    /** 仓库真实创建者缓存分钟数：创建者极少变化，缓存降低对 Gitee 的调用频率 */
    private static final long OWNER_CACHE_MINUTES = 60;

    private final GiteeCommunityLoginSupport communityLoginSupport;
    private final HttpClient httpClient;
    /** 已 Star 结论缓存；未 Star 不缓存，用户点完 Star 立即重试必须能通过 */
    private final Cache<String, Boolean> starCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(STAR_CACHE_MINUTES))
            .maximumSize(1000)
            .build();
    /** 仓库真实创建者缓存：Gitee 仓库 API 的 owner.login 是创建者个人账号，可能与配置的组织 namespace（如 ForgeLab）不同 */
    private final Cache<String, String> repoOwnerCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(OWNER_CACHE_MINUTES))
            .maximumSize(32)
            .build();

    @Autowired
    public GiteeStarCheckService(GiteeCommunityLoginSupport communityLoginSupport) {
        this(communityLoginSupport, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build());
    }

    GiteeStarCheckService(GiteeCommunityLoginSupport communityLoginSupport, HttpClient httpClient) {
        this.communityLoginSupport = communityLoginSupport;
        this.httpClient = httpClient;
    }

    public void assertStarred(String accessToken, String login) {
        if (!communityLoginSupport.requireStar()) {
            return;
        }
        if (StrUtil.isBlank(accessToken)) {
            throw new BusinessException(ERR_FAILED + " 未能获取 Gitee 授权，请重新登录");
        }
        if (StrUtil.isBlank(login)) {
            throw new BusinessException(ERR_FAILED + " 未能获取 Gitee 用户名，请重新登录");
        }
        GiteeCommunityLoginSupport.GiteeCommunitySettings config = communityLoginSupport.config();
        String owner = StrUtil.trimToEmpty(config.getOwner());
        String repo = StrUtil.trimToEmpty(config.getRepo());
        if (StrUtil.hasBlank(owner, repo)) {
            throw new BusinessException(ERR_FAILED + " Gitee 社区登录未配置仓库");
        }
        String targetFullName = owner + "/" + repo;

        // 已 Star 结论短缓存：重复登录/短时间重试不再请求 Gitee，规避限流
        if (Boolean.TRUE.equals(starCache.getIfPresent(login))) {
            log.debug("Gitee Star 校验命中本地缓存: login={}, repo={}", login, targetFullName);
            return;
        }

        // 仓库创建者直接放行：Gitee 不把用户对自己仓库的 Star 计入个人 Star 列表，
        // 创建者（如项目作者本人）无法通过列表校验，也不应被自己的 Star 门槛拦截
        if (isRepoOwner(login, config)) {
            log.info("Gitee Star 校验放行: login={} 是仓库 {} 的创建者", login, targetFullName);
            return;
        }

        StarFetchResult fetchResult = fetchStarred(accessToken, login, targetFullName, config);
        if (fetchResult.starred()) {
            starCache.put(login, Boolean.TRUE);
            return;
        }
        if (fetchResult.timedOut()) {
            throw new BusinessException(ERR_TIMEOUT
                    + " Gitee Star 校验超时，请稍后重试；若刚点完 Star 请稍候片刻再登录");
        }
        // 未 Star 不缓存：用户跳去点 Star 后马上回来重试必须能通过
        // 提示稍候：Gitee Star 列表存在秒级传播延迟，点完立即重试可能仍未命中
        String link = StrUtil.blankToDefault(config.getRepoUrl(), "https://gitee.com/ForgeLab/forge-admin");
        throw new BusinessException(ERR_STAR_REQUIRED
                + " 请先给仓库点 Star 后再登录（点完稍候片刻再重试）：" + link);
    }

    /**
     * 判断当前登录用户是否为仓库真实创建者（缓存 1 小时）。
     */
    private boolean isRepoOwner(String login, GiteeCommunityLoginSupport.GiteeCommunitySettings config) {
        String owner = StrUtil.trimToEmpty(config.getOwner());
        String repo = StrUtil.trimToEmpty(config.getRepo());
        String cacheKey = owner + "/" + repo;
        String repoOwner = repoOwnerCache.get(cacheKey, key -> fetchRepoOwner(owner, repo, config));
        return StrUtil.isNotBlank(repoOwner) && repoOwner.equals(login);
    }

    /**
     * 查询仓库真实创建者：GET /repos/{owner}/{repo} 的 owner.login。
     * 公开仓库匿名可查；查询失败（私有仓库/接口异常）返回 null 不缓存，回退正常 Star 校验。
     */
    private String fetchRepoOwner(String owner, String repo,
                                  GiteeCommunityLoginSupport.GiteeCommunitySettings config) {
        String url = REPO_API_URL + encode(owner) + "/" + encode(repo);
        int timeoutMs = Math.max(500, config.getTimeoutMs());
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Forge-Admin")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.debug("Gitee 仓库创建者查询非 200: owner={}, repo={}, statusCode={}, body={}",
                        owner, repo, response.statusCode(), truncate(response.body()));
                return null;
            }
            return JSONUtil.parseObj(response.body()).getJSONObject("owner").getStr("login");
        } catch (Exception exception) {
            log.warn("Gitee 仓库创建者查询失败: owner={}, repo={}, reason={}", owner, repo, exception.getMessage());
            return null;
        }
    }

    /**
     * 分页拉取当前用户 Star 列表并匹配目标仓库。
     * <p>
     * 未命中终止条件：空页 / 不足一页（列表拉完）；翻页达页数上限或总耗时预算时标记超时。
     * 每次未命中都会通过 logStarredDetail 打出实际拉取的仓库明细，便于定位 Gitee 接口异常。
     */
    private StarFetchResult fetchStarred(String accessToken, String login, String targetFullName,
                                         GiteeCommunityLoginSupport.GiteeCommunitySettings config) {
        int timeoutMs = Math.max(1000, config.getTimeoutMs());
        long deadline = System.currentTimeMillis() + TOTAL_BUDGET_MS;
        List<String> seenRepos = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            if (page > 1 && System.currentTimeMillis() > deadline) {
                log.warn("Gitee Star 列表翻页达总耗时预算: login={}, repo={}, page={}, budgetMs={}，按校验超时处理",
                        login, targetFullName, page, TOTAL_BUDGET_MS);
                logStarredDetail(login, targetFullName, seenRepos);
                return StarFetchResult.timeout();
            }
            String url = STARRED_LIST_URL + encode(login) + "/starred"
                    + "?access_token=" + encode(accessToken)
                    + "&page=" + page + "&per_page=" + PER_PAGE;
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofMillis(timeoutMs))
                        .header("Accept", "application/json")
                        .header("User-Agent", "Forge-Admin")
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JSONArray repos = interpret(response.statusCode(), response.body());
                if (repos == null) {
                    logStarredDetail(login, targetFullName, seenRepos);
                    return StarFetchResult.missing();
                }
                seenRepos.addAll(extractFullNames(repos));
                if (containsRepo(repos, targetFullName)) {
                    log.info("Gitee Star 校验通过: login={}, repo={}, 命中于第 {} 页，已扫描 {} 个仓库",
                            login, targetFullName, page, seenRepos.size());
                    return StarFetchResult.found();
                }
                if (repos.size() < PER_PAGE) {
                    logStarredDetail(login, targetFullName, seenRepos);
                    return StarFetchResult.missing();
                }
            } catch (BusinessException exception) {
                throw exception;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ERR_TIMEOUT + " Gitee Star 校验被中断，请稍后重试");
            } catch (Exception exception) {
                log.warn("Gitee Star 校验失败: login={}, repo={}, page={}, timeoutMs={}, reason={}",
                        login, targetFullName, page, timeoutMs, exception.getMessage());
                throw new BusinessException(ERR_TIMEOUT + " Gitee Star 校验超时或失败，请稍后重试");
            }
        }
        log.warn("Gitee Star 列表翻页达安全上限: login={}, repo={}, maxPages={}，按校验超时处理",
                login, targetFullName, MAX_PAGES);
        logStarredDetail(login, targetFullName, seenRepos);
        return StarFetchResult.timeout();
    }

    private record StarFetchResult(boolean starred, boolean timedOut) {
        static StarFetchResult found() {
            return new StarFetchResult(true, false);
        }

        static StarFetchResult missing() {
            return new StarFetchResult(false, false);
        }

        static StarFetchResult timeout() {
            return new StarFetchResult(false, true);
        }
    }

    /**
     * 打印本次实际拉取到的用户 Star 列表明细：校验未通过时用于定位
     * （区分“用户真没 Star”与“Gitee 接口返回异常/为空”）。
     */
    private void logStarredDetail(String login, String targetFullName, List<String> seenRepos) {
        log.warn("Gitee Star 校验未通过: login={} 未 Star 仓库 {}，本次实际拉取 {} 个仓库: {}",
                login, targetFullName, seenRepos.size(),
                StrUtil.maxLength(String.join(", ", seenRepos), 500));
    }

    /**
     * 解析 Star 列表单页响应。
     *
     * @return 当前页仓库数组；空页（列表拉完）返回 null
     */
    JSONArray interpret(int statusCode, String responseBody) {
        if (statusCode == 200) {
            try {
                JSONArray repos = JSONUtil.parseArray(responseBody);
                return repos.isEmpty() ? null : repos;
            } catch (Exception exception) {
                // 200 但非数组（如接口废弃提示对象）：日志保留响应体便于定位
                log.warn("Gitee Star 列表响应非预期（可能接口变更/废弃）: body={}", truncate(responseBody));
                throw new BusinessException(ERR_FAILED + " Gitee Star 校验失败，请稍后重试");
            }
        }
        if (statusCode == 401 || statusCode == 403) {
            log.warn("Gitee Star 校验未通过: statusCode={}, body={}", statusCode, truncate(responseBody));
            throw new BusinessException(ERR_SCOPE_INSUFFICIENT
                    + " Gitee 授权不足，请确认 OAuth 应用已勾选 projects，并在登录授权页重新勾选后重试");
        }
        log.warn("Gitee Star 校验返回非预期状态码: statusCode={}, body={}", statusCode, truncate(responseBody));
        throw new BusinessException(ERR_FAILED + " Gitee Star 校验失败（状态码 " + statusCode + "），请稍后重试");
    }

    static boolean containsRepo(JSONArray repos, String targetFullName) {
        if (StrUtil.isBlank(targetFullName) || repos == null) {
            return false;
        }
        for (Object item : repos) {
            JSONObject starredRepo = JSONUtil.parseObj(item);
            String fullName = starredRepo.getStr("full_name");
            if (targetFullName.equalsIgnoreCase(StrUtil.trimToEmpty(fullName))) {
                return true;
            }
        }
        return false;
    }

    static List<String> extractFullNames(JSONArray repos) {
        List<String> names = new ArrayList<>(repos.size());
        for (Object item : repos) {
            names.add(JSONUtil.parseObj(item).getStr("full_name"));
        }
        return names;
    }

    private static String truncate(String body) {
        return StrUtil.maxLength(StrUtil.trimToEmpty(body), 200);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
