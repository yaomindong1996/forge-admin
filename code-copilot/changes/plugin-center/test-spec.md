# 插件中心增量测试

复用 plugin-foundation 已完成的 Registry/Gate/CLI/脚手架测试，原证据不重录。

本轮新增：

- P0：内置声明严格解析、重复 ID（含外部碰撞）、大小/类型/字段约束、构建过滤与资源不误过滤。
- P0：平台管理员/租户管理员/匿名边界，RBAC 注解，服务查询/分页/详情/Gate 结果。
- P1：列表和详情请求竞态、失败清空/重试、菜单/字典协议、UI 操作及明暗/窄屏。
- Maven `-Penable-tests`（starter-plugin + system）、Admin 聚合 package；UI focused vitest/lint/build。
- 当前模板 DB 桩 + 新生成 full DB 桩；生成清单实际资源/version及 edition 门禁。
- SQL 幂等/placeholder 检查；本轮不修改共享真库，真实 Flyway/正常登录验收另记待执行。

UI 可使用明确标注的模拟接口夹具做截图/交互，不等价于真实部署验收。

## 2026-10-07 增量结果

- Java 相关回归 207/207；模板 Admin 聚合包、生成工程 Admin 聚合包通过。
- 生成工程针对目录/装配/API 的 33/33 测试通过；最新 full 工程 DB 桩 30/30 通过。
- Node CLI/脚手架/DB/文档/edition/本轮契约合计 348/348，通过且无跳过。
- UI 新增 8/8，范围 ESLint、生产构建通过；模拟页面浏览器点击及截图无 pageerror。
- 实际两个 Admin 包各有 14 个有效内置声明，独立 Flow 未混入；Flow 资源过滤分别验证。
- V1.0.210 在 H2 MySQL 模式的合成表连续执行两次及客户菜单冲突保护验证通过。
- 真 MySQL/Flyway、Sa-Token/Redis 正常登录及加密请求、独立 Flow 聚合构建均未执行；
  不将 H2/MockMvc/模拟 UI 替代这些验收。详细命令与边界见 execution-log.md。
