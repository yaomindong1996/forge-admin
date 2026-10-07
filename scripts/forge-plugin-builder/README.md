# Forge 离线插件构建执行器

P3.1 只做**源码副本预检和离线构建**。不读取插件中心任务、不修改原工程、不连接数据库，
不运行服务/迁移、不部署或证明插件健康；Web 任务领取/租约/回写为 P3.2，受控部署为 P3.3。
该工具连同原安装器一并复制到新生成的 Admin 工程，无第三方运行时依赖。

## 准备受控工作区

使用专用 Linux 非 root 构建用户和独立 rootless Docker daemon，不能复用生产 Docker。
容器内 UID 0 映射为该非 root 宿主用户；不是 rootful daemon 的 root 容器。
宿主只安装 Node20.19+、Git（`/usr/bin/git`）、Docker CLI，不安装并执行上传包的构建依赖。
协调器为 POSIX 实现，不支持 Windows、Docker Desktop/rootful、远程 TCP/context。

- `sourceRoot` 是审查后的完整 Admin+UI Git 仓库根目录；HEAD 必须等于固定 40 位 commit。
  工作区/暂存区必须干净，未跟踪文件也拒绝；忽略的本地覆盖不复制。
  已跟踪软/硬链接、子模块、`.forge-plugin` 备份、本地配置、密钥文件、构建产物均阻断。
  合法公开 `.env` 和环境模板可复制，但配置内容仍需人工检查，没有自动 Secret 内容扫描。
  没有 Git 仓库、开发接入插件、缺失已登记插件源码的快照不能自动构建。
  固定快照视图只含后端根、Admin UI、根包管理配置及模块 catalog；不带 Report/H5 UI、
  Docker/文档及历史 Admin `dist.zip`。结果声明 `scope=admin-build` 和排除数量。
  不支持依赖视图之外的自定义工作区；需要先独立设计构建协议，不能任意挂载生产目录兜底。
- `workspaceRoot` 必须是与源码完全分离、当前用户拥有的 0700 专用目录。
  每次生成 `job-*` 独占子目录，拒绝复用输出；不要放在生产配置或其它 Git 工程内。
- `packageFile` 必须是审查过的本地 ZIP，明确 SHA-256；沿用源码 CLI 格式和有界 ZIP 校验。
  离线 CLI 限额为32MiB包/16MiB单文件/128MiB展开/10000条目；Web上传仍执行其更小限额，
  离线工具不会放宽 Web 接口，也不会读取 Web BLOB。
  当前只支持 community 包，不校验收费许可证，不执行未确认的 Pro 工程方案。
- `dockerSocket` 必须是当前用户拥有的真实本地 Unix socket，daemon 必须报告 rootless。
  socket 直接父目录也必须为当前用户的私有目录；daemon 必须为 cgroup v2/systemd，
  并报告 memory/swap/CPU quota/pids 控制可用，否则阻断，不接受会忽略限额的环境。
  CLI 总是显式 `--host unix://...`，不继承 Docker context/登录配置/数据库环境。
- `image` 必须是提前人工准备、审查并加载本地的完整 `name@sha256:...`，不能填 tag。
  执行器不会自动拉镜像、下载依赖或在宿主降级构建。隔离不能替代源码、POM、SQL 审查。

## 镜像固定接口

镜像由运维在独立环境准备，记录镜像 digest 和离线缓存版本，不由上传包指定 Dockerfile。
需要提供：

| 路径 | 内容 |
| --- | --- |
| `/usr/local/bin/node` | Node20.19+ |
| `/opt/forge/java` | Java17；`/opt/forge/bin/java` 或 PATH 中的 java 指向该 JDK |
| `/opt/forge/bin/mvn` | 宿主兼容的 Maven（当前测试基线 3.9.11） |
| `/opt/forge/bin/pnpm` | 与宿主锁文件兼容的 pnpm |
| `/opt/forge/cache/maven` | 已预热的公开 Maven 缓存；不能带 settings.xml/服务器认证 |
| `/opt/forge/cache/pnpm` | 针对 Linux/对应架构预热的公开 pnpm store，无认证/私有配置 |

构建在 tmpfs 内复制缓存。Maven 固定离线安装真实宿主 BOM 后 `-pl <Admin> -am package -DskipTests`；
pnpm 固定 `--offline --frozen-lockfile --ignore-scripts`，再直接调用 Vite build。
不能依赖 install lifecycle 下载/编译原生依赖；需在审查镜像/公开离线缓存中准备好，缺失时失败。
不以发布构建替代测试；自动测试由另一个明确启用测试的受控流水线负责。

容器 `--network=none --pull=never --read-only --cap-drop=ALL --security-opt=no-new-privileges`，
4GiB 内存、2CPU、256 PIDs、6GiB `/work` tmpfs、256MiB `/tmp`；总超时20分钟。
只挂本次只读 source/ZIP/control 和独占 artifacts 输出，不挂 socket、生产目录、凭据。
构建源码仍会执行 Maven/Vite 中的代码，故仅用于可信审查包，不能宣称是恶意代码沙箱。
rootless 资源约束须满足 cgroup v2/systemd 及控制器委派，见
[Docker 官方说明](https://docs.docker.com/engine/security/rootless/tips/#limiting-resources)。

## 配置和命令

在专用工作目录创建本地 `builder.json`，所有字段必填；不能添加命令、环境变量或 credentials。
下面摘要是不可用占位符，必须换成实际审查包/commit/镜像的值，不可照抄执行：

```json
{
  "sourceRoot": "/srv/forge-build/source",
  "commit": "0000000000000000000000000000000000000000",
  "packageFile": "/srv/forge-build/input/plugin.zip",
  "packageSha256": "0000000000000000000000000000000000000000000000000000000000000000",
  "workspaceRoot": "/srv/forge-build/jobs",
  "dockerExecutable": "/usr/bin/docker",
  "dockerSocket": "/run/user/1000/docker.sock",
  "image": "forge-builder@sha256:0000000000000000000000000000000000000000000000000000000000000000"
}
```

```sh
node scripts/forge-plugin-builder/index.mjs check /srv/forge-build/builder.json
node scripts/forge-plugin-builder/index.mjs run /srv/forge-build/builder.json --reviewed
```

根 `package.json` 也提供 `forge:plugin-build`，可通过宿主 pnpm 调用；Node 直接执行不触发依赖安装。
只读源码检查也可单独使用 `node scripts/forge-plugin/index.mjs check /absolute/plugin.zip`。
已有插件整包替换必须显式 `--force`，run 的 `--reviewed` 表示操作者已审查源码/包/镜像。
P3.1 比交互式 CLI 更严格：已提交的定制也拒绝自动覆盖；请人工维护差异，不删标记绕过。

## 结果、失败与保留

`workspaceRoot/job-*/result.json` 与 stdout 一致，退出0为 checked/built，失败退出1。
包含协议版本、jobId、包摘要、镜像摘要、源码 commit/摘要、真实预检落点及产物大小/SHA-256，
不包含源码绝对路径或原始编译日志。`deployed` 固定 false，结果文件不是授权证明或签名回执。
build 成功文件为 `artifacts/backend/admin.jar` 和/或 `artifacts/frontend/*`；不打包/部署它们。
源码/ZIP/preflight 保留在同一私有 job 内，原工程保持不变；本阶段不产出新的源码 Git commit。

- `failurePhase` 区分 source_snapshot/package_preflight/source_preflight/container_build/artifact_verification。
- `SOURCE_DIRTY`/`SOURCE_COMMIT_MISMATCH`：核对受审查提交，不能自动提交或丢弃客户修改。
- `PACKAGE_DIGEST_MISMATCH`：停止，重新核对交付字节；不能自动改 SHA 来重试。
- `PREFLIGHT_FAILED`：该阶段宿主协议/路径/所有权失败，可对审查源副本运行原 CLI check 查看细节。
- `DOCKER_SOCKET_UNAVAILABLE`/`ROOTLESS_REQUIRED`/`ROOTLESS_RESOURCE_LIMITS_UNAVAILABLE`/
  `LOCAL_IMAGE_DIGEST_MISMATCH`：修正受控执行环境，
  不改成 rootful/远程/可变镜像，不开放网络绕过离线缓存。
- `COMMAND_FAILED`/`COMMAND_TIMEOUT`/`OUTPUT_LIMIT`/`INTERRUPTED`：本次失败，不上传原始编译日志；
  在专用审查环境复现具体构建，不向 Web 暴露源代码/凭据。
- `CONTAINER_CLEANUP_UNVERIFIED`：不得按成功处理。管理员检查本次 `forge-build-*` 容器和
  `forge.plugin-builder.owner` 标签后人工清理；本次名称/标签值保存在私有 `container.json`。
  不批量删除其它容器。普通失败只清理本次匹配容器；硬断电/SIGKILL 仍需按此回执人工核对。

单次源码最多30000文件/64MiB单文件/512MiB总量；产物4096文件/512MiB单文件/1GiB总量。
进程输出上限1MiB（Git tree 8MiB），超限立即失败。所有 job 保留，不自动递归删除用户数据。
运维应为专用目录设置磁盘配额、并发上限与保留周期；人工清理前保留摘要/审计及必要恢复输入。
共享数据库迁移、Web 租约绑定、产物仓库、自动保留策略和受控部署不在 P3.1 范围。

## 当前验证边界

已测试 Git/ZIP/原 CLI 的真实文件预检、离线命令规划、Docker 参数和异常清理桩、产物文件校验，
并对新生成 full 工程回归。开发机没有 Docker，尚未实跑 rootless 容器/镜像离线构建。
上线前必须单独验收镜像/缓存/资源限制、网络阻断、信号清理及有效 Admin JAR/UI 产物。
