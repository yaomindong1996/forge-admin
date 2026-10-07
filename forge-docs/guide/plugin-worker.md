# 插件构建任务执行器

插件中心可以将**选定且已确认**的任务交给独立执行器。它不是浏览器热安装，
不自动消费全部队列，不部署、不启动业务服务/Flyway，也不授予角色或商业许可。
先阅读 [离线构建与隔离要求](./plugin-builder.md)，准备经过审查的 rootless daemon、
固定 digest 镜像及离线公开依赖缓存。当前仓库不附带可直接运行的镜像。

## 1. 部署侧认证

机器端点为 POST `/internal/plugin-build/{taskId}/claim|archive|heartbeat|finish`。
专用认证过滤器始终注册、默认关闭；`SaIgnore` 仅避免 Sa-Token 用户登录重复拦截。
普通用户/管理员 Token、`X-Inner-Call`、请求租户 ID 都不能替代机器认证。

经人工安全审查后，Admin 的运维配置可加入下列配置。凭证必须由可信部署系统生成和传递，
不要放入 Git、镜像、浏览器、代理访问日志或源码包。`TOKEN_SHA256` 是32字节随机 token
的**小写 hex 文本**（64字符）的 SHA-256，不是对原始32字节做摘要。

```yaml
forge:
  plugin-build:
    worker:
      enabled: false # 审查、验收后由运维显式开启
      id: ${FORGE_PLUGIN_WORKER_ID:}
      tenant-id: ${FORGE_PLUGIN_WORKER_TENANT_ID:}
      token-sha256: ${FORGE_PLUGIN_WORKER_TOKEN_SHA256:}
      expires-at: ${FORGE_PLUGIN_WORKER_EXPIRES_AT:}
```

- ID 使用小写字母/数字/下划线/短横线，最长64；租户为部署方绑定的正数。
- 时间使用带时区的 ISO-8601 Instant；到期或配置缺失直接拒绝。轮换后旧 token 失效。
- 强制 HTTPS，并以 Servlet `isSecure()` 为准。不接受任意客户端伪造的 HTTPS 请求头；
  TLS 代理必须清理外部转发头，由可信转发配置建立安全状态。
- 最好仅在受控运维网络开放此路径；每个部署目前绑定一个 worker 身份。
  机器权限仅包括本租户已确认任务的领取/包下载/续期/结构化回写，不具备部署权限。
- 不使用用户加密协商协议；此专用协议由强制 TLS + 机器认证保护，仍发送正常防重放
  `X-Timestamp`/随机 `X-Nonce`，不豁免全局重放验证。服务和执行器时钟需同步。
- V1.0.212 必须经正常 Flyway 在目标环境上线；不在 Web 进程运行源码或调用 shell。

## 2. 运维侧运行一次

在专用非 root 账户上创建私有工作区并准备 `worker.json`（配置文件不含凭证）：

```json
{
  "apiBaseUrl": "https://admin.example.com/api",
  "sourceRoot": "/srv/reviewed-host",
  "commit": "<已审查、干净Git HEAD的40位SHA>",
  "workspaceRoot": "/srv/private-build-work",
  "dockerExecutable": "/usr/bin/docker",
  "dockerSocket": "/run/user/1001/docker.sock",
  "image": "registry.example.com/forge-builder@sha256:<已审查镜像的64位SHA>"
}
```

工作区必须由专用用户拥有、权限0700，且与源码目录双向分离。URL 只支持规范 HTTPS
origin 和可选的 API 前缀，不允许账号、查询串、编码路径、重定向或证书校验关闭。
源码保持已提交/干净；不会修改原工作区。节点只接受明确白名单配置，不接受服务端命令或路径。

通过可信运维机制将 raw token 放入该进程的 `FORGE_PLUGIN_WORKER_TOKEN` 环境。
从插件中心详情获取任务 ID、**当前 revision**、ZIP SHA-256。审查包来源、POM/前端源码、
主机快照和镜像后执行：

```sh
pnpm forge:plugin-build worker /absolute/worker.json <任务ID> <revision> <包SHA256> --reviewed
```

整包替换还需 `--force`，它不绕过客户定制/未提交改动/本地配置保护。
仅 queued 可首次领取；重复同租约可恢复响应，但不会创建第二次构建。CLI 不自动重试领取。
包限8MiB，机器下载后再次核对长度/SHA，再交给原离线执行器。
凭证、租约和 `transfer-*` 恢复收据不进入无网络构建容器。

## 3. 租约与结果

租约90秒，每30秒及阶段切换串行续期，硬期限25分钟。网络/认证失效或信号中止时，
本轮构建会沿用随机名称+nonce 所有权核对的容器清理，不在宿主降级。
阶段为源码快照 → 包核验 → 源码预检 → 隔离容器构建 → 实际产物核验。

状态为 queued → building → built（构建通过、待部署）/build_failed。
结果包含源码/镜像/包绑定、稳定错误码、文件数/总字节/清单 SHA；不含日志、私有路径或下载 URL。
服务端校验协议与租约，但**产物实际核验发生在 worker，不是服务端再次检查制品**。
当前产物仅保存在私有 `job-*/artifacts/`，尚未接入制品仓库或部署流程。

`transfer-*/worker-result.json` 保存桥接结果；`lease.json` 为权限0600的私有恢复收据，
不要上传/公开，raw 机器 Bearer 不落盘。原 `job-*/result.json` 和产物仍保留。
结果不确定时返回 `report_pending`，不能当作远端构建完成，也不能用新随机租约再执行一遍。

租约到期、构建成功或失败均保留插件占用和审计，当前页面不提供强行释放/删除按钮。
人工先核查本次容器、结果/收据及后台状态；后续受控恢复/部署阶段再处置占用。
此边界防止失联旧构建与新任务并发，不要直接改表或删除审计以绕过保护。

## 4. 验收边界

H2/MockMvc、模拟 UI 和 Docker 桩不是目标环境验收。上线前仍需确认真实 HTTPS/重放/租户、
Flyway、rootless 资源限制、镜像与离线缓存、超时/失联清理；部署/健康核验在下一阶段单独授权。
