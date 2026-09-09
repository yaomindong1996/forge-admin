# 低代码统一字段查询事件执行日志

## 1. 基线

- 日期：2026-08-10
- 状态：阶段 3 已完成
- 前置变更：`lowcode-query-source` 已完成，external 19、data 5、generator 5 个目标测试通过，相关 32 个 Reactor 模块编译和前端生产构建成功。
- 用户授权：按既定七阶段路线连续实施，无需阶段间确认。
- 工作区已有改动继续保留，不覆盖预售原型/PRD、`.DS_Store` 和 `output/视频草稿-企业协同与开放平台集成.md`。
- 本阶段不启动真实服务，不修改真实数据库、Redis 或外部系统运行态。

## 2. 设计结论

- 字段事件归属 `formDesignerSchema.settings.governance.fieldEvents`，随应用表单发布快照交付。
- 新协议只引用受管 `EXTERNAL_API`、`DATASET`，不接受任意 URL、SQL、Header、认证或脚本。
- 统一触发器为 `FORM_LOAD`、`CHANGE`、`BLUR`、`MANUAL`、`SCAN_COMPLETE`。
- 运行时统一负责防抖、取消、过期响应隔离、显式结果映射和字段级状态。
- 浏览器上下文只可作为查询参数，授权继续由服务端可信 Session、ACL、租户和数据权限决定。

## 3. 执行记录

| 时间 | 动作 | 结果 |
|---|---|---|
| 2026-08-10 | 盘点 AiForm、AiFormItem、AiCrudPage、设计器治理配置、发布检查和运行时编译器 | 完成 |
| 2026-08-10 | 建立第三阶段四份 SDD 文档并固定协议与验证矩阵 | 完成 |
| 2026-08-10 | 先增加字段事件纯函数/并发测试并确认缺少实现的预期失败 | 完成 |
| 2026-08-10 | 完成协议标准化、显式参数/结果映射、防抖、取消、序列隔离和卸载清理 | 完成 |
| 2026-08-10 | 完成 AiForm/AiFormItem/AiCrudPage 触发、回填、状态反馈和当前用户只读上下文 | 完成 |
| 2026-08-10 | 完成 FieldEventRulesEditor 与查询源目录/元数据结构化配置 | 完成 |
| 2026-08-10 | 完成后端发布检查、嵌套表单校验和协议覆盖报告 | 完成 |
| 2026-08-10 | 自审修复隐藏弹窗提前 onLoad、formOnly 默认值前执行和嵌套弹窗继承父规则 | 完成 |

## 4. 验证记录

### 4.1 前端

- 首次字段事件测试执行因运行时文件尚不存在而按预期失败，随后补实现并通过。
- AiForm 目录回归：`pnpm exec vitest run src/components/ai-form/__tests__`，8 个文件、35 个测试通过。
- 目标 ESLint：0 errors；仅 `AiForm.vue` 保留仓库既有 `vue/no-required-prop-with-default` warning。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：成功，8887 modules transformed。
- 进入阶段 4 前补充重跑生产构建：Node `v20.19.0`，8887 modules transformed，`✓ built in 1m 28s`。
- 构建保留仓库既有 warning：组件同名、动态/静态混合导入、CSS `//` 注释，均不阻断。

### 4.2 Java

- `BusinessObjectPublishServiceFieldEventTest`：4 个测试通过。
- `LowcodeProtocolSnapshotBuilderTest`：5 个测试通过。
- `LowcodeRuntimeConfigBuilderTest#publishesManagedFieldEventsIntoRuntimeOptions`：1 个测试通过。
- 合计 10 个目标测试通过，均显式使用 JDK 17 和 `-Penable-tests`。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：32 个 Reactor 模块 BUILD SUCCESS。

### 4.3 静态检查

- `git diff --check`：通过。
- 新字段事件入口只调用 `/ai/lowcode/query-source/execute`，设计器未提供 URL、Header、认证、凭据、SQL 或脚本输入项。
- 前后端均拒绝危险配置键、危险参数名、原型污染路径和未知协议值。

## 5. 警告与跳过项

- Reactor 全测试存在与本阶段无关的 `forge-plugin-message/MessageServiceImplTest` 构造器基线问题；generator 测试必要时沿用阶段 2 的隔离验证方法。
- `LowcodeRuntimeConfigBuilderTest` 整类的既有主子表用例因测试模型 id 未声明自增而失败；本阶段新增方法单独通过，未扩大修改范围。
- 未启动 Admin、Flow、Vite、数据库、Redis 或外部服务。
