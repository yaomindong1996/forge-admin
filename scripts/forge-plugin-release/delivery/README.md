# 插件制品发布和 Compose 部署

插件中心的“发布与部署”页负责审批绑定、授权目标、任务状态和审计；独立目标执行器负责 COS 传输与 Docker 操作。
Web 服务不会运行浏览器传入的命令。这里只交付已核验的 community Admin 应用 JAR/UI，不扩展付费源码构建权限，也不替代运行许可证。

## 配置目标

运行 V1.0.214 至 V1.0.216 的正式迁移后，在控制面外部配置中启用交付。目标 ID、仓库 ID 必须和执行器一致。
配置不会从数据库配置中心加载；凭证只存环境变量或私有挂载，不能放在源码、页面或日志。

```yaml
forge:
  plugin-delivery:
    enabled: true
    worker:
      id: delivery-worker
      tenant-id: 1
      token-sha256: ${FORGE_DELIVERY_TOKEN_SHA256}
      expires-at: ${FORGE_DELIVERY_EXPIRES_AT}
    targets:
      - id: staging-admin
        name: 测试管理端
        repository-id: admin-artifacts
```

交付 worker 和构建 worker 必须使用不同的 32 字节随机凭证。执行器使用 `FORGE_PLUGIN_DELIVERY_TOKEN` 原值，
控制面只配置它的 SHA256 小写摘要和明确的 UTC 到期时间。所有机器请求使用 HTTPS，禁重定向。
按目标/租户部署独立控制面配置；当前一个控制面绑定一名执行器及一个租户，可配置多个目标，但每个执行器配置只操作一个目标。

平台管理员需要分别授权 `system:plugin:delivery:list`、`system:plugin:delivery:execute`、
`system:plugin:delivery:reconcile`；迁移不自动授予任何角色。候选仍需原任务的构建审查和独立制品登记。

## 配置 COS

在 `scripts/forge-plugin-release` 执行 `npm ci --ignore-scripts`，仅安装锁定的官方 COS SDK。
使用私有桶，关闭版本控制；构建制品和源码 ZIP 使用不同前缀及不同 CAM 权限。
仅允许目标构建前缀的 PutObject/GetObject/GetObjectACL，以及桶的 GetBucketACL/GetBucketPolicy/GetBucketVersioning。
不授予删除、改 ACL、改策略等权限；PutObject 强制要求防覆盖头。
[腾讯 COS 防覆盖条件说明](https://cloud.tencent.cn/document/product/436/71307)给出 CAM 条件配置。

配置示例 `cos.json`，文件中没有 AK/SK：

```json
{
  "protocolVersion": 1,
  "repositoryId": "admin-artifacts",
  "vaultRoot": "/srv/forge-delivery/vault",
  "bucket": "replace-bucket-1250000000",
  "region": "ap-beijing",
  "prefix": "build-artifacts/",
  "sourcePrefix": "plugin-sources/"
}
```

执行器从 `FORGE_ARTIFACT_COS_SECRET_ID`、`FORGE_ARTIFACT_COS_SECRET_KEY`、可选
`FORGE_ARTIFACT_COS_SESSION_TOKEN` 读取独立最小权限凭证。优先使用短期凭证。
上传流式读取封存文件，所有对象读回实际 SHA256；清单最后上传。同名对象不覆盖，重试也必须读回验真。
取回清单有大小上限，按 releaseId 摘要验证后逐文件落盘、封存；没有清单的半包不可部署。

## 配置 Compose 执行器

目标必须是专用非 root 用户持有的 rootless Docker，使用 cgroup v2、systemd 和已委派的 CPU/内存/PID 控制器。
提前加载两个固定 digest 镜像，执行器不会拉取镜像或构建应用。原生 Linux 目标验收由部署方完成。
Docker socket 和父目录必须归执行器用户所有、父目录0700，不接受 TCP socket 或 rootful Docker。

先准备互不嵌套的私有目录：vault、compose、runtime-config、state、docker-empty。
`docker-empty` 必须为空，防止环境中的 credential helper 或 Docker CLI 配置影响执行。
配置使用 canonical 绝对路径，文件0600、目录0700；制品封存后由工具改为0400/0500。

`compose.json` 配置沿用 [Compose 离线准备](../compose/README.md)的十个字段：

```json
{
  "protocolVersion": 1,
  "repositoryId": "admin-artifacts",
  "vaultRoot": "/srv/forge-delivery/vault",
  "outputRoot": "/srv/forge-delivery/compose",
  "configRoot": "/srv/forge-delivery/runtime-config",
  "projectName": "forge-staging-admin",
  "serverImage": "eclipse-temurin@sha256:替换为实际64位摘要",
  "uiImage": "nginx@sha256:替换为实际64位摘要",
  "serverPort": 18580,
  "uiPort": 13000
}
```

镜像须能在只读根和 rootless 用户映射下运行；nginx 镜像须监听8080、将临时文件放到/tmp。
固定限制为2GiB内存、禁额外swap、2核CPU、256个PID；执行器还会检查实际容器限制，缺少限制即拒绝成功回执。
业务 `application.yml` 和 `nginx.conf` 放在 runtime-config，不能链接、硬链接或公开读取；配置不进入制品库。
后端 JAR 内端口须保持8580。挂载的 Spring 配置应包含数据库、Redis、许可证及必要配置。
Flyway 迁移路径须指向 JAR 内已打包的 `classpath:db/migration`，不要依赖宿主源码目录；先核查迁移再部署。
前端反向代理按实际网关配置；若 nginx 直连同 Compose 项目后端，可使用 `http://server:8580`，不能使用容器内127.0.0.1。

`delivery.json`：

```json
{
  "protocolVersion": 1,
  "apiBaseUrl": "https://control.example.com",
  "workerId": "delivery-worker",
  "targetId": "staging-admin",
  "cosConfig": "/srv/forge-delivery/cos.json",
  "composeConfig": "/srv/forge-delivery/compose.json",
  "dockerExecutable": "/usr/bin/docker",
  "dockerSocket": "/run/user/1000/docker.sock",
  "clientConfigRoot": "/srv/forge-delivery/docker-empty",
  "stateRoot": "/srv/forge-delivery/state"
}
```

实际可执行文件不得是软链接或组/其它用户可写。vault/COS/Compose 的 repositoryId 和 vaultRoot 必须相同。
单端目标从首次部署起保持同一 server/ui 组合；变更组合使用独立目标，不能静默删掉另一端服务。
不会执行 shell、自动 down、删容器、回滚数据库或覆盖未知项目。

## 配置运行探针

在被部署应用的私有 application.yml 中启用独立探针，凭证必须和构建、交付凭证不同：

```yaml
forge:
  plugin-runtime-probe:
    enabled: true
    allow-loopback: true
    token-sha256: ${FORGE_PROBE_TOKEN_SHA256}
    expires-at: ${FORGE_PROBE_EXPIRES_AT}
    jar-path: /opt/forge/admin.jar
```

外部配置中可以直接填摘要和到期时间；若使用以上环境占位符，必须通过部署侧 Secret 配置传入容器，
本工具不会自动转发宿主环境。原凭证仅通过执行器环境 `FORGE_RUNTIME_PROBE_TOKEN` 提供。
容器映射端口只绑定宿主127.0.0.1；网关禁止公开 `/internal/plugin-runtime/**`。
`allow-loopback` 只允许明确的 loopback HTTP，仍需独立凭证和 nonce；对其它请求要求 HTTPS。
IDE、WAR或 exploded 运行模式无法证明当前 JAR 身份，会拒绝运行核验。

执行器核对实际 JAR 摘要、当前 classpath 插件声明和版本，再从实际 HTTP 读取每个 UI 文件摘要。
启动连接暂不可用可有界等待；401、错误版本和摘要错误直接失败。
这证明部署文件与声明匹配，不替代数据库升级、付费功能授权或业务冒烟验收。

## 执行发布和部署

1. 在安装工作台完成构建审查；本地封存、导出登记 JSON，再在任务详情登记候选。
2. 在“发布与部署”选授权目标、锁定候选和“发布到COS”，填写确认说明，创建任务。
3. 在目标执行器运行下面命令领取该任务；不要将 Web 服务用户当作执行器。
4. COS 读回成功后，再创建“部署”任务，填写真实数据库备份编号、确认迁移影响，然后运行同一命令。
5. 刷新页面查看状态及展开的确认依据；只有完成运行核验的部署回执才能更新当前版本。

```bash
node scripts/forge-plugin-release/delivery/index.mjs run /srv/forge-delivery/delivery.json <任务UUID> --reviewed
```

执行器仅运行明确指定的任务，不自动扫描队列、自动升级或选择 latest。配置中的目标限制不会因页面切换而扩大。
每次 COS 写入和 Compose 切换前重新检查原审批；任务关闭、审批变化、凭证到期或租约失效都停止后续操作。
请求 ID 使页面提交重试幂等；已有 running/未知任务不能重复领取。

## 恢复和故障核查

正常恢复选择上一确认版本，必须在该目标曾完成 COS 发布，并确认旧应用兼容当前数据库。
恢复也重新读回制品、核对挂载和运行内容；不会删除 Flyway 历史或反向执行 SQL。

发起切换后的失败和失联保留为“结果待核查”，目标锁不自动过期接管。
即使收到未知结果回执，本地锁仍保留；收到回执不代表允许再次切换。
先停止执行器及其 Docker CLI 进程，检查实际容器、数据库迁移和备份，再在页面“人工核查”填写说明。
人工关闭保留原操作与核查审计，不把版本改成健康，也不终止进程。

目标有未核验版本时只允许恢复最后确认版本；没有任何成功版本的首次部署须人工处置，不伪造恢复依据。
已确认原任务人工关闭后，使用原任务 ID 归档遗留本地锁：

```bash
node scripts/forge-plugin-release/delivery/index.mjs recover-lock /srv/forge-delivery/delivery.json <原任务UUID> --reviewed
```

归档须再次通过独立控制面认证，检查锁 inode/nonce/任务/目标并保留 `closed-<任务ID>`；不提供 force 或自动删除。
随后创建恢复任务并执行。现场只能是最后确认版或该次未核验版，遇到其它项目/版本/挂载立即停止。
损坏 vault、缺失旧部署包、配置被改或真实备份不可用时，由部署方核查，不能强行跳过验证。

## 验证范围

代码包含真实 COS SDK、真实控制面 API 和固定 Compose 执行流程。
仓库自动化测试用独占 H2、协议夹具及本地文件验证状态机和安全负例；不会上传真实 COS 或操作部署主机。
上线前仍需执行客户 SSO、私有 COS 策略、MySQL 迁移、Linux rootless Compose、许可证和业务冒烟验收。
