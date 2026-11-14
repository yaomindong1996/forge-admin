# 执行记录：H5 支持发起低代码「业务流程」

## 2026-11-13 阶段一编码与验证

- 版本：未发版，变更计入 `CHANGELOG.md` Unreleased。
- 状态：代码已完成并自测，待提交 / 待部署（演示环境验证使用本地 H5 dev server 代理演示网关）。

### 自动化验证

| 命令 | 结果 |
|------|------|
| `cd forge-h5-ui && node --test`（Node 24.21） | 207 条，204 通过，3 失败 |
| 同命令，暂存本次改动后（基线） | 200 条，197 通过，3 失败（同 3 条） |
| `npx uni build --outDir /tmp/h5-build-check` | Build complete |

失败的 3 条为存量契约测试，与本次改动无关：
`mobile registry covers every component type exposed by the admin designer`、
`mobile field contract classifies every property exposed by the PC designer`、
`mobile layout contract classifies every property exposed by the PC designer`。

新增用例：业务流程动作收集、去重与已发起隐藏、权限与显示条件、运行失败原因解析、
store 合并主流程/业务流程按钮、非单据对象保留流程状态、列表行操作排除 `START_PROCESS`。

### 演示环境浏览器验证（ego-browser，H5 `localhost:3009` → 演示 H5 网关）

应用「费用报销E2E」（`hr_e2_e`），业务流程 `approval_qsxf`（报销审批），对象 `apply_kdcp`。

1. 单据 1 详情页底部出现业务流程按钮「手动开始」；确认弹窗「确认发起「手动开始」吗？」。
2. `start-config` 返回 `initiatorSelectNodes: []`，跳过选人；`POST start` 创建运行
   `2121250129455792129`；审批节点失败：「当前单据已有主流程实例，不能重复发起」（单据 1 已走过对象主流程）。
   失败运行不计入已发起，按钮保留；再次点击由服务端重试同一运行（未新增运行），状态变为 PENDING 后按钮隐藏。
3. 单据 2：首次发起由后端返回业务错误，页面如实提示失败原因；重试后提示「业务流程已启动」，按钮隐藏。
4. 刷新防闪烁：发起过程 543 次 DOM 采样，状态卡缺失 0 次。

### 未完成 / 跳过

- 完整审批闭环（H5 新建 → 业务流程审批 → 已通过）未走通：演示数据源已按约定恢复只读，无法新建单据；
  存量单据均已有对象主流程实例。需可写数据源或无主流程的新单据再验收 V1。
- V4 无权限账号未实测，仅单测覆盖。
- 演示环境新增的运行数据：单据 1、2 各一条 `approval_qsxf` 运行记录（PENDING，重试中）。

## 2026-11-13 完整闭环验收（V1）与阶段二编码

### V1 闭环（演示环境，已部署后端 + 本地 H5）

1. 管理端「应用数据源」临时打开「写数据」（原值关闭；改表结构保持关闭）。
2. H5 新建报销单 3（差旅费 860 元，发生日期默认今天）→ 详情页同时显示「提交审批」「手动开始」。
3. 「手动开始」→ 确认 → 无自选审批人 → 启动成功，运行 `2121260908859015169` 状态 WAITING，
   单据「流程中」，按钮切换为「去审批」「撤回」。
4. 「去审批」→ 待办详情 → 常用语「同意」→ 确认同意 →「同意成功」，自动回到单据详情。
5. 单据「已通过」；运行 SUCCESS（手动开始、审批流程、完成均 SUCCESS）；「我发起的」搜索「报销」可见已通过记录。
6. 数据源「写数据」已关闭，刷新确认：默认 true、只读 false、写数据 false、改表结构 false、启用 true。

观察到但未处理：审批详情常用语出现两个「同意」；审批节点默认允许审批人改字段（节点表单权限配置）；
「我发起的」默认列表把较早的进行中流程排在新完成流程之前。

### 阶段二自动化验证

| 命令 | 结果 |
|------|------|
| `mvn -o -Penable-tests -pl …/forge-plugin-generator -am test -Dtest='BusinessStartableObjectServiceTest,BusinessStartableObjectMapperContractTest,BusinessProcessRuntimeActionCompilerTest'` | 16 条全部通过 |
| `cd forge-h5-ui && node --test` | 210 条，207 通过，3 失败（同前述存量契约测试） |
| `npx uni build --outDir /tmp/h5-build-check` | Build complete |

浏览器（本地 H5，未部署后端改动）：直接打开 `mode=create&process=approval_qsxf`，页脚为「取消 / 保存 / 手动开始」；
空表单发起提示「请完善必填字段」；填写后确认弹窗「将先保存当前填写内容，再发起「手动开始」」，取消后无写入。
目录接口返回业务流程条目需部署后端后验收（V5）。

## 2026-11-14 演示环境部署准备（待执行）

- 用户选择部署演示环境。演示入口 51890 只承载后端 API（前端用本地 dev server 连接），本次只更新 Admin 与 App。
- 部署前线上版本：Admin `81c9e011e`（11-13 17:04 发布）；App（H5 API）`08b71ae75`（10-19 起未更新）。
  `81c9e011e..ed06cfac5` 仅本日 4 个提交，无 Flyway 脚本变化；App 将从 10-19 跨到最新（App 模块自身仅配置迁移，不执行迁移）。
- 独立 worktree `/private/tmp/forge-demo-release-ed06cfac5` 构建（`-DskipTests`，联网补齐 postgresql 42.7.13），
  build-info：1.2.1 / `ed06cfac54bdfcab840e3fcd98b68cab8f7c5255` / `2026-11-13T16:13:33.503Z`。
  - admin SHA256 `977a7bc8451ea1b60937e447d7b8466b7876f516fdd5bd09206c65822f363fab`
  - app SHA256 `83196c2a72daeb5f3b06f6638ac2e19ed724195e798ad35354b6712593c76639`
- 已上传至 `~/forge-upload/20261114-admin-app-ed06cfac5/`（含 `deploy.sh`、`SHA256SUMS`），目标机校验通过。
- 目标 Docker 需 root，sudo 需用户本人输入密码；`deploy.sh` 只替换 admin/app 镜像，备份 compose，
  任一服务 10 分钟未 healthy 即恢复备份并重启原镜像。

## 2026-11-14 09:17 演示环境部署（已完成）

- 11-14 凌晨 grokbot 部署的是 ForgePro studio-test（`~/forgepro-upload/*-studio-test`），未涉及 `/opt/forge-backends`。
- 用户授权提供 sudo 后执行 `deploy.sh`：替换前 admin `1.2.1-81c9e011e-r170407`、app `1.2.1-08b71ae75-fonts1`；
  替换后均为 `1.2.1-ed06cfac5`，`DEPLOY_OK`。compose 备份
  `/opt/forge-backends/backups/compose-20261114-091721-before-20261114-admin-app-ed06cfac5.yaml`（回滚：恢复该文件后 `up -d --no-deps admin app`）。
- 新旧镜像层一致（同 Dockerfile、同运维 JAR、同字体 8 个 truetype、中文字体 0），仅应用 JAR 不同。
- 启动：admin 52.9s、app 40.1s，restartCount 0；均有 API-only 标记、无 Scheduler started；
  Flyway 校验 233 个迁移，`forge_admin_new` 1.0.234 up to date，未执行迁移。`PropertiesMigrationListener` 报告为 Boot4 配置迁移提示（10 月已记录）。
- 功能验收（V5、按钮文案）待登录：管理端与 H5 登录均需验证码，已交由用户在浏览器登录。
