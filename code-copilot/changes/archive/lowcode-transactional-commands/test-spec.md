# 低代码事务型业务命令测试规格

## 1. 发布态、输入与幂等

| ID | 场景 | 预期 |
|---|---|---|
| COMMAND-01 | 页面执行动作 | 只读取最新 PUBLISHED 快照，不读取对象草稿 |
| COMMAND-02 | 设计器预览 | 可读取草稿定义，但不执行步骤 |
| COMMAND-03 | 缺失/非法幂等键 | 副作用前拒绝 |
| COMMAND-04 | 同键同摘要成功重试 | 返回同一日志和结果，零重复副作用 |
| COMMAND-05 | 同键异摘要 | 返回稳定冲突错误，零副作用 |
| COMMAND-06 | 相同键跨发布版本 | 按动作版本隔离，不误命中旧版本 |
| INPUT-01 | inputSchema 声明字段 | 只保留声明字段并完成类型/必填/范围校验 |
| INPUT-02 | 未声明字段或危险身份键 | 失败关闭 |
| INPUT-03 | 浏览器 context.row/tenantId/userId | 不进入动作映射上下文 |
| INPUT-04 | SYSTEM userId/tenantId/activeOrgId | 只来自可信 Session/执行身份 |
| INPUT-05 | 显式空 inputSchema 与存量缺失 Schema | 空 Schema 拒绝任意输入；只有缺失 Schema 保留兼容语义 |

## 2. 事务与执行模式

| ID | 场景 | 预期 |
|---|---|---|
| TX-01 | LOCAL_TRANSACTION 两个本地步骤成功 | 同一事务提交 |
| TX-02 | 第二步失败 | 第一步回滚，日志终态 FAILED |
| TX-03 | LOCAL_TRANSACTION 含流程/消息/领域动作 | 保存或发布失败，运行时再次拒绝 |
| TX-04 | LOCAL_TRANSACTION 的 rollbackOnFailure=false | 发布和运行时拒绝 |
| TX-05 | LOCAL_TRANSACTION 指向外接数据源 | 副作用前拒绝，不宣称事务覆盖 |
| TX-06 | ORCHESTRATION 含非本地步骤 | 允许执行但结果明确为编排语义 |
| TX-07 | FOREACH 嵌套非法步骤 | 递归失败关闭 |

## 3. 条件更新与数值调整

| ID | 场景 | 预期 |
|---|---|---|
| UPDATE-01 | UPDATE_FIELD 期望状态命中 | 写入成功 |
| UPDATE-02 | 期望状态不命中/并发已改 | 单 SQL 影响 0 行，事务失败 |
| UPDATE-03 | 条件字段或写字段不在白名单 | 拒绝，不执行 SQL |
| ADJUST-01 | 两字段 ADD/SUBTRACT | 一条 UPDATE 原子调整全部字段 |
| ADJUST-02 | SUBTRACT 后低于 min | 影响 0 行，事务回滚 |
| ADJUST-03 | ADD 后高于 max | 影响 0 行，事务回滚 |
| ADJUST-04 | delta 非数值、字段重复、空调整 | 副作用前拒绝 |
| ADJUST-05 | 数据权限/租户/逻辑删除不匹配 | 影响 0 行，安全失败 |

## 4. 发布与前端

| ID | 场景 | 预期 |
|---|---|---|
| PUBLISH-01 | 合法本地事务动作 | 发布检查通过并固化协议 |
| PUBLISH-02 | 未知模式/步骤/输入类型 | 发布失败并返回具体路径 |
| PUBLISH-03 | URL/Header/认证/SQL/script/handler | 任意层级命中即拒绝 |
| UI-01 | 有 inputSchema 的手动动作 | 根据 Schema 展示结构化输入表单 |
| UI-02 | 必填/数值范围错误 | 浏览器阻止提交，服务端仍独立校验 |
| UI-03 | 用户确认并提交 | 生成 8～128 位幂等键，请求不携带整行 context |
| UI-04 | 同一次请求重试 | 复用原幂等键，不重新生成 |
| UI-05 | 本地/编排模式设计器 | 清晰显示事务范围，非法步骤不可选 |

## 5. 验证层级

1. JUnit/Mockito：BusinessAction 执行、输入协议、步骤策略和发布检查。
2. Repository/Service 契约：条件 SQL、数值调整 SQL、字段白名单和数据源边界。
3. Vitest：运行时输入、幂等键和设计器协议纯函数/组件。
4. JDK 17、`-Penable-tests` 下 generator 定向测试与相关模块聚合编译。
5. Node `v20.19.0` 下目标 ESLint、前端生产构建、`git diff --check` 和敏感配置静态扫描。

## 6. 部署环境补验

- 在真实 MySQL 事务中验证第二步失败后的数据回滚和并发条件更新；
- 多租户、不同数据权限角色验证目标记录不可越权更新；
- 真实浏览器验证双击、弱网超时和同请求重试只产生一次副作用；
- Flyway 在干净库与已存在旧唯一索引的升级库各执行一次；
- 本轮不启动真实服务、不修改真实数据库、Redis、流程和外部系统。

## 7. 本轮增量验证（2026-08-11）

- P0：发布态动作解析、版本化幂等、输入 Schema、危险上下文、本地事务步骤白名单、条件更新与原子数值调整。
- P0：`AiCrudPage` 输入表单、幂等键复用及最小请求载荷；设计器默认本地事务、非本地步骤禁用及三类结构化步骤。
- P1：V1.0.103 防重复迁移结构与本阶段 Flyway placeholder 扫描。
- 构建：generator 32 模块聚合安装、Node 20 前端生产构建、目标 ESLint、`git diff --check`。
- 环境补验继续保留第 6 节范围，不在本轮启动真实服务或修改数据库。
