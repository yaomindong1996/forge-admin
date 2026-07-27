# 单测 Spec — 框架加密密钥生命周期加固
> status: done
> created: 2026-07-26

## 0. 测试原则

- **Red/Green TDD**：每个 Task 的行为测试先在旧实现上失败，再实施最小修复。
- **First Run the Tests**：`/apply` 前先运行 Starter Crypto 现有测试和相关模块编译，记录真实基线。
- **增量复用**：复用阶段一已验证的 JDK 17、Node 20.19.0、Admin 聚合 package 与前端 build 命令。
- **安全证据**：除单测外必须扫描历史根密钥、部署级密钥字段、日志输出和 Flyway 占位符。
- **不碰生产数据**：自动验证不连接生产库、不执行真实迁移、不生成可提交密钥。
- **失败关闭优先**：错误密钥、未知 keyId、损坏密文、非法配置和容量不足必须有反向测试，避免“只测成功路径”。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit Jupiter 5，由 Spring Boot 3.5.13 BOM 管理 |
| Mock 框架 | Mockito + AssertJ/JUnit Assertions |
| 现有测试基线 | Starter Crypto 1 个测试类/3 条；Starter Config 无测试目录；Generator 有既有专项测试，Data 暂无加密专项测试 |
| 数据访问验证 | Mockito/测试 JdbcTemplate + Mapper XML/SQL 合同测试；有隔离测试库时再做可选 E2E |
| 前端验证 | Node `v20.19.0` + `pnpm build`，本次 UI 仅删除字段，不做视觉改版 |

## 2. 覆盖范围

### P0 — 密钥配置与持久化协议（必须覆盖）

#### 类名：`CryptoConfigurationValidator`

| 场景 | 配置 | 预期结果 |
|------|------|----------|
| 传输加密启用且根密钥空 | `enabled=true` | 启动校验失败并指出 `FORGE_CRYPTO_SECRET_KEY` |
| 根密钥非法 Base64/错误长度 | 任一启用路径 | 启动校验失败，不回显配置值 |
| 兼容写入 | `writeVersioned=false` | 要求旧格式写入密钥，不要求活动 key |
| 版本化写入 | `writeVersioned=true` | 要求合法 activeKeyId 和活动 key |
| 历史 keyring | 合法/非法 keyId、重复/空 key | 合法通过，非法失败 |
| 全部能力关闭 | 不配置密钥 | 不因无关密钥失败 |

#### 类名：`CryptoSecretEnvironmentPostProcessor`

| 场景 | 输入 | 预期结果 |
|------|------|----------|
| 首次新安装 | 无显式根密钥、密钥文件不存在 | 原子生成两个独立 16 字节 Base64 密钥，注入 Spring Environment |
| 重启 | 密钥文件已存在 | 复用全部原值，文件不重写 |
| 显式配置 | 环境/JVM 提供非空根密钥 | 跳过自动生成，不创建文件 |
| 损坏文件 | 缺少必需字段、非法 Base64 或错误长度 | 启动失败，不重生成 |
| 权限 | POSIX 文件系统 | 目录 `0700`、密钥文件 `0600` |
| 空白 Compose 变量 | 容器传入空字符串 | 按未配置处理，由密钥文件值覆盖空白值 |

#### 类名：`VersionedPersistentCryptoService`

| 场景 | 输入 | 预期结果 |
|------|------|----------|
| 兼容写 | `writeVersioned=false` | 输出旧无版本 Base64，可被旧/新服务读取 |
| 活动 key 写 | `writeVersioned=true` | 输出 `FPC1:算法:keyId:payload`，不含明文 |
| 活动 key 读 | 当前 keyId | 正确解密 |
| 历史 key 读 | 轮换前 keyId | 正确解密，新写仍使用活动 keyId |
| 旧密文读 | legacy key + legacy algorithm | 正确解密 |
| 未知 keyId | 合法格式但 key 不存在 | 失败关闭 |
| 头/算法/payload 损坏 | 多种非法输入 | 失败关闭，不返回原值 |
| 缺旧钥 | 无版本密文 | 失败关闭 |
| null/空值 | null/空字符串 | 按接口合同原样处理，不误分类 |
| 重加密 | 旧/历史/活动格式 | 旧和历史转活动；活动格式保持幂等 |

### P1 — 配置暴露面

#### 类名：`CryptoConfigExposureTest` / `CryptoConfigSanitizerTest`

| 场景 | 输入 | 预期结果 |
|------|------|----------|
| 管理 GET | crypto 配置 | JSON 不含根密钥、RSA 私钥、activeKey、keys、legacyKey |
| 管理 PUT | 非敏感开关 | 正常保存并同步 |
| 管理 PUT | 敏感字段 | 明确拒绝，不写数据库 |
| ConfigConverter | 含历史敏感字段 JSON | 属性 map 不产生部署级密钥键 |
| 通用 group list/page/detail | crypto 原始 JSON | 返回前完成清洗 |
| 通用 group add/edit | 含敏感字段 | 明确拒绝 |
| 散配置 add/edit/改名 | 部署级 crypto key | 明确拒绝，不写数据库 |
| 普通敏感散配置 | 文件存储等既有配置 | 继续使用既有掩码保留语义，不扩大禁用范围 |
| 非 crypto 分组 | 普通 JSON | 不受敏感清洗误伤 |

#### Flyway 合同

- 脚本版本为 `V1.0.52` 且仓库无重复版本。
- `sys_config` 删除目标覆盖根密钥、RSA 私钥和 persistence keyring。
- `sys_config_group` 只清理 `group_code='crypto'` 的敏感 JSON 路径。
- 脚本不包含历史密钥值、真实密钥或 `${...}` Flyway 占位符。
- 安全物理删除原因和回滚方式已写入 Spec/Runbook。

### P1 — 数据连接加密与迁移

#### 类名：`DataConnectionCryptoLifecycleTest`

| 场景 | 输入/Mock | 预期结果 |
|------|-----------|----------|
| 新增密码 | 版本化写开启 | 保存 `FPC1`，不保存明文 |
| 修改空密码 | 已有密文 | 保留原密文 |
| 读取旧密码 | legacy 密文 | JDBC 获得明文密码 |
| 读取新密码 | active/historical | JDBC 获得明文密码 |
| 错误 key/损坏密文 | 解密抛异常 | 连接创建失败，不把密文当密码 |

#### 类名：`DataConnectionCryptoMigrationServiceTest`

| 场景 | 预期结果 |
|------|----------|
| inventory | 仅计数分类，不返回敏感值 |
| dry-run | 不执行 UPDATE |
| execute | 旧格式变活动格式，更新条件含 id + 原密文 |
| 并发修改 | 条件更新 0 行，计冲突，不覆盖新值 |
| 重复执行 | 活动格式跳过，结果幂等 |
| 单行失败 | 批次回滚/失败计数，其他批次按合同继续 |

### P1 — 低代码字段加密与迁移

#### 类名：`DynamicCrudCryptoLifecycleTest`

| 场景 | 输入 | 预期结果 |
|------|------|----------|
| SM4/AES 写入 | camelCase 配置 + snake_case 数据 | 指定列写版本化密文 |
| 旧/活动/历史读取 | 多字段混合 | 全部正确解密 |
| 非字符串/空字段 | null/数字/空串 | 按既有合同跳过 |
| 非法算法/未知 keyId/损坏值 | 任一字段 | 当前操作失败，不返回或落库原值 |
| encrypt_config 非法 | 非对象/字段规则非法 | 明确业务错误 |

#### 类名：`LowcodeCryptoMigrationServiceTest`

| 场景 | 预期结果 |
|------|----------|
| 未显式 configKey | 拒绝执行 |
| 主库/外部库 | 使用对应 RuntimeJdbcTemplate 与方言 |
| 数据源只读 | BLOCKED，不执行写入 |
| 表/字段不存在 | BLOCKED |
| 非单主键/非法标识符 | BLOCKED |
| 列容量不足 | BLOCKED，不自动 DDL |
| dry-run | 返回分类和容量结果，不 UPDATE |
| execute | 参数化批次更新，按当前租户，原值比较 |
| 重跑/并发冲突 | 幂等并正确计数 |

### P2 — 入口、装配与静态证据

- `ConfigManageController` 只允许平台管理员调用 migration API。
- migration execute 默认 `dryRun=true`；`expectedActiveKeyId` 不匹配时拒绝。
- Admin/Report 均能装配 `PersistentCryptoService` 和 Data 插件依赖。
- 管理端构建通过，crypto 表单不再读写部署级密钥字段。
- YAML 显式 Secret 仍为空默认且不包含历史密钥；启动引导开关和外部文件路径可通过环境变量覆盖。
- `EncryptTypeHandler` 和 `SysClient` 死引用清零。
- 浏览器 `CryptoRuntimeConfig`/`SessionKeyStore` 仍存在，API 会话协议未被误删。

### 不测试（明确原因）

- 不执行生产库真实迁移：需要生产备份、真实 keyring、租户清单和维护窗口。
- 不调用外部低代码数据源：仓库没有可提交的凭据；使用 Mock/测试数据源验证路由和 SQL 合同。
- 不重写 Git 历史：历史清理是仓库治理操作，轮换后旧钥失效即可形成运行安全边界。
- 不升级 SM4/AES 加密模式：本变更解决密钥生命周期；算法模式升级需独立密码学评审和新协议版本。
- 不新增 UI E2E：配置中心只删除字段，生产 build 和静态选择器扫描足以覆盖本轮 UI 差异。

## 3. 执行计划

- [x] Step 1：使用 JDK 17 运行 Starter Crypto 现有测试，记录实施前基线。
- [x] Step 2：Task 1/3/4/6 配置与协议测试 Red → Green。
- [x] Step 3：Task 7 数据连接测试 Red → Green。
- [x] Step 4：Task 8/10 低代码测试 Red → Green。
- [x] Step 5：Task 9/11 迁移服务与入口测试 Red → Green。
- [x] Step 6：执行相关 Reactor 测试、Admin/Report package 和 UI build。
- [x] Step 7：执行 YAML/JSON/SQL/敏感值/死代码/Flyway 静态扫描和 `git diff --check`。
- [x] Step 8：完成 Spec Compliance → Code Quality 两阶段审查并增量修复。
- [x] Step 9：实现 Task 13 启动自动引导，执行定向 Reactor 测试、最小 SpringApplication 合同和 Compose 解析。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-26 | 阶段一 Admin 聚合 | JDK 17 Admin 43 模块 package | 通过 | 复用 `framework-captcha-security-hardening/execution-log.md` |
| 2026-07-26 | 阶段一前端 | Node 20.19.0 `pnpm build` | 通过 | 8725 模块，存在既有 Vite 警告 |
| 2026-07-26 18:54 CST | 本 Proposal | 文档结构、行尾/EOF、敏感值扫描 | 通过 | HARD-GATE 前不运行代码测试 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-26 18:54 CST | Proposal 文档 | 行尾/EOF、章节/状态一致性、历史密钥精确扫描 | `rg` + `tail/od` + 章节断言 | 通过 | 四份文件均以换行结束，无尾随空白，不含两处已知硬编码密钥值；仅文档，不运行代码测试 |
| 2026-07-26 21:22-21:53 CST | 配置、协议、数据连接、低代码和迁移入口 | Red/Green 与批次事务回归 | Reactor `test-compile` + `/tmp/ForgeJUnitRunner` 隔离矩阵 | 通过 | Red 捕获 2 条批次原子性失败和 1 条非法 JSON 失败；修复后批次事务 12 条及各目标组通过 |
| 2026-07-26 22:02 CST | 最终目标测试 | Config、Crypto、Data、Generator/System/Admin | Reactor 当前 `target/classes` 隔离执行 | 通过 | Config 8、Crypto 22、Data 7、Generator/System/Admin 22，共 59 条，0 失败 |
| 2026-07-26 22:02 CST | Admin/Report 测试编译 | 44 模块 `-Penable-tests -DskipTests test-compile` | JDK 17 Maven Reactor | 通过 | 44/44 SUCCESS；测试源码全部编译 |
| 2026-07-26 22:07 CST | Admin UI | Node 20.19.0 `pnpm build` | Vite 生产构建 | 通过 | 8725 模块，1 分 33 秒；保留既有组件命名、CSS 注释和分块警告 |
| 2026-07-26 22:08 CST | Admin/Report 聚合包 | `mvn -pl forge-admin-server,forge-report-server -am package -DskipTests` | JDK 17 Maven Reactor | 通过 | 44/44 SUCCESS，38.178 秒 |
| 2026-07-26 22:09 CST | 最终静态安全证据 | 敏感值、Flyway、初始化 SQL、死代码、协议保留、diff | `rg`/`find`/`git diff --check` | 通过 | 受管范围历史根密钥和旧种子密文均 0；Flyway 无重复/占位符；SessionKeyStore 链路保留 |
| 2026-07-27 | Task 13 启动引导 | 首启/重启/显式优先/空变量/损坏/并发/权限/SpringApplication 自动注册 | Starter Crypto Reactor 目标测试 | 通过 | 自动引导 8/8；与原配置校验合计 15/15；不启动真实服务 |
| 2026-07-27 | Docker 密钥持久化 | Compose 环境变量与 `crypto_secrets` 命名卷 | `docker compose config --quiet` | 通过 | 未构建镜像、未启动容器 |

### 基础设施警告

- 直接插件测试会解析本地仓库中的陈旧 `forge-starter-crypto` jar，目标测试编译失败；最终证据使用已成功编译的 Reactor `target/classes` 置于 classpath 首位执行。
- Generator 常规 Reactor `test` 仍受仓库既有 Surefire `groups/excludedGroups` 与测试引擎配置影响；44 模块 `test-compile` 和隔离 JUnit 结果已分别覆盖编译与行为。
- 一次聚合 package 未显式固定 JDK，落到系统 Java 8 后以“无效的目标发行版: 17”失败；同一命令固定 JDK 17 后通过，不属于代码失败。
- 未启动后端、前端开发服务器、数据库或 Redis；未产生需要停止的服务 PID。

## 6. 执行证据

- `execution-log.md`：逐 Task 追加命令、关键输出、失败根因和跳过项。
- 关键接口：`/api/config/manage/crypto`、`/api/config/group/**`、crypto migration inventory/execute、数据连接和低代码既有 CRUD。
- 关键数据库检查：`sys_config`/`sys_config_group` 敏感字段清理，`password_cipher` 和动态字段格式分类；真实结果仅在隔离测试库/生产变更单留档。
- 服务启动与停止：默认不启动服务；如需装配验证，只停止本轮明确启动的 PID。
