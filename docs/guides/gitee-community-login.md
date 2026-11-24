# Gitee 社区体验登录排障

## 正常流程

1. 配置中心开启「Gitee 社区体验登录」，仓库 owner/repo/url 指向需 Star 的仓（默认 `ForgeLab/forge-admin`）。
2. 协作连接中 Gitee LOGIN 应用 `scope=user_info,projects`，回调地址与 Gitee 开放平台登记的前端 `/login/callback` 完全一致。
3. 用户先 Star 仓库，再点「Gitee 免密登录」，授权页勾选 **projects**。
4. 服务端换票后调用 `GET /users/{login}/starred` 分页匹配 `full_name`，通过后进入体验租户。

## 前端报错对照

| 页面提示 | 服务端前缀 / 关键词 | 处理 |
|---------|---------------------|------|
| 请先给 Gitee 仓库点 Star | `[GITEE_STAR_REQUIRED]` | 确认 Star 了配置中的仓库；点完稍候再登 |
| Gitee 授权权限不足 | `[GITEE_SCOPE_INSUFFICIENT]` | 开放平台勾选 projects；取消旧授权后重登 |
| Gitee Star 校验超时 | `[GITEE_STAR_CHECK_TIMEOUT]` | 稍后重试；Star 很多时可能翻页超时 |
| Gitee Star 校验失败 | `[GITEE_STAR_CHECK_FAILED]` | 看服务端 warn 日志响应体 |

## 服务端日志关键词

- `Gitee Star 校验`
- `授权不足` / `projects`
- `三方授权码换取失败`
- `本次实际拉取 N 个仓库`

## 存量库

执行迁移 `V1.0.245__gitee_oauth_scope_projects` 将仍为仅 `user_info` 的 Gitee scope 补齐为 `user_info,projects`。改 scope 后用户必须重新授权。
