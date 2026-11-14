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
- 待清理：分类「QA分类1114改」及子分类（停用后在旧版本 Flow 中不可见，部署修复后清理）。
