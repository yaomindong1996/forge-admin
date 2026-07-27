# 执行日志 — 客户端凭据与公共客户端协议加固

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-07-27 | Research | 复核细化整改清单和当前源码 | 确认浏览器 Secret、数据库明文比较和弱种子仍存在；未输出值 |
| 2026-07-27 | Proposal/HARD-GATE | 创建 Spec、Tasks、Test Spec 和执行日志 | 用户已明确授权按分阶段思路继续实施 |
| 2026-07-27 | Baseline | System 插件现有测试 | 8 条在 Mockito agent 和未安装 crypto Core 类处失败，未进入业务断言 |
| 2026-07-27 | Red/Green | 新增 Codec、Policy、Service 条件升级测试 | Red 为 Codec 不存在；Green 10 条通过 |
| 2026-07-27 | Backend | System Reactor package | JDK 17，25 模块全部成功，测试按增量矩阵单独执行 |
| 2026-07-27 | Frontend | Admin UI production build | Node 20.19.0，8725 模块，1分28秒；仅既有警告 |
| 2026-07-27 | Static | XML/SQL/Secret/Wrapper 扫描 | Mapper XML 合法；弱种子、浏览器 Secret、SysClient Controller Wrapper、Flyway placeholder 均为零 |
| 2026-07-27 | Review/阶段二 | 独立代码质量复审 | 首次 FAIL：迁移会覆盖已关闭 legacy-read，缺少 CAS 更新失败测试和最新证据 |
| 2026-07-27 | Fix | 修复两项阶段二阻塞 | JSON 属性仅缺失时补默认值；新增条件升级返回 0 时不清缓存测试 |
| 2026-07-27 | Retest | System 4 个目标测试类 | 20 条通过，0 失败、0 错误、0 跳过；`BUILD SUCCESS` |
| 2026-07-27 | Review/阶段二终审 | 独立 Reviewer 基于最新工作树复审 | FAIL：1 Critical、1 Important、2 Minor；登录操作日志和缓存旧值回填为阻塞 |
| 2026-07-27 | Final fix | 硬排除凭据入口日志；认证配置数据库直读；删除全部浏览器 Secret 环境声明；补合同测试 | 不改变浏览器会话密钥协议，不连接数据库或 Redis |
| 2026-07-27 | Final retest | System 5 个目标测试类、System Reactor、Admin UI build | 24/24；Reactor 退出码 0；8727 modules 构建成功 | 构建仅有既有组件命名、CSS 注释和 chunk 警告 |
| 2026-07-27 | Review/阶段二第三次 | 独立 Reviewer 基于最新工作树复审 | FAIL：2 Critical、2 Important、2 Minor；Secret 并发恢复、Token 日志和 Mapper JSON 映射为阻塞 | 未连接数据库或 Redis |
| 2026-07-27 | Review fix 3 | 空 Secret 零写、Secret CAS、Token 日志/广播清理、在线下线日志硬排除、Mapper resultMap | completed | 保持既有登录和在线管理接口协议 |
| 2026-07-27 | Review fix 3 retest | System 5 个目标测试类、System Reactor、XML/静态扫描 | 26/26；25 模块构建成功；扫描无命中 | 单模块首次受旧 Core 快照阻断，刷新 Reactor 后通过 |
| 2026-07-27 | Review fix 4 | 修复 2 Critical + 2 Important | `/auth/online/**` 强制登录/显式权限/租户边界；删除 `/test`；响应改会话 ID；客户端单 SQL 双旧值 CAS；删除 Token 原文日志 | 未修改 Flow 文件，未连接 DB/Redis |
| 2026-07-27 | Review fix 4 retest | 7 个目标测试类及目标静态检查 | 36/36，0 failure/error/skip；Vue ESLint、Mapper XML、Flyway、环境/Token/响应/格式扫描通过 | 未执行真实 DB/Redis/E2E、全量构建或前端 build |
| 2026-07-27 | Latest Code Quality Review | 独立 Reviewer 复核在线会话边界 | FAIL：1 Critical | 超级管理员下缺少显式 `tenantId` SQL 边界；用户明确要求暂缓，未修改代码 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|------------|------|
| 浏览器客户端 | `client_auth_method=none` | 在浏览器继续保存固定 Secret | 分发式客户端无法保密 |
| 机密客户端存储 | `{bcrypt}` 摘要 | 可逆加密、裸 SHA-256 | 不需要取回原文，BCrypt 抵抗弱值枚举 |
| 存量兼容 | 成功验证后条件升级 | 启动即批量改写、永久明文双读 | 避免无凭据迁移和并发覆盖 |
| 查询实现 | Mapper XML | Service/Controller Wrapper | 满足 DataScope 和项目 SQL 规范 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|----------|--------|
| 待执行 | — | — | — |

## 验证记录

| 时间 | 范围 | 命令 | 结果 | 警告/跳过 |
|------|------|------|------|-----------|
| 2026-07-27 | Review fix 格式/迁移合同 | `git diff --check`；扫描 `JSON_CONTAINS_PATH` 门禁 | 通过 | 未执行真实 Flyway/MySQL |
| 2026-07-27 | System 目标测试 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system test -Dforge.compiler.skip=false -Dforge.tests.skip=false -Dforge.test.groups= -Dtest=ClientSecretCodecTest,ClientCredentialPolicyTest,ClientServiceImplCredentialTest,SystemAuthServiceImplClientCredentialTest` | 20 条通过，`BUILD SUCCESS` | 首次 Reactor 运行被 `forge-starter-datascope` 缺少 JUnit 依赖阻断；模块定向测试成功 |
| 2026-07-27 | 阶段二最终修复测试 | `compiler:testCompile` 后执行 5 个目标类 `surefire:test` | 24 条通过，0 failure/error/skip | 单模块首次受 `.m2` 旧 core 快照阻断；先执行 System `-am package -DskipTests` 后有效运行 |
| 2026-07-27 | 阶段二第三次修复测试 | System `-am package -DskipTests`；5 个目标类模块测试；`xmllint` 和 Token/环境静态扫描 | 25 模块构建成功；26 条通过；XML 合法且扫描无命中 | 真实 MySQL、Redis 和登录/轮换 E2E 保留为部署门禁 |
| 2026-07-27 | 本轮首次目标测试 | System 单模块目标测试 | 未进入测试 | 默认 JDK 8 不支持 Java 17；切换本机 JDK 17 |
| 2026-07-27 | 本轮依赖对齐 | Core 单模块 `install -DskipTests`；Core+System Reactor | Core 编译成功；安装被沙箱禁止写 `~/.m2`，Core+System 被共享 Excel 新常量阻断 | 改为只选 Core、Excel、System 三模块，不执行 install 或全量构建 |
| 2026-07-27 | 本轮目标测试首次执行 | Core、Excel、System 三模块；7 个目标测试类 | 32 条通过，3 条在线服务测试因 Mockito agent 无法自附加报环境错误 | 将新增测试改用 JDK 动态代理，不降低断言 |
| 2026-07-27 | 本轮目标测试最终执行 | `mvn -pl forge-starter-core,forge-starter-excel,forge-plugin-system test`，限定 7 个测试类 | 36 条通过，0 failure/error/skip，`BUILD SUCCESS` | Reactor 仅 3 个模块；有既有 deprecated/commons-logging 警告 |
| 2026-07-27 | 在线 SQL 合同加固复测 | 同一三模块 Reactor，限定 `OnlineUserSecurityContractTest` | 5 条通过，0 failure/error/skip，`BUILD SUCCESS` | 列集合显式断言不含 `token_value` |
| 2026-07-27 | 本轮目标静态检查 | Vue 单文件 ESLint、两份 Mapper XML、Flyway 唯一版本/placeholder、环境 Secret、Token 日志/响应、尾随空白和 diff check | 全部通过 | 未启动任何服务 |

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| 待执行 | — | — | — |

## 代码质量备忘

- 任何扫描只能报告文件、行号和是否命中，禁止打印 Secret 值。
- 当前 crypto 变更未提交；后续验证需区分既有 crypto 改动与本变更证据。
