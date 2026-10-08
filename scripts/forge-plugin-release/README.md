# 私有本地候选制品库

P3.3b1 将成功构建的 **Admin JAR/UI 实际文件** 封存为可复验的本地候选制品。
没有远程发布、部署、数据库迁移、启动/停止服务、任务回写或商业授权功能。
`--reviewed` 是本地操作者的声明，不是插件中心 `release_ready` 的实时认证。
生产 Web 服务不运行本工具，也不读取工作目录。P3.3b2a已加入当前审批的只读认证核验，
远程仓库、制品登记和目标部署仍未接入。

## 前置条件与配置

- Node >=20.19、POSIX、专用非 root 用户；本地文件系统，所有写者遵循同一发布锁。
- 使用已停止构建任务的 `job-*/result.json`；只支持 `built`、`deployed=false`、community、admin-build。
  原始报告、可信交付方、代码/权限/迁移影响须人工核查；文件摘要不能证明代码安全或源代码正确。
- 预创建 `vaultRoot` 并设置0700；`jobRoot` 同样由当前用户持有且不向组/其它用户开放。
  使用 `realpath` 得到的绝对路径，不接受软链接路径或 `..`、根/用户主目录，两根不能重叠。
- 从受核查原始 `result.json` 的文件字节计算 `resultSha256`（如 `shasum -a 256`）。
  **它不是服务端任务详情的 resultSha256**：后者是机器回写报告的摘要，两种文件协议不能混用。
  `artifactManifestSha256` 与已有 worker 的 `path:bytes:sha256\n` 算法一致，供后续桥接使用。

`release.json` 严格五个字段，无凭证、URL、命令或环境变量替换：

```json
{
  "protocolVersion": 1,
  "repositoryId": "local-candidates",
  "vaultRoot": "/absolute/private/vault",
  "jobRoot": "/absolute/private/workspace/job-xxxxxx",
  "resultSha256": "替换成原始result.json的64位小写SHA256"
}
```

独立离线核验用 `vault.json`，不再依赖原工作区：

```json
{
  "protocolVersion": 1,
  "repositoryId": "local-candidates",
  "vaultRoot": "/absolute/private/vault"
}
```

```bash
node scripts/forge-plugin-release/index.mjs check /absolute/release.json
node scripts/forge-plugin-release/index.mjs publish /absolute/release.json --reviewed
node scripts/forge-plugin-release/index.mjs verify /absolute/vault.json rel-<64位清单摘要>
```

根 package.json 也提供 `forge:plugin-release`，生成工程会原字节携带工具和共享协议。
`check` 和 `verify` 不写制品库；publish 没有 force/更新/删除/latest 命令。

## 封存和复验

重新扫描产物，校验文件名/目录/链接、JAR签名、单文件/总量及实际SHA，并与报告逐项核对。
沿用构建器限额：4096文件、每文件512MiB、总1GiB；配置16KiB，报告/清单8MiB。
成功候选目录为 `vaultRoot/rel-<规范manifest.json字节SHA256>/`：

```text
rel-<sha256>/
├── manifest.json       # 固定源码/包/镜像/插件/核心版本、原始报告及实际产物摘要
├── receipt.json        # 本地封存时间与本地审查声明，无认证身份
└── artifacts/
    ├── backend/admin.jar  # 有server时
    └── frontend/...       # 有ui时，必须有index.html
```

只复制上述产物，不复制源码、ZIP、控制配置、凭证或日志。
流式复制并重新散列，复制前后核查源文件状态；最终再次复验实际副本。
文件0400/目录0500是工具只读约定，不是OS不可变标记/数字签名，授权管理员仍可改写。
清单内容寻址检测构建元数据与产物变化；本地回执只校验格式与绑定声明，封存时间并无签名认证。

私有 vault 独占锁防止两个发布者覆盖。macOS 重命名目录要求源顶层可写，因此提交时仅顶层
短暂0700、子目录/文件仍只读；重命名后收敛0500并再次复验才报告 sealed。
中断或崩溃后的未完成目录不能通过 verify。已有相同ID必须全部核验成功才报告 already_sealed；
损坏不自动修复、不覆盖。重试还会核对原工作区；只检查已封存制品使用独立 verify。

CLI 输出摘要和固定部署准备清单，不输出私有根目录/原报告。
离线结果及封存回执的`deployed=false`、`liveTaskApprovalVerified=false`始终保留，
不能把 sealed/verified 当成服务端已发布。
只有 `artifact_integrity` 通过；当前任务授权、目标/权限、备份恢复、迁移资源、部署运行健康均为 pending。

## 当前任务审批认证核验（P3.3b2a）

从插件中心任务详情取得**当前revision、approve_build审查记录ID、execution.resultSha256**。
后者填写serverResultSha256，不填写原result.json或manifest的文件SHA。
核验只接受原构建机器、同租户、已结束且release_ready的任务；关闭/旧审批/报告不一致即拒绝。
沿用默认关闭的`forge.plugin-build.worker`配置及专用HTTPS认证，启用前须人工安全审查。
不申请部署权限，不使用用户Token或X-Inner-Call；Bearer只从FORGE_PLUGIN_WORKER_TOKEN读取，
不得写进配置/命令行/日志。不要把凭证复制给浏览器、容器或其它用户。

approval.json严格九个字段（无命令/用户/租户/凭证）：

```json
{
  "protocolVersion": 1,
  "repositoryId": "local-candidates",
  "vaultRoot": "/absolute/private/vault",
  "apiBaseUrl": "https://your-admin.example/forge",
  "taskId": "替换成任务UUID",
  "revision": 4,
  "reviewId": "替换成当前approve_build审批UUID",
  "serverResultSha256": "替换成服务端execution.resultSha256",
  "workerId": "替换成原构建机器标识"
}
```

```bash
node scripts/forge-plugin-release/index.mjs verify-approval /absolute/approval.json rel-<64位清单摘要>
```

先复验实际快照，再单次POST选定服务/任务，最后重新复验文件，防止网络往返期间改写。
返回须绑定随机checkId、期望任务revision/审批/机器/报告/清单SHA；不接受旧缓存响应。
请求3秒超时/响应64KiB有界、不重定向；服务端与执行器时钟需同步（允许5秒偏差）。
核验成功仅本次输出liveTaskApprovalVerified=true与checkedAt、validOnlyAtCheck=true，
原封存回执不改、不落新的长期成功标志；下次操作须重新核对当前审批。
服务端仅核对保存报告的元数据，实际文件由本地CLI复验，serverArtifactBytesVerified=false。
结果不是部署凭证、签名或后续事务授权；deployed=false、deploymentAuthorized=false，
目标/备份/迁移/部署及健康仍pending。当前审批通过不会登记制品、释放任务占用或宣称已安装。

## 失败、保留和恢复边界

- 参数/摘要/链接/权限错误失败关闭；未知文件异常只输出 RELEASE_FAILED，不出内部路径。
- 锁冲突输出 VAULT_BUSY，不抢占；仅成功核对本进程 nonce/inode 的锁会由本进程移除。
- 本工具不自动删除任何产物。失败的 `.pending-*` 保留用于核查；崩溃可能留下 `.publish-lock`。
  运维确认没有活跃发布者后，核对这些**具体目录**再手工移出隔离/保留区，不批量删库或清空 vault。
- commit 后通信/文件系统同步异常可能已经存在最终目录，先用 verify 核对，不能盲目重新发布。
  非只读/损坏目录必须人工调查并移出隔离；不得修改旧快照来冒充同一候选制品。
- 容量与保留策略由运维管理。本阶段无垃圾收集器、远程存储凭证/上传或自动回退。
- 真实HTTPS/任务认证、rootless离线镜像、MySQL/Flyway及目标服务/UI健康验收仍须单独执行。
