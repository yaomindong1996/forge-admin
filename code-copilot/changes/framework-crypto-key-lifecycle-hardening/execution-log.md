# 执行日志 — 框架加密密钥生命周期加固
> status: complete

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-07-26 | Research | 核实根密钥来源、配置优先级和全部 `EncryptorFactory` 调用点 | 确认根密钥间接保护持久化数据，不能直接替换 |
| 2026-07-27 | Proposal amendment | 增加启动前密钥自动引导和 Docker 持久化卷 | 用户明确要求启动自动注入，无需手工 export；历史 legacy key 仍不可推导 |
| 2026-07-26 | Research | 核实数据连接密码和低代码动态字段链路 | `password_cipher` 已有种子密文；生产 `encrypt_config` 状态未知 |
| 2026-07-26 | Research | 核实配置中心与通用配置分组旁路 | 数据库配置 `addFirst`，可覆盖环境/YAML；通用 CRUD 可读写原始 JSON |
| 2026-07-26 | Proposal 审查 | 补查 `/system/config` 散配置入口 | 读取已脱敏，但 insert/update 仍可写部署级 crypto key，已纳入 Spec |
| 2026-07-26 | Research | 核实浏览器会话密钥和固定 TypeHandler | 会话协议应保留；TypeHandler 为死代码，可删除 |
| 2026-07-26 | Proposal | 创建 Spec、Tasks、Test Spec 和执行日志 | 等待 HARD-GATE，未修改生产代码 |
| 2026-07-26 | HARD-GATE | 用户明确要求继续完成剩余任务 | 进入 `/apply framework-crypto-key-lifecycle-hardening` |
| 2026-07-26 | Apply A | 完成来源、数据库配置、API、UI、初始化 SQL 与死代码暴露面清理 | 非法 crypto JSON 读写失败关闭；Map 方括号键名旁路同步封堵 |
| 2026-07-26 | Apply B | 完成 `FPC1` 持久化协议及 Data/Generator 双读接入 | 默认仍写旧格式；浏览器会话密钥协议未改 |
| 2026-07-26 | Apply C | 完成 inventory/dry-run/execute、全 scope 预检和批次原子事务 | 未执行真实数据库迁移 |
| 2026-07-26 | Review | 完成 Spec Compliance 和 Code Quality 两阶段审查 | 修复批次事务、配置失败关闭、预检顺序、容量元数据和日志安全问题 |

## Proposal 验证

| 时间 | 范围 | 命令 | 结果 | 警告/跳过 |
|------|------|------|------|-----------|
| 2026-07-26 18:54 CST | 四份 SDD 文档 | `rg -n '[[:blank:]]+$' .../*.md`；EOF `tail -c 1 | od`；章节/状态断言；两处已知硬编码密钥精确扫描 | 通过：无尾随空白，四文件 EOF 均为 `0a`，Spec 13 章和 propose/HARD-GATE 状态存在，文档未复制历史密钥值 | 仅文档，不运行代码测试、不启动服务 |

## 实施记录

| Task | 时间 | Red 证据 | Green/构建证据 | 提交 | 备注 |
|------|------|----------|------------------|------|------|
| Task 1-5（含 4B） | 2026-07-26 | 配置管理/旁路测试在旧合同下失败；非法 JSON 新用例 3 条中 1 条失败 | Config 8 条通过；System 防护 4 条通过；初始化值和历史密钥扫描通过 | 未提交 | 配置只保留非敏感功能开关，Flyway 清理数据库历史值 |
| Task 6-8（含 7B） | 2026-07-26 | 新协议、数据连接和低代码失败关闭行为在旧实现下不成立 | Crypto 22 条、Data 读写相关测试、Dynamic CRUD 生命周期测试通过 | 未提交 | `SessionKeyStore`、API Advice 和前端会话协议保持不变 |
| Task 9-11 | 2026-07-26 | 批次事务 Red 8 条中 6 条通过、2 条原子性断言失败 | 批次事务专项 12 条通过；最终 Data 7、Generator/System/Admin 22 条通过 | 未提交 | Data 使用 `REQUIRES_NEW`；低代码使用实际运行数据源事务管理器 |
| Task 12 | 2026-07-26 | — | 44 模块 test-compile/package、59 条隔离测试、前端 build、静态扫描通过 | 未提交 | 生产迁移和旧钥退役按设计跳过 |
| Task 13 | 2026-07-27 | 旧实现缺少启动前自动注入 | 启动引导 8/8，引导+原校验 15/15，Compose 解析通过 | 未提交 | 新安装自动生成并持久化；历史 legacy key 不可自动推导 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|------------|------|
| 密钥域 | 传输根密钥与持久化 keyring 分离 | 继续共用单一根密钥 | 避免 API 密钥轮换再次破坏持久化数据 |
| 密文协议 | `FPC1:algorithm:keyId:payload` | 直接替换旧 key | 需要兼容读取、定位 keyId 和受控迁移 |
| 发布策略 | 双读能力先发布，写入开关后切换 | 新版本一部署即写新格式 | 兼容滚动发布中的旧节点 |
| 迁移策略 | 管理员 dry-run/execute，按租户/configKey | Flyway 自动解密所有动态表 | Flyway 没有运行密钥和动态数据源上下文 |
| 动态表 DDL | 容量不足阻塞，不自动扩列 | 迁移器自动修改字段长度 | 外部数据源和业务表 DDL 风险过高 |
| 错误处理 | 解密失败关闭 | 把密文当明文或静默跳过 | 防止错误密钥被掩盖和敏感值错误流转 |
| 启动 Secret | EnvironmentPostProcessor + 外部稳定文件 | 每次启动生成、写入 Git/DB、运行后修改父环境 | 配置绑定前生效，又不会因重启或容器重建换钥 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|----------|--------|
| 初步判断根密钥只用于 API 降级不完整 | 只追踪了直接 Advice 调用，遗漏 `EncryptorFactory` 的业务消费者 | 全仓搜索工厂注入和无参 encrypt/decrypt，确认两类持久化调用 | 已写入 `pitfalls.md` |
| 只改 YAML 仍可能不生效 | DB PropertySource 使用 `addFirst` | 同时移除 ConfigConverter 映射、配置 API 字段和数据库历史值 | 已沉淀 |
| 只封堵 config group 仍存在散配置旁路 | `DbConfigLoader` 同时加载 `sys_config` 与 `sys_config_group` | 共享精确密钥策略，拦截 `/system/config` 写入并由 Flyway同步清理 | 已沉淀 |
| 滚动发布会产生格式不兼容 | 旧节点不能识别版本前缀 | 增加 `write-versioned` 开关并固化发布顺序 | 已写入 `decisions.md` |
| per-row 捕获会破坏批次原子性 | 行级失败被吞掉后事务仍可提交前序更新 | 先准备整批，再在 `REQUIRES_NEW` 中执行；冲突/异常抛出触发整批回滚 | 已沉淀 |
| 只在各 scope 内校验会产生部分迁移 | Data 先写入后 Lowcode 参数校验失败 | Coordinator 在首次写入前预校验全部 included scope | 已沉淀 |

## 知识发现

- [x] **持久化加密调用审计**：追踪加密工厂时必须检查间接业务消费者，不能只看配置类和 Advice。
- [x] **密钥轮换发布协议**：引入版本化密文时应先部署双读，再切换单写，最后迁移和退役。
- [x] **数据库配置优先级**：外部化 Secret 时要同步排查优先级更高的数据库动态配置源。

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| 每 Task 独立提交 | 每个实施 Task 使用独立提交 | 工作区在进入本变更前已有多组用户修改，且用户未要求代为提交；本轮未创建 Git commit | 保留按 Task 的验证和文件记录，不混入或提交用户其它变更；由仓库所有者按需要整理提交 |
| SQL Server 低代码迁移验证 | Tasks 初稿列出 MySQL/PostgreSQL/Oracle/SQL Server | 当前低代码运行时仅注册 MySQL/PostgreSQL/Oracle，仓库没有可复用的 SQL Server 完整方言 | 不在密钥变更中局部新增不完整方言；文档改为验证现有三种方言 |
| 生产迁移与退役 | 代码就绪后按 Runbook 执行 | 本地无真实 keyring、租户清单、备份和维护窗口 | 保持部署待办，不标记归零或退役完成 |

## 代码质量备忘

- 迁移日志和响应不得出现明文、密文、密钥或可逆摘要。
- 动态表名/列名/主键必须来自校验后的配置并使用方言引用；值参数必须绑定。
- 所有自定义 XML 查询必须显式包含租户和逻辑删除条件。
- 生产旧钥退役是运维门禁，不得因代码测试通过而自动标记完成。

## 2026-07-27 启动自动引导增量验证

- 生产代码：新增 `CryptoSecretEnvironmentPostProcessor`，由 `META-INF/spring.factories` 在配置绑定前自动加载。
- 密钥生命周期：显式非空配置优先；否则复用外部文件；仅文件不存在时原子生成两个独立 16 字节密钥。
- 文件安全：POSIX 目录 `0700`、文件/锁 `0600`；非法字段、空值、非法 Base64、错误长度和符号链接失败关闭。
- 定向测试：JDK 17 Reactor 下自动引导 8/8 通过；与 `CryptoConfigurationValidatorTest` 合并执行 15/15 通过。
- 启动合同：最小 `SpringApplication` 不依赖手工 export，Context 刷新前可读取根密钥和活动密钥。
- Docker：`docker compose config --quiet` 通过；Admin 挂载 `crypto_secrets:/var/lib/forge/secrets`，容器未启动。

## 2026-07-27 自动引导静态收尾

- 文档一致性：修正多实例“共享只读密钥文件”的错误描述。自动文件引导需要可写共享目录以创建锁文件；只读 Secret 通过显式环境/JVM 配置注入并跳过文件引导。
- 整改清单：补齐首次自动生成、重启复用、显式配置优先、损坏失败关闭、Docker 卷持久化和 legacy key 不可推导的完整流程。
- 静态检查：`git diff --check` 通过；相关 Java、注册文件和文档无尾随空白，EOF 均为 LF。
- 注册检查：`META-INF/spring.factories` 已注册 `CryptoSecretEnvironmentPostProcessor`；Admin/Report YAML 和 Docker Compose 均包含 bootstrap 配置，Admin 挂载 `crypto_secrets` 卷。
- 敏感值扫描：生产源码、配置、Docker 和前端范围内，历史根密钥、固定 TypeHandler 密钥及旧客户端弱密钥精确扫描均为 0。
- 跳过：本轮仅做文档一致性和静态收尾，复用已通过的 15/15 定向测试与 Compose 解析结果，不重复启动服务、数据库、Redis 或容器。

## 2026-07-26 最终验证

- Red：批次事务专项首次 8 条中 2 条失败，证明旧实现会在后续冲突/异常时保留前序写入；非法 crypto JSON 专项首次 3 条中 1 条失败，证明旧清洗器会放行原始非法 JSON。
- Green：批次事务专项 12 条通过；最终隔离矩阵 Config 8、Crypto 22、Data 7、Generator/System/Admin 22，共 59 条通过，0 失败。
- 测试编译：JDK 17 执行 Admin/Report 依赖链 `-Penable-tests -DskipTests test-compile`，44/44 模块成功，耗时 39.411 秒。
- 后端打包：JDK 17 执行 `mvn -pl forge-admin-server,forge-report-server -am package -DskipTests`，44/44 模块成功，耗时 38.178 秒。
- 前端构建：Node 20.19.0 执行 `pnpm build`，8725 模块成功，耗时 1 分 33 秒；仅有既有命名冲突、动态/静态 import、CSS 注释和分块警告。
- 静态检查：`git diff --check` 通过；Flyway 最新版本 `1.0.52`、无重复版本、无占位符；受管源码/配置中的历史根密钥和旧种子密文均为 0；初始化 JSON 无部署密钥字段；`EncryptTypeHandler` 引用为 0；浏览器会话协议引用保留。
- 环境失败：一次 package 未固定 JDK，系统 Java 8 报“无效的目标发行版: 17”；固定 JDK 17 后通过。直接插件测试受本地陈旧 Starter jar 和既有 Surefire groups/引擎配置阻断，目标行为改用 Reactor 当前 classes 隔离执行。
- 服务：未启动后端、前端开发服务、数据库、Redis 或浏览器进程；新增服务 PID 无，无需停止。
- 跳过：未生成真实密钥，未连接生产库，未执行 migration execute，未做全租户归零，未关闭 legacy read，未移除历史/旧密钥，未轮换传输根密钥。

## 两阶段审查

- Spec Compliance：PASS。里程碑 A/B/C 全部代码和文档任务完成，生产操作按边界保留。
- Code Quality：PASS_WITH_COMMENTS。已修复审查发现的批次事务、scope 预检、失败关闭、方括号配置键旁路、空历史密钥和 PostgreSQL 容量元数据问题；没有剩余阻塞项。
