# 外部连接器与数据集运行时加固执行日志

## 1. 基线

- 日期：2026-08-10
- 状态：已完成
- 用户授权：按既定七阶段路线连续实施，无需阶段间确认。
- 工作区已有改动已识别并保留：`.DS_Store`、预售原型/PRD、`output/视频草稿-企业协同与开放平台集成.md`。
- 本阶段不启动真实服务，不修改真实数据库运行态。

## 2. 研究结论

- 外部连接器认证、参数映射和响应解析已经存在，权限/限流/缓存/重试字段尚未接入执行链。
- 连接器调用日志存在请求、URL query 和响应泄露风险。
- 外部系统凭据当前直接落库并由实体管理接口返回。
- 数据集已经提供参数化 SQL/表查询，无需新建查询模块；当前需补缓存、错误传播、日志和只读连接。
- Java 17 默认不保证提供 `javascript` Script Engine，现有脚本适配器缺少空 Engine 处理。

## 3. 执行记录

| 时间 | 动作 | 结果 |
|---|---|---|
| 2026-08-10 | 读取项目 AGENTS、记忆和适用 Skill | 完成 |
| 2026-08-10 | 盘点 external/data/crypto/cache/outbound 实现 | 完成 |
| 2026-08-10 | 建立四份 SDD 文档 | 完成 |
| 2026-08-10 | 接入权限、限流、GET 缓存、安全重试和统一受控出站 | 完成 |
| 2026-08-10 | 外部系统凭据写入加密、运行解密、管理端掩码 | 完成 |
| 2026-08-10 | 统一请求、URL、响应、错误和运行日志脱敏 | 完成 |
| 2026-08-10 | 加固数据集只读连接、资源关闭、行数/超时、错误传播、日志和缓存 | 完成 |
| 2026-08-10 | 增加 `V1.0.101` 场景字典迁移和静态迁移契约测试 | 完成 |
| 2026-08-10 | 完成 Spec 合规、代码质量和敏感日志静态自审 | 通过 |

## 4. 验证记录

所有后端验证均显式使用 JDK 17；根 POM 默认跳过测试，定向测试使用 `-Penable-tests` 并核对实际 `Tests run`。

| 验证 | 命令摘要 | 结果 |
|---|---|---|
| 外部连接器定向测试 | `mvn -pl .../forge-plugin-external -am -Penable-tests -Dtest=... test` | `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS` |
| 数据集定向测试 | `mvn -pl .../forge-plugin-data -am -Penable-tests -Dtest=DataQueryExecutorTest,DataQueryRuntimeCacheTest test` | `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS` |
| 统一出站模块测试 | `mvn -pl .../forge-starter-outbound -am -Penable-tests test` | outbound 模块 `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS` |
| 受影响模块聚合编译 | `mvn -pl .../forge-plugin-external,.../forge-plugin-data -am -DskipTests compile` | 21 个 Reactor 模块全部 `SUCCESS` |
| 差异格式检查 | `git diff --check` | 通过，无输出 |
| Flyway 占位符扫描 | 扫描 `V1.0.101__add_external_connector_outbound_scene.sql` 中的 `${...}` | 通过，无匹配 |
| 敏感日志静态扫描 | 扫描 external/data 新增日志及 `getMessage()` 使用 | 未发现参数值、完整查询串、响应原文或凭据直接输出 |

## 5. 警告与跳过项

- 未执行真实 Redis 限流竞争压测、真实出站白名单/重定向/TLS 联调、真实 OAuth2 服务联调、Flyway 落库和业务数据库只读账号验收；这些需要部署环境配置后补验。
- 未执行存量明文凭据批量迁移；运行时已兼容并输出不含原值的迁移告警。
- 聚合构建存在项目既有的 Lombok `@Builder` 默认值、弃用 API 和测试 unchecked 编译警告，不属于本变更引入的失败。
- 本阶段未启动任何 Forge 服务，无运行进程需要清理；未修改真实数据库、Redis 或外部系统状态。
