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
