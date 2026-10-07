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

## P2 本轮增量范围

- ZIP 拒绝路径/大小/CRC/压缩/本地头/重叠/链接/NFC 重名；描述与 CLI 跨语言同包一致。
- XML 外部实体与 POM/组件约束；商业包/内置冲突/降级拒绝，摘要与运行快照固定。
- 上传幂等、CAS 确认/取消、过期 revision/摘要及运行快照变化、租户和平台/RBAC 边界。
- 任务 SQL 不查 BLOB、不物理删除；迁移重复保护/字典/清理模板；包不出响应/日志。
- UI 上传预检、任务详情/阻断/确认/取消/错误重试；实际 UI manifest 缺组件/坏登记失败。
- 复用 P1 构建/单测与 full 生成/DB 桩；无授权不运行真实 MySQL/登录/生产 builder。

## 2026-10-07 P1 增量结果

- Java 相关回归 207/207；模板 Admin 聚合包、生成工程 Admin 聚合包通过。
- 生成工程针对目录/装配/API 的 33/33 测试通过；最新 full 工程 DB 桩 30/30 通过。
- Node CLI/脚手架/DB/文档/edition/本轮契约合计 348/348，通过且无跳过。
- UI 新增 8/8，范围 ESLint、生产构建通过；模拟页面浏览器点击及截图无 pageerror。
- 实际两个 Admin 包各有 14 个有效内置声明，独立 Flow 未混入；Flow 资源过滤分别验证。
- V1.0.210 在 H2 MySQL 模式的合成表连续执行两次及客户菜单冲突保护验证通过。
- 真 MySQL/Flyway、Sa-Token/Redis 正常登录及加密请求、独立 Flow 聚合构建均未执行；
  不将 H2/MockMvc/模拟 UI 替代这些验收。详细命令与边界见 execution-log.md。

## 2026-10-07 P2 增量结果

- 模板 Java 相关回归 236/236；新增有界 ZIP、JSON/XML、幂等/CAS、BLOB/租户及权限迁移测试。
- UI focused Vitest 14/14、ESLint 及生产构建通过；明确模拟接口的上传/确认/取消/重试、
  构建版本不同提示、深色 320px 抽屉按钮可见性及无页面级横溢出验证通过，无 pageerror。
- 完整 Node 基线 353/353；新 full 改名工程 DB 桩 31/31、相关 Java 56/56、Admin 聚合包通过。
- 原始 Forge 坐标的包在生成后的验证器中通过；标准 hello 样例安装到真实生成工程后，
  UI manifest 输出 hello 1.0.0/core 1.2.0，不泄露 source/路径，生成宿主仍可 package。
- 实际 V1.0.211 在 H2 合成表连续执行两次，权限唯一键、字典及客户菜单/权限冲突保护通过。
- 构建执行器未接入；源工作区/定制差异仍不可核验；未连接真实 MySQL/Redis、执行 Flyway、
  正常登录/加密/multipart 部署验收，不把模拟 UI 或 H2 结果当成这些验收。
