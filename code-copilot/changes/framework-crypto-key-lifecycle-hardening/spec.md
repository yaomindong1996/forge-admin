# 框架加密密钥生命周期加固
> status: review
> created: 2026-07-26
> complexity: 🔴复杂

## 1. 背景与目标

当前 Admin 与 Report 的默认配置将同一根密钥明文提交到 Git，配置中心又允许从数据库、管理 API 和前端表单读取或修改该密钥。进一步排查确认，该密钥不仅是 API 动态密钥不可用时的降级密钥，还间接用于加密持久化数据：数据连接密码和低代码动态业务字段。

因此不能直接替换旧密钥。直接替换会导致 `ai_report_data_connection.password_cipher` 及已经启用 `ai_crud_config.encrypt_config` 的业务字段无法解密。本变更按兼容发布、启用新写入、存量迁移与退役三个里程碑完成以下可验证结果：

1. 仓库、数据库动态配置、配置管理 API 和管理端 UI 不再保存、返回或编辑根密钥、RSA 私钥及持久化密钥环。
2. 密钥只允许由环境变量或外部配置文件注入；需要密钥的能力启用但密钥缺失/非法时，应用启动失败并给出明确配置名。
3. 新增版本化持久化密文协议，密文携带算法和 `keyId`；活动密钥负责新写入，历史密钥只负责解密。
4. 数据连接密码和低代码加密字段统一使用持久化加密服务，兼容旧无版本密文；解密失败必须失败关闭，不再把密文当明文使用或静默返回。
5. 通过显式开关控制“旧格式写入 → 新格式写入”，支持多实例先完成兼容版本发布，再切换写入格式，避免滚动发布期间新旧节点互不兼容。
6. 提供只读盘点、预检和显式执行的存量迁移能力。迁移前检查密钥、算法、表/列/主键、数据源写权限及字段容量；迁移失败不能吞掉或覆盖原值。
7. 只有盘点报告中旧格式、未知格式、缺失密钥和迁移失败数均为 0，才允许移除旧泄露密钥。
8. 浏览器会话密钥协商协议保持不变；本变更不移除 `SessionKeyStore` 和前端会话密钥状态。

本变更不执行生产数据库迁移、不提交真实密钥、不重写 Git 历史。新安装可由启动引导器首次生成并持久化运行密钥；已有环境的 legacy key、生产 Secret Manager 注入和迁移命令执行仍由部署人员在代码验证通过后完成。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- 根密钥配置：
  - `forge-server/forge-admin-server/src/main/resources/application.yml#forge.crypto.secretKey`
  - `forge-server/forge-report-server/src/main/resources/application.yml#forge.crypto.secretKey`
- 配置绑定：`forge-starter-core/.../CryptoProperties` 将根密钥绑定到 `secretKey`。
- 数据库配置优先级：`forge-starter-config/.../DbPropertySourcePostProcessor#postProcessEnvironment` 通过 `addFirst` 注册数据库配置源，优先级高于环境和 YAML。
- 配置分组拍平：`forge-starter-config/.../ConfigConverter#convertCryptoConfig` 将 `secretKey`、RSA 公私钥写入 `forge.crypto.*`。
- 配置管理入口：
  - `ConfigManageController#getCryptoConfig/updateCryptoConfig`
  - `SysConfigGroupController` 的 `/api/config/group/**` 通用 CRUD
  - `SysConfigController` 的 `/system/config/**` 散配置 CRUD
  - `forge-admin-ui/src/views/system/config-center.vue` 的加密配置表单
- 加密器：`EncryptorFactory` 注册 `SM4Encryptor` 与 `AESEncryptor`；无参 `encrypt/decrypt` 从 `CryptoProperties.secretKey` 取默认密钥。
- API 传输加密：`EncryptResponseBodyAdvice`、`DecryptRequestBodyAdvice` 优先使用浏览器会话密钥，无会话密钥时回退默认根密钥。
- 数据连接密码：
  - `DataConnectionController#convertToEntity` 用默认根密钥加密并写入 `ai_report_data_connection.password_cipher`。
  - `JdbcDataSourceProvider#decryptPassword` 用默认根密钥解密后创建 Hikari 数据源。
- 低代码持久化字段：`DynamicCrudService#applyEncrypt/applyDecrypt` 按 `ai_crud_config.encrypt_config` 中的算法，用默认根密钥处理动态业务表字段。
- 固定密钥死代码：`EncryptTypeHandler` 内含固定密钥；唯一引用是 `SysClient` 中已注释的 `@TableField`。

### 2.2 现有实现

- Admin 与 Report YAML 提交了同一 Base64 根密钥，Git 历史已永久记录该值。
- `sys_config_group` 初始化数据包含 `secretKey`、`rsaPrivateKey` 等字段；通用配置分组接口会把原始 `config_value` 返回前端并允许整体覆盖。
- `/system/config` 已对敏感键查询结果做脱敏，但 `SysConfigServiceImpl#insertConfig/updateConfig` 仍允许新增或修改 `forge.crypto.*` 部署级密钥，`DbConfigLoader#loadSysConfig` 会把 `config_type='Y'` 的值加载为高优先级配置。
- `ConfigManagerService#getCryptoConfig` 直接反序列化为 `CryptoProperties`，因此管理 API 会序列化运行密钥字段。
- `CryptoProperties` 没有启动期校验。错误密钥通常直到某次加解密才报错，故障被推迟到运行期。
- `JdbcDataSourceProvider#decryptPassword` 捕获解密异常后返回原始密文，随后把密文作为 JDBC 密码使用，掩盖真实的密钥故障。
- `DynamicCrudService#applyEncrypt/applyDecrypt` 捕获顶层异常后只记 WARN；写入可能继续保存明文，读取可能继续返回密文/明文，无法形成可靠安全边界。
- 现有无版本密文仅有 Base64 载荷，没有算法或密钥 ID。数据连接依赖全局默认算法，低代码字段依赖动态 `encrypt_config.algorithm` 才能确定旧密文算法。
- 初始化 SQL 已包含数据连接密码密文；当前仓库种子中的 `encrypt_config` 大多为空，但不能据此推断生产库不存在低代码加密字段。
- 高风险审批模块已有 `VersionedKekCapabilityPayloadCrypto`，证明项目接受“活动 keyId + 历史 key map + 缺钥失败关闭”的模型；该实现服务于审批载荷，不直接复用其数据格式。

### 2.3 发现与风险

1. **旧密钥已经泄露，隐藏不等于轮换。** 删除 YAML 常量只能阻止继续扩散，必须完成新写入和存量重加密后才能闭环。
2. **数据库配置可覆盖环境配置。** 若只改 YAML，历史 `sys_config`/`sys_config_group` 仍可让旧密钥继续生效。
3. **持久化密文存在兼容性约束。** 立即轮换会让数据连接和低代码字段不可读。
4. **多实例存在协议切换窗口。** 旧版本节点无法识别带版本前缀的新密文，必须先让所有节点具备双读能力，再切换新写入。
5. **动态表无法用一条 Flyway SQL 完成迁移。** 表名、字段、算法、数据源和字段长度都来自运行配置，必须先盘点和预检，再按配置受控迁移。
6. **旧密文不具备完整性和自描述能力。** 无法仅凭字符串可靠区分“旧密文、历史明文、损坏数据”；解密失败必须计入阻塞项，禁止猜测后覆盖。
7. **版本前缀会增加存储长度。** 低代码字段迁移前必须校验实际新密文长度不超过目标列容量。
8. **迁移涉及敏感数据写回。** 必须按租户、配置和批次执行，使用原值比较避免覆盖并发修改，日志不得打印明文、密文或密钥。

## 3. 功能点

### 里程碑 A：来源与暴露面加固

- [x] 两处 YAML 根密钥改为 `${FORGE_CRYPTO_SECRET_KEY:}`，Docker 示例只声明变量名，不提供默认密钥。
- [x] 增加启动校验：启用 API/字段默认加密、旧格式持久化写入或旧密文兼容读取时，所需密钥必须存在且满足算法长度。
- [x] 配置中心改用不含密钥字段的管理 DTO；管理端删除根密钥和 RSA 私钥输入框。
- [x] `ConfigConverter` 不再从数据库配置分组生成任何根密钥、RSA 私钥或持久化密钥环属性。
- [x] `/api/config/group/**` 对 `crypto` 分组统一做敏感字段清洗；任何写入敏感字段的请求明确拒绝，不静默保存。
- [x] `/system/config/**` 禁止新增、修改或改名为部署级 crypto 密钥键，避免通过散配置表重新建立覆盖。
- [x] Flyway `V1.0.52` 从 `sys_config` 和 `sys_config_group` 清除历史敏感配置字段；安全清理使用物理删除并记录原因。
- [x] 删除 `EncryptTypeHandler` 及 `SysClient` 中未使用的 import/注释引用。
- [x] 新安装缺少显式 Secret 时，启动前自动生成独立传输/持久化密钥，以外部文件稳定复用；Docker 通过持久化卷共享。

### 里程碑 B：版本化持久化密文

- [x] 在 `CryptoProperties` 增加持久化密钥配置：启用状态、版本化写入开关、活动 keyId/密钥、历史解密密钥和旧无版本密钥兼容开关。
- [x] 新增 `PersistentCryptoService`，格式固定为 `FPC1:<algorithm>:<keyId>:<payload>`；keyId 只允许安全字符，格式异常和未知 keyId 失败关闭。
- [x] `writeVersioned=false` 时保持旧格式写入，供兼容版本首次滚动发布；`true` 时只用活动密钥写 `FPC1`，读取同时支持 `FPC1` 和旧格式。
- [x] 数据连接新增/修改改用 `PersistentCryptoService`；`JdbcDataSourceProvider` 解密失败抛业务异常，不把密文当 JDBC 密码。
- [x] 低代码 `applyEncrypt/applyDecrypt` 改用 `PersistentCryptoService`；配置或密文错误中止当前写/读操作，不再吞异常继续。
- [x] API Advice、Jackson 字段序列化和浏览器会话密钥继续使用现有 `EncryptorFactory`，不改传输协议。

### 里程碑 C：盘点、迁移与退役门禁

- [x] 增加平台管理员专用的迁移盘点接口，按当前租户统计数据连接和低代码字段的 `LEGACY`、`ACTIVE`、`HISTORICAL`、`UNKNOWN`、`BLOCKED` 数量，只返回计数和定位元数据。
- [x] 数据连接迁移支持 dry-run 和显式 execute，按批读取、解密并写为活动 keyId，更新条件包含 `id + 原密文`。
- [x] 低代码迁移按 `configKey` 显式选择，解析 `encrypt_config`、运行数据源、表、主键和字段；先校验标识符、写权限、列容量和单主键，再允许分批迁移。
- [x] 迁移接口要求 `expectedActiveKeyId` 与运行配置一致；默认 `dryRun=true`，执行模式必须由平台管理员明确提交。
- [x] 单行迁移失败只回滚当前批次并进入失败报告，不打印敏感值；重复执行只处理非活动格式，具备幂等性。
- [x] 输出部署与轮换 Runbook：兼容版本全节点发布 → 配置新 keyring → 开启版本化写入 → dry-run → 执行迁移 → 再次盘点 → 移除旧钥。

## 4. 业务规则

1. 密钥不得存入 Git、Flyway、初始化 SQL、`sys_config`、`sys_config_group`、接口响应、浏览器状态或日志。
2. 根密钥和持久化 keyring 只能来自启动引导器的外部持久化文件、环境变量、挂载的外部配置或 Secret Manager 注入。
3. 活动持久化 keyId 必须匹配 `[A-Za-z0-9_-]{1,32}`；活动密钥和历史密钥必须为 Base64 编码且解码后满足算法要求。为同时兼容 SM4/AES，本阶段持久化 keyring 统一使用 16 字节密钥。
4. 版本化密文只信任内嵌算法和 keyId；调用方传入的 `legacyAlgorithm` 只用于无版本旧密文。
5. 无版本旧密文只有在 `legacy-read-enabled=true` 且旧密钥存在时才允许读取，否则失败关闭。
6. `write-versioned=false` 仅用于首次兼容发布，不是长期运行模式；生产启用新 keyring 后必须切换为 true。
7. 未完成全节点兼容发布前禁止开启版本化写入；回滚期间必须保留能够读取新旧格式的应用版本和全部相关密钥。
8. 数据连接、低代码读写和迁移中任何解密异常都必须返回安全错误；禁止把密文当明文、禁止静默跳过写入加密。
9. 迁移只处理能够成功解密并通过容量检查的旧密文。未知/损坏/疑似明文值必须报告并人工确认，禁止自动覆盖。
10. 低代码迁移必须显式指定 `configKey`，只处理当前租户；只读运行数据源、无单主键、字段不存在或列容量不足时只报告阻塞，不执行写入。
11. 旧密钥退役门禁：所有租户、所有数据连接、所有非空 `encrypt_config` 的盘点中，`LEGACY + UNKNOWN + BLOCKED + FAILED = 0`。
12. 本地自动化验证不连接生产库、不生成或提交生产密钥、不启动真实服务；测试仅在临时目录生成一次性随机值验证引导合同。
13. 启动引导只在传输根密钥未显式配置时生效；已注入的非空环境变量或 JVM 参数优先，不得被自动文件覆盖。
14. 自动密钥文件必须原子创建并稳定复用；已有文件损坏或缺字段时启动失败，禁止删除后静默换钥。
15. 自动生成的新安装默认开启 `FPC1` 写入并关闭 legacy read；已有历史密文的升级环境必须显式提供原 legacy key 和兼容开关。
16. 多实例必须使用同一套 Secret Manager 配置，或挂载同一持久化密钥文件。自动文件引导会在同目录创建锁文件并收紧权限，因此挂载目录必须可写；只读 Secret 注入应使用显式环境/JVM 配置并跳过文件引导。每个节点在独立本地目录自动生成不构成可用的集群方案。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 清理 | `sys_config` | `config_key` | 物理删除根密钥、RSA 私钥和持久化 keyring 对应散配置；这是敏感信息清除例外，回滚时从外部 Secret 重新注入，禁止恢复入库 |
| 清理 | `sys_config_group` | `config_value` JSON | 对 `group_code='crypto'` 使用 `JSON_REMOVE` 删除 `secretKey`、`rsaPrivateKey`、`persistence` 等敏感节点，保留非敏感功能开关 |
| 更新 | 初始化 SQL | crypto 分组 JSON | 新装库不再包含敏感字段占位，避免通用接口继续暴露密钥模型 |
| 数据迁移 | `ai_report_data_connection` | `password_cipher` | 运行期受控迁移，不由 Flyway 解密；字段 `varchar(500)` 保持不变 |
| 数据迁移 | 动态业务表 | `encrypt_config` 指定字段 | 运行期按配置、数据源和租户迁移；不自动执行 DDL，容量不足时阻塞并报告 |

Flyway 版本：`forge-server/db/migration/V1.0.52__remove_database_crypto_secrets.sql`。脚本不得包含真实密钥，必须使用表存在检查/JSON 有效性判断，且可重复执行核心清理语句。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 收紧 | `/api/config/manage/crypto` | GET | 返回不含 `secretKey`、`rsaPrivateKey`、持久化 keyring 的管理 DTO |
| 收紧 | `/api/config/manage/crypto` | PUT | 只接受非敏感功能开关；请求包含部署级密钥字段时拒绝 |
| 收紧 | `/api/config/group/**` | GET/POST/PUT | crypto 分组响应清洗敏感 JSON；写入敏感字段拒绝 |
| 收紧 | `/system/config/**` | POST | 新增/修改散配置时拒绝部署级 crypto 密钥键；现有普通敏感配置继续沿用脱敏编辑语义 |
| 新增 | `/api/config/manage/crypto/migration/inventory` | POST | 平台管理员按当前租户和可选 `configKeys` 执行只读盘点 |
| 新增 | `/api/config/manage/crypto/migration/execute` | POST | 平台管理员显式执行迁移，默认 dry-run，校验 `expectedActiveKeyId` |
| 内部兼容 | 数据连接 CRUD/连接测试 | 既有 | 协议不变；密码密文改为版本化持久化服务处理 |
| 内部兼容 | 低代码动态 CRUD | 既有 | 协议不变；加密字段支持新旧格式，错误改为失败关闭 |

迁移响应只包含租户、来源、configKey、表/字段、格式分类、计数、阻塞原因和批次结果；不得返回明文、密文、密钥或可逆摘要。

## 7. 影响范围

- `forge-starter-core`：扩展 `CryptoProperties` 持久化密钥配置，增加共享的部署级 crypto 密钥键策略。
- `forge-starter-crypto`：启动校验、版本化持久化密文服务、格式解析和单元测试；删除死 TypeHandler。
- `forge-starter-config`：安全管理 DTO、配置转换、通用配置分组清洗/拒绝、迁移编排入口及测试。
- `forge-plugin-data`：数据连接持久化加密、失败关闭、盘点/迁移贡献器及 Mapper XML。
- `forge-plugin-generator`：动态 CRUD 持久化加解密、低代码盘点/迁移贡献器和动态数据源预检。
- `forge-plugin-system`：封堵 `sys_config` 散配置写入旁路，删除 `SysClient` 的死 import/注释。
- `forge-admin-server`、`forge-report-server`：环境变量配置和装配验证。
- `forge-admin-ui`：配置中心删除部署级密钥编辑项。
- `forge-server/db/migration`、两份初始化 SQL、`docker`、`forge-docs`：历史敏感配置清理与部署 Runbook。

不涉及前端浏览器 `CryptoRuntimeConfig` 白名单、RSA 会话交换路径、Flow 服务协议、文件存储 AK/SK、生成器数据源 `GenDatasourcePasswordCodec` 和高风险审批独立 KEK。

## 8. 风险与关注点

- **安全变更**：旧根密钥已泄露，在完成迁移前只能作为临时只读兼容密钥，不得继续视为安全密钥。
- **数据变更**：持久化密文迁移会更新数据连接和业务字段，必须 dry-run、分批、可重入，并由平台管理员审查报告。
- **多实例发布**：错误的发布顺序会造成旧节点无法读取新密文。Runbook 和开关默认值必须强制兼容优先。
- **动态数据源**：外部库可能只读、网络不可达或使用不同数据库方言；盘点/迁移按配置隔离失败，不影响其他配置。
- **字段容量**：版本头增加长度。容量不足必须先由业务所有者评估扩列，迁移器不自动修改动态表 DDL。
- **历史明文**：旧实现会吞掉加密失败，生产数据可能混有明文。此类值归类为 UNKNOWN，必须人工处置。
- **回滚**：迁移后不能回滚到只识别旧格式的代码。回滚只允许回到本变更的兼容版本，并保留活动和历史密钥。
- **Git 历史**：本变更不执行 `git filter-repo`。旧值仍可从历史读取，因此必须以“密钥已轮换并退役”作为最终安全边界。

## 8.5 测试策略

- **测试范围**：配置校验、格式解析、活动/历史/旧密钥读写、篡改/未知 keyId/非法算法失败关闭、配置 API 清洗与拒绝、Flyway 清理合同、数据连接读写和迁移、低代码动态字段新旧格式及迁移预检、前端构建、敏感值静态扫描。
- **覆盖率目标**：新增持久化加密协议和迁移状态分支场景覆盖 100%；不设仓库总行覆盖率阈值。
- **独立 Test Spec**：是，见 `test-spec.md`。
- **增量构建**：Starter Crypto/Config、Plugin Data/Generator 目标测试，Admin/Report 聚合 package，Node 20.19.0 前端 build。
- **数据库验证**：自动测试使用 Mock/内存测试或 SQL 合同测试，不连接生产数据库；真实迁移 E2E 由部署人员在备份后的测试库执行。

## 9. 待澄清

- 无。新安装自动引导直接使用 `write-versioned=true` 且关闭 legacy read；已有密文的升级仍使用保守的双读和分阶段写入流程。迁移接口 `dryRun=true`，低代码容量不足不自动扩列，生产真实迁移不在本地执行。

## 10. 技术决策

1. **区分传输根密钥与持久化 keyring。** `EncryptorFactory` 继续服务 API/会话兼容；`PersistentCryptoService` 专门服务数据库密文，避免以后轮换传输密钥再次影响存量数据。
2. **采用紧凑版本头而非一次性替换。** `FPC1:<algorithm>:<keyId>:<payload>` 复用现有 SM4/AES 实现，新增最小元数据支持轮换；本阶段不顺带升级加密模式，避免把算法重构与密钥迁移耦合。
3. **活动密钥单写、keyring 多读。** 新密文只能用活动 keyId；历史 key 只能解密，禁止继续写入。
4. **旧格式算法由调用方上下文提供。** 数据连接使用全局 legacy algorithm，低代码使用对应字段的 `encrypt_config.algorithm`；版本化格式以密文内算法为准。
5. **两阶段写入开关。** 先部署双读代码，再开启新写入，显式解决滚动发布兼容问题。
6. **迁移不放进 Flyway。** Flyway 只清理配置元数据；业务密文需要运行密钥和动态数据源，采用管理员 dry-run/execute 机制。
7. **迁移不自动扩列。** 自动 DDL 会放大动态数据源风险；容量不足以阻塞报告交给业务所有者处理。
8. **失败关闭。** 数据连接与低代码不再将解密失败解释为明文，避免错误密钥造成隐蔽的数据泄露或连接故障。
9. **数据库敏感配置物理清理。** 该删除点属于泄露凭据清除，不需要可恢复；回滚来源只能是外部 Secret，禁止恢复到数据库。
10. **每个实施 Task 独立提交。** 每个里程碑完成后先做 Spec 合规检查和目标测试，再进入下一里程碑。
11. **启动前自动引导而非运行期写环境变量。** `EnvironmentPostProcessor` 在配置绑定前将显式配置或持久化密钥文件加入 Spring Environment；Java 进程不尝试修改父 Shell 环境。
12. **首次生成必须稳定持久化。** 采用进程内锁、文件锁、临时文件和原子移动；文件存在后只校验和复用，损坏时失败关闭。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Proposal | 完成 | `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` | 用户明确要求继续完成剩余任务，HARD-GATE 已通过 |
| Task 1-5 | 完成 | Core/Crypto/Config/System、YAML、Docker、UI、初始化 SQL、Flyway | 清除配置来源与管理暴露面；补充方括号 Map 键旁路防护和非法 JSON 失败关闭 |
| Task 6-8 | 完成 | Persistent Crypto、Data、Generator | `FPC1` 双读/分阶段写入接入数据连接和低代码字段；读取失败关闭 |
| Task 9-11 | 完成 | Data/Generator migration、Admin migration API | 当前租户盘点、默认 dry-run、全 scope 预检、批次原子事务和原值比较更新 |
| Task 12 | 完成 | Runbook、测试、构建、静态扫描、两阶段审查 | 代码就绪；生产迁移、全租户归零和旧钥退役仍为部署门禁 |
| Task 13 | 完成 | Starter Crypto 启动引导器/注册/测试、Admin/Report YAML、Docker 持久化卷、Runbook | 新安装无需手工 export；历史 legacy key 仍需从既有 Secret 来源提供 |

## 12. 审查结论

- **阶段一 Spec Compliance：PASS**。里程碑 A/B/C 的代码、Flyway、UI 和 Runbook 均已实现；浏览器 `SessionKeyStore` 协议保留，默认仍为兼容写入，未执行生产迁移或密钥退役。
- **阶段二 Code Quality：PASS_WITH_COMMENTS**。审查修复了批次非原子写入、跨 scope 写前预检缺失、非法 crypto JSON 原文返回、Spring Map 方括号键名旁路、空历史密钥和 PostgreSQL 字符容量误判；最终未发现阻塞性正确性或安全问题。
- **验证结论**：JDK 17 下 44 模块 `test-compile` 和 Admin/Report 聚合 package 通过；隔离矩阵 59 条测试通过；Node 20.19.0 前端 8725 模块生产构建通过；敏感值、Flyway、死代码和协议保留静态检查通过。
- **保留项**：仓库既有 Surefire groups/测试引擎配置和本地陈旧 Starter jar 会阻断部分直接模块测试，已使用 Reactor `target/classes` 隔离运行目标测试。低代码运行时当前只注册 MySQL、PostgreSQL、Oracle 方言，本变更验证这三种现有方言，未扩展 SQL Server 整体运行时支持。
- **部署待办**：真实 Secret 注入、测试库/生产 dry-run 与 execute、所有租户归零报告、观察窗口、旧密钥移除和传输根密钥轮换均未在本地执行。
- **Task 13 增量验证**：Starter Crypto Reactor 编译成功；自动引导 8/8，引导+原配置校验 15/15；最小 SpringApplication 合同确认无手工 export 也会在 Context 刷新前生成并注入；`docker compose config --quiet` 通过。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-26
- **确认人**：用户（明确要求“继续完成剩余任务”）
