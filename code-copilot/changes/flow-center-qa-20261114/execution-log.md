# 执行记录：流程管理 / 流程编排全量巡检（2026-11-14）

- 范围：流程中心（模型、BPMN 设计器、版本历史/对比、分类、表达式、监控、用户组、常用意见）、
  我的待办/已办/发起/抄送、应用业务流程（列表、编排设计器、运行记录、复制/停用/删除）。
- 环境：本地 admin-ui dev server（最新代码）→ 演示网关；浏览器 ego-browser，账号 admin。
- 版本：未发版，计入 CHANGELOG Unreleased；问题清单与根因见 `findings.md`。

## 环境处理

- 演示 Flow 服务停在 `08b71ae75`，与最新前端不匹配（发布信息字段缺失）；部署 Flow 至 `ed06cfac5`。
- 部署回归：Admin/App/Flow 新镜像误用无字体库基础镜像，发布流程 500（`libharfbuzz.so.0`）；
  改用 `forge-local-runtime:ubuntu24-java17-fonts-20261019` 重建为 `1.2.1-ed06cfac5-fonts`，与原线上镜像字体库一致（6 个）。

## 自动化验证

| 命令 | 结果 |
|------|------|
| `vitest run src/components/flow-designer src/views/flow src/components/flow` | 72 文件 / 547 条全部通过 |
| `vitest run src/components/business-process-designer` | 57 条全部通过 |
| `vitest run src/views/workspace/__tests__/WorkspaceSummaryMetrics.spec.js` | 3 条通过（去掉修复时 2 条失败） |
| `vitest run`（全量） | 2916 条，69 失败；同环境暂存本次改动基线 73 失败（本次修复 4 条存量失败，未新增） |
| `mvn -Penable-tests -pl forge-plugin-flow,forge-flow-server -am test`（流程包全部） | 17 + 206 + 43 条全部通过 |
| `mvn -Penable-tests ... -Dtest='BusinessProcess*Test,BusinessStartableObject*Test,...'` | 98 条全部通过 |
| `FlowModelPublicationDictionaryMigrationTest` | 修正迁移目录后通过 |

全量前端存量失败集中在 `AiFormItem`、`printRuntimeContext` 等非流程用例，昨日基线为 4 条，今日环境升至 73 条，待单独排查。

## 浏览器验证（修复后）

- 设计器通知模板预览正常显示；版本详情只读图结束圆点居中，「自动布局」后重新保存坐标一致；发布 v1/v2、版本对比正常。
- 「发起测试」→「我发起的」自动按标题筛出 1 条；待办同意 →「审批通过 1 条」。
- 分类新增/子级/编辑/停用/启用；用户组新增/成员/删除；表达式、常用意见新增/删除；业务流程新建/复制/停用/删除、取消运行。
- 删除有历史数据的流程模型：仅提示一次具体原因。
- 后端修复（常用语去重、分类管理含停用、运行详情名称与顺序）需部署后验证，见下节。

## 测试数据

- 保留：流程模型「QA流程测试1114」（有 1 条历史实例，按设计不可删除）。
- 已删除：模型副本、业务流程 QA编排测试1114 及副本、QA 表达式、QA 用户组、QA 常用意见。
- 已取消：单据 1/2 的 `approval_qsxf` 测试运行。
- 已清理：分类「QA分类1114改」及子分类（部署修复后在管理页可见，已删除）。

## 部署与线上验证（f33f7d6a1）

- 提交 `f33f7d6a1` 已推送；独立 worktree 构建 Admin/App/Flow（`-DskipTests`），build-info commit 均为 `f33f7d6a1`。
- `~/forge-upload/20261114-admin-app-flow-f33f7d6a1/deploy.sh`：固定 `FROM forge-local-runtime:ubuntu24-java17-fonts-20261019`，
  构建后校验字体库（libharfbuzz/libfreetype/liblcms2 共 6 个）不足即中止；compose 备份后替换三服务，任一不健康即回滚。
- 结果：三服务 `1.2.1-f33f7d6a1-fonts` healthy，字体库 6/6，Scheduler started 0，Flyway `forge_admin_new` up to date（无迁移）。
- 线上验证：可用常用语「批阅、同意、已阅、情况属实」不再重复；分类管理树含停用分类、选择树仅启用；
  业务流程运行详情显示「报销审批」，节点顺序 手动开始 → 审批流程 → 完成；Admin `/system/version` commit `f33f7d6a1`。

## 踩坑

- 演示环境 Java 镜像必须基于 `forge-local-runtime:ubuntu24-java17-fonts-20261019`；无字体库基础镜像会在 Flowable 部署生成流程图时
  `UnsatisfiedLinkError: libharfbuzz.so.0`，表现为「发布流程」500。旧标签 `-fonts1` 后缀即指该基础镜像，不能只比对 Dockerfile 末尾层。
- 质量门禁：本机 `quality-gate.py check` 报 800 条 stale 基线条目（改动前即 799 条，疑与生成基线环境差异有关），未执行 prune，待人工确认。

## 抄送节点发布与审批操作（a62ef91c0 起）

- Flow `a62ef91c0`：plugin-flow 210、flow-server 43、client 17 测试通过；字体基础镜像部署 `1.2.1-a62ef91c0-fonts` healthy，字体库 6/6。
- 线上：「QA审批操作1114」（含抄送人节点）发布返回「部署成功」，「发起测试」成功，待办出现在审批A。
- 动态加签 test33：返回「加签成功」，`/api/flow/task/{taskId}/sign-relations` 返回 `targetUserName=test33`，页面却显示 70 → 修复 started.vue。
- `npx vitest run src/views/flow/utils/__tests__/signRelations.spec.js`：2 passed；eslint 通过；`pnpm check:edition` 通过；
  `pnpm quality:check` 无新增发现（仍为既有 800 条 stale 基线，未 prune）。
- 演示 Admin 会话中途失效被登出（验证码登录需人工），减签/转办/驳回/撤回/抄送的浏览器验证待重新登录后继续。
- 重新登录后继续：加签记录显示 test33 ✓；减签只列 test33，「减签成功」后记录变「已撤回」✓；改派 test33 / 改回 admin「当前任务已改派」✓；
  转办 test33「转办成功」，时间轴显示 test33 + 转办说明 ✓。
- 改派回 admin 后同意报「任务动作幂等冲突」→ 修复 FlowTaskActionAuthorization；plugin-flow 213、flow-server 43 通过；部署 Flow `1.2.1-150bdb5c8-fonts` healthy、字体库 6/6；
  同一任务重试「审批通过」✓。审批B 驳回弹窗仅列「审批A」，「已退回」后回到审批A ✓。
- 发起时间修复：`npx vitest run src/views/flow` 12 文件 53 用例通过；浏览器待办详情显示 2026-11-14 14:29:39 ✓。
- 审批A→B 同意后抄送节点生效：「我的抄送」出现「QA审批操作1114-测试发起」未读 ✓，「已读」后状态变已读 ✓，但顶部「未读抄送」卡仍为 1（刷新后 0）→ 修复。
- 撤回：新实例在审批A「撤回成功」✓。指标卡同步修复后：待办页同意审批A，「本周已办」13→14 无需刷新 ✓。
  `npx vitest run src/views/workspace src/stores ...` 14 文件 122 用例通过。
- 分支：QA分支1114（条件分支，分支1 `${1 == 2}`）发起后进入默认「分支2审批」并审批通过 ✓；QA并行1114 同时生成分支1/2 两条待办，
  审批一条后另一条仍在，两条都通过后流程结束 ✓；工作台 summary「发起中」按实例计数后 35（原 65 左右）✓。
- 包容分支修复后：QA包容1114 分支1 `${1 == 2}` 发布「部署成功」，发起后只生成默认「分支2审批」✓（修复前分支1无条件会必走）。
  `npx vitest run src/components/flow-designer` 57 文件 491 用例通过（6 个 unhandled errors 为既有，stash 后同样 6 个）。
- 流程监控（测试实例 QA审批操作1114）：挂起「流程已挂起」后回退/转派提示先激活 ✓；激活 ✓；节点回退到审批A「流程已回退」待办回到审批A ✓；
  终止「流程已终止」✓；删除流程数据「流程数据已删除」列表与任务行消失 ✓。
- 版本：QA包容1114 发布 v2（改名「默认分支审批」）→ 回退到 v1 生成 v3「回退成功」，新发起实例节点为「分支2审批」✓；清理历史（保留 1）返回已清理 0（release 版本受保护）。
- 修复后：首屏加载中输入「QA并行1114」立即点查询 →「匹配 1 个流程模型」✓；版本历史 v3（当前）无回退按钮 ✓；监控节点回退在仅有当前节点的实例上显示「无数据」✓。
  `npx vitest run src/views/flow` 12 文件 55 用例通过。
- 业务流程触发（费用报销E2E）：新建「QA事件触发1114」开始节点选「记录创建后」→ 发布 V1；临时开启应用数据源「写数据」，PC 运行态新增报销单
  「新增成功」，运行记录出现「QA事件触发1114 记录 4 执行成功」✓。「QA定时触发1114」选「定时扫描」日期字段=发生日期，发布 V1 ✓
  （演示节点调度关闭、运行账号未配置，未实际触发）。应用发布：发布检查 0 阻断 2 提醒，「应用 v2 发布成功」✓。
- 应用数据源「写数据」已关回（服务端读回 false）。
- 设计器修复后：开始节点名自动变「事件触发/定时触发」✓，过滤条件文案正确 ✓，日期字段仅「发生日期/创建时间/更新时间」✓；
  `npx vitest run src/components/business-process-designer` 60 用例通过。
- 产品确认后（我发起的按申请、版本清理只保护当前/运行中）：plugin-flow 217、flow-server 43 通过；部署 Flow `1.2.1-21f21fa75-fonts` healthy、字体库 6/6。
  线上：「我发起的」QA审批操作1114 由 5 行变 2 行（每实例一条）、QA并行1114 由 2 行变 1 行，全部发起总数 188 ✓；
  QA包容1114 清理历史（保留 1）「已清理 2 个版本」，仅剩当前 v3 ✓。
- H5（:3009，admin + 固定验证码登录）：「发起审批」显示「暂无可发起的审批」，接口 startable-objects 正常返回，原因是应用 v2 发布后页面菜单 visible=0
  被导航过滤 → 修复后列出「费用报销E2E：报销申请、报销审批」✓（事件/定时触发流程不在目录，符合预期）。`node --test` mobile-menu/startable-objects 12 通过。
- H5「我发起的」每申请一张卡片 ✓；对运行中 QA包容1114 点「催办」提示「已催办」并进入冷却 ✓。卡片时间为当前节点时间 → 补 processStartTime，plugin-flow 217 通过。
- H5 待办（QA审批操作1114 新实例）：更多→加签 test33「加签成功」，加签人员区显示 test33 ✓；减签「减签成功」✓；同意审批A ✓；
  审批B 驳回 → 选择驳回节点仅「审批A」→ 回到审批A ✓（修复前需二次确认「确认退回」，修复后单次确认 ✓）；节点进度顺序修复后为 发起人→审批A→审批B→抄送人→结束 ✓。
  `node --test` utils/store：207 用例 205 通过，2 个失败为既有 mobile-field-contract（与本次无关）；同步更新 console-design-system 对提交时间表达式的断言。
