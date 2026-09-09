# 低代码子表行动作与父子可信上下文执行日志

## 1. 基线

- 日期：2026-08-11
- 状态：阶段 5 已完成
- 前置阶段：外部连接器、统一查询源、字段事件和事务型业务命令已完成。
- 用户授权：按既定七阶段路线连续实施，无需阶段间再次确认。
- 工作区已有改动全部保留，不覆盖预售原型/PRD、`.DS_Store`、输出文档及前四阶段改动。
- 本阶段不启动真实服务，不修改真实数据库、Redis、流程或外部系统运行态。

## 2. 差距审计

- 已有：主子表表单、权威父详情读取、BusinessAction 发布快照、输入 Schema、权限、幂等、事务步骤和执行日志。
- 缺口：动作位置仅 TOOLBAR/ROW/DETAIL；子表操作列只有删除；执行请求没有父 ID、子 ID 和关系键；动作上下文没有 parentRecord；发布运行配置不投影子行动作。
- 结论：复用 BusinessAction 和 masterDetailConfig，新增 CHILD_ROW 协议，不建设专用业务服务。

## 3. 执行记录

| 时间 | 动作 | 结果 |
|---|---|---|
| 2026-08-11 | 审计子表编辑器、关系设计器、发布快照、动作执行和主子详情读取 | 完成 |
| 2026-08-11 | 固定 CHILD_ROW、relationKey、父子最小请求和可信上下文协议 | 完成 |
| 2026-08-11 | 建立第五阶段四份 SDD 文档 | 完成 |

## 4. 验证记录

### 2026-08-11 阶段收尾

- 后端 JDK 17 定向测试：`BusinessActionExecutionServiceTest`、`BusinessObjectPublishServiceCommandTest`、`LowcodeRuntimeConfigBuilderTest`、`BusinessActionCommandPolicyTest` 共 26 tests，0 failures，0 errors，BUILD SUCCESS。
- 前端 Node v20.19.0 Vitest：运行时与设计器协议测试共 10 tests，全部通过。
- 前端目标 ESLint：本阶段新增/修改目标文件通过；`BusinessRelationDesigner.vue` 保留文件既有的缩进与 `no-use-before-define` 报告，未扩大修改范围。
- 前端生产构建：`NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 成功，输出仅有既有动态导入、CSS 注释和 chunk size 警告。
- `git diff --check` 通过。

本轮未启动服务、数据库、Redis、Flow 或外部系统，没有需要清理的 PID。

## 5. 警告与跳过项

- 不处理工作区中与本阶段无关的用户改动。
- 真实 MySQL、多租户、数据权限和浏览器弱网 E2E 留给部署环境补验。
