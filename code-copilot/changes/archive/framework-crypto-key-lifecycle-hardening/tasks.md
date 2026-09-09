# 任务拆分 — 框架加密密钥生命周期加固
> status: complete
> 拆分顺序：配置合同 → 暴露面 → 持久化协议 → 业务接入 → 迁移 → 部署验收
> 每个 Task 独立验证并提交；里程碑 A/B/C 分别完成一次阶段检查

## 前置条件

- [x] 已确认根密钥的直接与间接调用链。
- [x] 已确认存在数据连接持久化密文和动态低代码持久化字段。
- [x] 已确认数据库配置源优先于环境/YAML。
- [x] 已确认浏览器会话密钥协议不在移除范围。
- [x] 用户完成 Proposal HARD-GATE 确认。
- [x] `/apply` 前设置 JDK 17，并先运行现有目标测试基线。
- [x] 不使用真实生产密钥，不连接或修改生产数据库。

## 执行状态

- [x] Task 1：建立密钥配置合同与启动校验。
- [x] Task 2：外部化应用密钥并补部署说明。
- [x] Task 3：收紧配置管理 API 与数据库配置源。
- [x] Task 4/4B：封堵配置分组和 `/system/config` 散配置旁路，清理数据库历史值。
- [x] Task 5：清理 UI、初始化数据和固定密钥死代码。
- [x] Task 6：实现 `FPC1` 版本化持久化密文服务。
- [x] Task 7/7B：接入数据连接写入与读取失败关闭。
- [x] Task 8：接入低代码动态字段链路。
- [x] Task 9：实现数据连接盘点与批次原子迁移。
- [x] Task 10：实现低代码盘点、预检与批次原子迁移。
- [x] Task 11：提供管理员迁移入口与全 scope 写前预检。
- [x] Task 12：完成 Runbook、聚合验证和两阶段审查。
- [x] Task 13：实现启动前密钥自动引导、稳定持久化和 Docker 共享卷。
- [x] Task 14：修复 Admin/Flow 多 JVM 读取同一自动密钥文件时的排他锁冲突。
- [ ] 部署门禁：生产 Secret 注入、真实迁移、所有租户归零、观察窗口和旧钥退役；不属于本地 `/apply` 执行范围。

## 里程碑 A：来源与暴露面加固

### Task 1：建立密钥配置合同与启动校验

- **目标**：明确传输根密钥和持久化 keyring 的配置模型，在能力启用但密钥缺失/非法时启动失败。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/context/CryptoProperties.java` — 增加 persistence 配置、旧格式兼容与版本化写入开关。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/config/CryptoConfigurationValidator.java` — 新增，校验 Base64、长度、activeKeyId 和开关依赖。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/config/CryptoAutoConfiguration.java` — 注册并在初始化阶段执行校验。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/test/java/com/mdframe/forge/starter/crypto/config/CryptoConfigurationValidatorTest.java` — 新增 Red/Green 测试。
- **关键签名**：
  ```java
  public final class CryptoConfigurationValidator {
      public void validate(CryptoProperties properties);
  }
  ```
- **验证**：
  - 根密钥/活动 key 缺失、非法 Base64、错误长度、非法 keyId 均失败。
  - 相关能力关闭时不要求无关密钥。
  - `mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-crypto -am test`。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 建立密钥配置与启动校验`

### Task 2：外部化应用密钥并补部署说明

- **目标**：删除仓库内根密钥常量，为 Admin/Report/Docker 提供无默认值的外部注入合同。
- **涉及文件**：
  - `forge-server/forge-admin-server/src/main/resources/application.yml` — 根密钥改为环境变量，加入持久化兼容开关和 keyring 变量。
  - `forge-server/forge-report-server/src/main/resources/application.yml` — 同步外部化配置。
  - `docker/.env.example` — 仅增加变量名和生成说明，不提供可用密钥。
  - `docker/docker-compose.yml` — 向 Admin/Report 实际存在的容器传递变量；若当前 compose 不部署 Report，则只配置 Admin 并在文档说明。
  - `forge-docs/backend/modules/crypto.md` — 修正过期示例，说明 Secret 注入、双读切换和退役门禁。
- **验证**：
  - YAML 解析通过。
  - 精确扫描受管源码/配置中不存在历史根密钥值。
  - 空配置与合法测试配置的启动校验由 Task 1 测试覆盖。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 外部化应用加密密钥`

### Task 13：启动前自动密钥引导

- **目标**：新安装在未显式注入密钥时，由 Spring 配置绑定前的引导器自动生成一次并持久化，后续启动稳定复用。
- **优先级**：非空系统环境/启动参数 > 已有外部密钥文件 > 首次原子生成。
- **安全边界**：传输根密钥与持久化活动密钥独立生成；文件权限为 `0600`，目录尽可能为 `0700`；自动引导目录必须可写以支持同目录文件锁，损坏、空值、非法 Base64 或无法持久化时失败关闭，禁止静默换钥。
- **Docker**：Admin 容器挂载持久化 `crypto_secrets` 卷，密钥文件路径固定为 `/var/lib/forge/secrets/crypto.properties`；容器重建不重新生成。
- **升级限制**：历史密文的 legacy key 无法由程序推导；有历史密文的环境必须由 Secret Manager/既有部署配置提供原密钥，引导器不得伪造兼容成功。
- **验证**：首次生成、二次复用、显式配置优先、损坏文件失败、并发首启和文件权限合同。

### Task 14：多服务共享读取自动密钥文件

- **目标**：Admin、Flow 等独立 JVM 指向同一完整 `crypto.properties` 时允许并发读取，同时保持首次生成和兼容升级的单写安全。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/config/CryptoSecretEnvironmentPostProcessor.java` — 将完整文件读取改为共享锁；需要创建或补齐字段时释放共享锁、获取排他锁并重新读取。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/test/java/com/mdframe/forge/starter/crypto/config/CryptoSecretEnvironmentPostProcessorTest.java` — 增加跨 JVM 共享读取回归测试并保留首次并发创建测试。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/test/java/com/mdframe/forge/starter/crypto/config/CryptoSecretBootstrapProcess.java` — 提供独立 JVM 启动引导测试入口。
- **执行步骤**：
  - [x] 在父 JVM 持有锁文件共享锁时启动子 JVM 读取完整密钥文件，确认旧实现因申请排他锁而超时失败。
  - [x] 锁文件通道增加读权限，完整文件在 `lock(0, Long.MAX_VALUE, true)` 保护下读取、校验并直接返回。
  - [x] 文件不存在或缺少 Capability Pepper 时释放共享锁，再申请排他锁；排他锁内重新判断文件状态后才生成或原子写回。
  - [x] 运行跨 JVM 回归、首次并发创建、旧文件补字段、损坏文件失败关闭和 Starter Crypto 全量测试。
  - [x] 执行 Starter Crypto、Admin、Flow Server 聚合构建与 `git diff --check`，并回填 `test-spec.md` 与 `execution-log.md`。
- **验证**：完整文件共享读取不被另一 JVM 的共享锁阻塞；写路径仍只有一个进程生效，密钥原值不变化且最终文件字段完整。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] fix: 优化多服务密钥引导锁`

### Task 3：收紧配置管理 API 与数据库配置源

- **目标**：配置中心只管理非敏感开关，数据库配置不能覆盖部署级密钥。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/config/CryptoManageConfig.java` — 新增不含密钥字段的管理 DTO。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/service/ConfigManagerService.java` — 使用安全 DTO，保存时保留/清理非敏感字段。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/controller/ConfigManageController.java` — GET/PUT 改用安全 DTO并拒绝未知敏感字段。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/pom.xml` — 增加现有标准测试依赖，仅作用于 test scope。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/test/java/com/mdframe/forge/starter/config/CryptoConfigExposureTest.java` — 新增序列化与转换测试。
- **关键签名**：
  ```java
  public CryptoManageConfig getCryptoConfig();
  public boolean saveCryptoConfig(CryptoManageConfig config);
  ```
- **验证**：
  - GET 序列化不含 `secretKey`、`rsaPrivateKey`、`activeKey`、`keys`。
  - PUT 中出现未知部署级密钥字段时由 DTO 的显式未知字段校验拒绝，不依赖 Jackson 全局 unknown-property 开关。
  - Config Starter 目标测试通过。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 收紧加密配置管理接口`

### Task 4：封堵通用配置分组旁路

- **目标**：通用分组 CRUD 无法读写 crypto 敏感字段，数据库配置拍平也不再产生部署级密钥属性。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/converter/ConfigConverter.java` — 删除密钥、私钥和 keyring 拍平映射。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/security/CryptoConfigSanitizer.java` — 新增 JSON 清洗与敏感字段检测。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/controller/SysConfigGroupController.java` — list/page/detail/byCode 清洗，add/edit 拒绝敏感字段。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/test/java/com/mdframe/forge/starter/config/security/CryptoConfigSanitizerTest.java` — 新增旁路测试。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/test/java/com/mdframe/forge/starter/config/controller/SysConfigGroupCryptoGuardTest.java` — 新增各查询/写入入口测试。
- **验证**：
  - 各查询入口均不返回敏感 JSON。
  - add/edit 带敏感字段时返回明确错误。
  - JSON 转属性 map 不产生任何部署级密钥键。
  - 非 crypto 分组读写不受误伤。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 封堵加密配置分组旁路`

### Task 4B：封堵散配置旁路并清理数据库历史值

- **目标**：`sys_config` 不能重新写入部署级 crypto 密钥，Flyway 清除两种数据库配置模型中的历史敏感值。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/util/CryptoDeploymentSecretPolicy.java` — 新增共享的精确键/JSON 字段策略，避免把普通存储 AK/SK 一并禁用。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysConfigServiceImpl.java` — insert/update/键名变更时拒绝部署级 crypto 密钥。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/service/impl/SysConfigServiceCryptoGuardTest.java` — 新增新增、修改、改名和普通敏感配置兼容测试。
  - `forge-server/db/migration/V1.0.52__remove_database_crypto_secrets.sql` — 清理 `sys_config` 和 `sys_config_group` 的敏感键。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/test/java/com/mdframe/forge/starter/config/migration/CryptoSecretRemovalMigrationContractTest.java` — 验证脚本不含真实密钥且覆盖清理合同。
- **验证**：
  - `/system/config` 不能新增、修改或改名为部署级 crypto 密钥键。
  - 普通敏感配置仍可按既有掩码保留语义维护，不被扩大禁止范围。
  - Flyway 脚本无 `${...}` 占位符、无真实密钥、包含 JSON 有效性保护。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 清理数据库加密密钥旁路`

### Task 5：清理 UI、初始化数据和固定密钥死代码

- **目标**：删除剩余可见编辑入口和不会被安全管理的固定密钥代码。
- **涉及文件**：
  - `forge-admin-ui/src/views/system/config-center.vue` — 删除根密钥、RSA 密钥编辑区和表单字段。
  - `forge-server/db/全量初始化SQL.sql` — crypto 分组 JSON 删除敏感字段占位。
  - `forge-server/forge-admin-server/sql/初始化脚本.sql` — 同步新装库初始化数据。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/handler/EncryptTypeHandler.java` — 删除文件。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysClient.java` — 删除未使用 import 和注释注解。
- **验证**：
  - `rg` 扫描配置中心不再出现部署级密钥表单字段。
  - 初始化 SQL 的 crypto 分组不含敏感字段名。
  - Node 20.19.0 前端 build 与相关 Java compile 通过。
- **里程碑检查**：完成 Spec 里程碑 A 合规检查，确认尚未切换新密文写入。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 清理密钥编辑与固定密钥死代码`

## 里程碑 B：版本化持久化密文

### Task 6：实现版本化持久化密文服务

- **目标**：提供活动 key 单写、历史 key 多读、旧无版本格式兼容的独立服务。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/persistence/PersistentCryptoService.java` — 新增接口。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/persistence/VersionedPersistentCryptoService.java` — 新增协议实现。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/persistence/PersistentCiphertext.java` — 新增严格格式解析与分类。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/config/CryptoAutoConfiguration.java` — 注册服务。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/test/java/com/mdframe/forge/starter/crypto/persistence/VersionedPersistentCryptoServiceTest.java` — 新增协议测试。
- **关键签名**：
  ```java
  public interface PersistentCryptoService {
      String encrypt(String plaintext, String algorithm);
      String decrypt(String ciphertext, String legacyAlgorithm);
      PersistentCiphertext inspect(String ciphertext, String legacyAlgorithm);
      String reencrypt(String ciphertext, String legacyAlgorithm);
  }
  ```
- **验证**：
  - `writeVersioned=false/true` 两种写入。
  - 活动、历史、旧格式往返。
  - 未知 keyId、非法头、非法算法、缺旧钥均失败关闭。
  - 不在异常或日志中泄露输入值。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 实现版本化持久化密文`

### Task 7：接入数据连接密码链路

- **目标**：把数据连接密码加密从 Controller 下沉到 Service，新写受版本化开关控制。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataConnectionService.java` — 增加保存 DTO 转换/密码更新合同，移出 Controller 加密细节。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/impl/DataConnectionServiceImpl.java` — 用 `PersistentCryptoService` 加密密码并保留空密码更新语义。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/pom.xml` — 增加现有标准测试依赖，仅作用于 test scope。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/test/java/com/mdframe/forge/plugin/data/service/DataConnectionServiceCryptoTest.java` — 新增加密写入和空密码保留测试。
- **验证**：
  - 新增/修改写入不保存明文，空密码更新保留原密文。
  - Data Plugin Service 目标测试通过。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 接入数据连接持久化密钥环`

### Task 7B：完成数据连接入口与读取失败关闭

- **目标**：Controller 只做协议编排，连接池同时支持新旧密文且错误密钥失败关闭。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/controller/DataConnectionController.java` — 删除直接 `EncryptorFactory` 使用，调用 Service 编排。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/support/JdbcDataSourceProvider.java` — 用持久化服务解密，失败抛安全异常。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/test/java/com/mdframe/forge/plugin/data/support/JdbcDataSourceProviderCryptoTest.java` — 新增旧/活动/历史格式和失败关闭测试。
- **验证**：
  - 旧、活动和历史格式均向 Hikari 提供正确明文。
  - 错误 key/损坏密文中止连接创建，不把密文当 JDBC 密码。
  - Data Plugin 目标测试与 Admin/Report compile 通过。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 加固数据连接密文读取`

### Task 8：接入低代码动态字段链路

- **目标**：低代码动态业务字段使用版本化持久化服务，错误不再静默降级。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — `applyEncrypt/applyDecrypt` 改用持久化服务并抛安全业务异常。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/crypto/LowcodeEncryptConfigParser.java` — 新增集中解析与字段/算法校验。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/crypto/LowcodeEncryptConfigParserTest.java` — 新增配置合同测试。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/DynamicCrudCryptoLifecycleTest.java` — 新增旧/新格式、混合数据和失败关闭测试。
- **验证**：
  - SM4/AES 字段、camel/snake 映射、新旧密文、非法配置、未知 keyId。
  - 写入失败不保留明文继续执行；读取失败不返回原始值。
  - Generator 目标测试通过。
- **里程碑检查**：完成 Spec 里程碑 B 合规检查；默认开关仍为兼容写入，部署 Runbook 才允许生产切换。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 接入低代码持久化密钥环`

## 里程碑 C：盘点、迁移与退役门禁

### Task 9：实现数据连接盘点与受控迁移

- **目标**：按当前租户 dry-run/execute 迁移数据连接密码，具备幂等和并发保护。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataConnectionCryptoMigrationService.java` — 新增盘点和迁移服务。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/mapper/DataConnectionMapper.java` — 增加迁移批次查询和原值比较更新签名。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataConnectionMapper.xml` — 明确 tenant/del_flag、批次查询和条件更新 SQL。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/test/java/com/mdframe/forge/plugin/data/service/DataConnectionCryptoMigrationServiceTest.java` — 新增 dry-run、execute、重跑和并发冲突测试。
- **关键签名**：
  ```java
  public CryptoMigrationReport inventory(Long tenantId);
  public CryptoMigrationReport migrate(Long tenantId, String expectedActiveKeyId,
                                       int batchSize, boolean dryRun);
  ```
- **验证**：Mapper XML 显式租户/逻辑删除过滤；执行更新包含 `id + password_cipher` 原值条件。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 增加数据连接密文迁移`

### Task 10：实现低代码盘点与受控迁移

- **目标**：按当前租户和显式 configKey 对动态表加密字段预检、盘点和迁移。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/crypto/LowcodeCryptoMigrationService.java` — 新增迁移编排、格式分类和容量预检。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/crypto/LowcodeCryptoMigrationRepository.java` — 新增参数化批次查询、原值比较更新和事务边界。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlRepository.java` — 补充可用字符容量元数据读取。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/crypto/LowcodeCryptoMigrationServiceTest.java` — 新增主/外部数据源、容量、只读、重跑和失败测试。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/crypto/LowcodeCryptoMigrationRepositoryTest.java` — 验证标识符白名单与参数化更新。
- **验证**：
  - 未显式 configKey、无单主键、只读数据源、列缺失/容量不足均只报告阻塞。
  - 不自动 DDL，不跨租户，不打印敏感值。
  - 对低代码运行时当前已注册的 MySQL/PostgreSQL/Oracle 方言执行分页和容量元数据合同检查；SQL Server 尚无完整运行时方言，不在本变更中局部扩展。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 增加低代码密文迁移`

### Task 11：提供管理员迁移入口与安全响应

- **目标**：统一暴露平台管理员专用 inventory/execute，默认 dry-run 并校验活动 keyId。
- **涉及文件**：
  - `forge-server/forge-admin-server/src/main/java/com/mdframe/forge/admin/crypto/CryptoMigrationRequest.java` — 新增请求 DTO。
  - `forge-server/forge-admin-server/src/main/java/com/mdframe/forge/admin/crypto/CryptoMigrationReport.java` — 新增无敏感值响应 DTO。
  - `forge-server/forge-admin-server/src/main/java/com/mdframe/forge/admin/crypto/CryptoMigrationCoordinator.java` — 在应用聚合层编排 Data/Generator 服务，避免 Starter 反向依赖业务插件。
  - `forge-server/forge-admin-server/src/main/java/com/mdframe/forge/admin/crypto/CryptoMigrationController.java` — 映射 `/api/config/manage/crypto/migration` 并强制 `SessionHelper.assertAdmin`。
  - `forge-server/forge-admin-server/src/test/java/com/mdframe/forge/admin/crypto/CryptoMigrationControllerTest.java` — 新增鉴权、默认 dry-run、keyId 不匹配和响应脱敏测试。
- **验证**：非平台管理员拒绝；execute 未显式 `dryRun=false` 不写；响应 JSON 不含敏感字段。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 增加密文迁移管理入口`

### Task 12：完成部署 Runbook、聚合验证与审查

- **目标**：固化多实例发布/轮换/回滚顺序，完成共享安全能力的增量验收。
- **涉及文件**：
  - `forge-docs/backend/modules/crypto.md` — 补齐兼容发布、新写入、迁移、退役和回滚步骤。
  - `code-copilot/changes/framework-crypto-key-lifecycle-hardening/spec.md` — 回填执行和审查结论。
  - `code-copilot/changes/framework-crypto-key-lifecycle-hardening/tasks.md` — 回填 Task 状态。
  - `code-copilot/changes/framework-crypto-key-lifecycle-hardening/test-spec.md` — 回填实际验证。
  - `code-copilot/changes/framework-crypto-key-lifecycle-hardening/execution-log.md` — 记录命令、结果、跳过项和服务状态。
- **验证**：
  - Starter Crypto/Config、Plugin Data/Generator 目标测试。
  - Admin 与 Report 聚合 package（JDK 17）。
  - Admin UI build（Node 20.19.0）。
  - YAML/SQL/JSON 静态解析、Flyway placeholder 扫描、历史根密钥和部署级字段暴露扫描。
  - 两阶段 Review：Spec Compliance 后再做 Code Quality。
- **里程碑检查**：里程碑 C 代码就绪不等于生产旧钥已退役；实际生产盘点和迁移结果必须由部署人员留档。
- **提交信息**：`[framework-crypto-key-lifecycle-hardening] 完成密钥生命周期验证`
