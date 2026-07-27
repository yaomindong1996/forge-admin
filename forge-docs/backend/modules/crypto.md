# 加解密与持久化密钥轮换

Forge 的加密配置分为两个密钥域：

- 传输根密钥用于 API 动态会话密钥不可用时的兼容降级，不改变浏览器会话密钥协商协议。
- 持久化 keyring 用于数据连接密码和低代码动态字段，密文格式为 `FPC1:<algorithm>:<keyId>:<payload>`。

部署级密钥只能由启动引导器的外部持久化文件、环境变量、挂载的外部配置或 Secret Manager 注入。配置中心、`sys_config`、`sys_config_group`、初始化 SQL 和管理端 UI 均不得保存或编辑这些值。

## 配置合同

| 配置 | 环境变量 | 说明 |
|------|----------|------|
| `forge.crypto.bootstrap.enabled` | `FORGE_CRYPTO_BOOTSTRAP_ENABLED` | 未显式配置根密钥时是否启用自动引导，默认 `true` |
| `forge.crypto.bootstrap.file` | `FORGE_CRYPTO_BOOTSTRAP_FILE` | 自动密钥持久化文件；本地默认 `~/.forge/secrets/crypto.properties` |
| `forge.crypto.secret-key` | `FORGE_CRYPTO_SECRET_KEY` | API 传输兼容根密钥，Base64 编码 |
| `forge.crypto.persistence.enabled` | `FORGE_CRYPTO_PERSISTENCE_ENABLED` | 是否启用持久化密文服务 |
| `forge.crypto.persistence.write-versioned` | `FORGE_CRYPTO_PERSISTENCE_WRITE_VERSIONED` | `false` 写旧格式，`true` 只写 `FPC1` |
| `forge.crypto.persistence.legacy-read-enabled` | `FORGE_CRYPTO_PERSISTENCE_LEGACY_READ_ENABLED` | 是否允许读取旧无版本密文 |
| `forge.crypto.persistence.legacy-key` | `FORGE_CRYPTO_PERSISTENCE_LEGACY_KEY` | 旧无版本密文兼容密钥 |
| `forge.crypto.persistence.active-key-id` | `FORGE_CRYPTO_PERSISTENCE_ACTIVE_KEY_ID` | 新写入使用的活动 keyId |
| `forge.crypto.persistence.active-key` | `FORGE_CRYPTO_PERSISTENCE_ACTIVE_KEY` | 新写入使用的活动密钥 |

传输密钥按所选算法校验长度。持久化密钥为兼容 SM4/AES，统一使用 Base64 编码的 16 字节随机值；keyId 必须匹配 `[A-Za-z0-9_-]{1,32}`。不要在命令历史、日志、工单正文或版本库中记录生成结果。

历史版本化密钥通过外部配置维护，只用于读取，不得继续写入：

```yaml
forge:
  crypto:
    persistence:
      keys:
        <previous-key-id>: ${PERSISTENCE_PREVIOUS_KEY_SECRET}
```

生产集群优先由 Secret Manager 或编排平台提供同一套密钥。Docker Compose 的新安装无需手工生成，Admin 会自动初始化并将密钥文件保存到 `crypto_secrets` 命名卷；Report 独立部署时必须挂载同一文件或注入同一套可读 keyring。

## 启动自动引导

Starter Crypto 通过 `EnvironmentPostProcessor` 在 Spring `CryptoProperties` 绑定前完成密钥注入，不需要在启动前手工执行 `export`。处理顺序固定为：

1. 已存在非空 `FORGE_CRYPTO_SECRET_KEY` 或对应 JVM/Spring 参数：跳过自动文件引导，其余密钥和开关按显式配置及应用配置处理。
2. 根密钥未配置，但外部密钥文件已存在：校验并注入原值。
3. 根密钥和文件都不存在：首次原子生成独立的传输根密钥和持久化活动密钥，写入文件后注入 Spring Environment。

自动生成的新安装默认使用：

```text
forge.crypto.persistence.enabled=true
forge.crypto.persistence.write-versioned=true
forge.crypto.persistence.legacy-read-enabled=false
forge.crypto.persistence.active-key-id=bootstrap-<随机安全标识>
```

传输密钥和持久化密钥均为独立的 Base64 16 字节密钥。POSIX 系统上，密钥目录权限收紧为 `0700`，密钥和锁文件为 `0600`。首启使用进程内锁和文件锁串行化，写入使用临时文件和原子移动。

自动文件引导要求密钥文件所在目录可写，因为每次启动都需要创建或打开同目录锁文件并校验权限。多实例必须共享这一持久化目录。只读 Secret Manager 或只读容器 Secret 应改用显式环境/JVM 配置注入；检测到非空传输根密钥后，引导器会跳过自动文件处理。

已有密钥文件缺字段、非法 Base64、密钥长度错误、出现未允许配置键或无法持久化时，应用启动失败。引导器绝不删除损坏文件或静默换钥。

### 默认路径

| 运行方式 | 密钥文件 |
|----------|------------|
| 本地 Maven/IDE/JAR | `~/.forge/secrets/crypto.properties` |
| Docker Compose | `/var/lib/forge/secrets/crypto.properties`，保存在 `crypto_secrets` 卷 |
| 自定义编排 | `FORGE_CRYPTO_BOOTSTRAP_FILE` 指定的持久化挂载路径 |

密钥文件只注入当前 Java 进程的 Spring Environment，不会也无法修改父 Shell 的系统环境变量。这不影响配置绑定和应用自动启动。

### 已有环境升级

启动引导只能自动创建新密钥，无法从历史密文反推旧密钥。已有旧格式密文的环境必须通过 Secret Manager、容器 Secret 或既有部署配置提供原 `legacy-key`，并按下文发布与迁移流程处理。这是密码学约束，不能通过自动生成绕过。

## 发布与轮换

### 1. 兼容版本全节点发布

首次发布保持：

```text
FORGE_CRYPTO_PERSISTENCE_WRITE_VERSIONED=false
FORGE_CRYPTO_PERSISTENCE_LEGACY_READ_ENABLED=true
```

注入当前旧无版本密钥，逐批发布 Admin 和 Report，确认所有实例都已运行支持新旧双读的版本。此阶段仍写旧格式，不得提前开启版本化写入。

启动校验失败时先修复 Secret 注入，禁止临时把错误密钥写回数据库配置。Flyway `V1.0.52` 会物理清理数据库中的部署级 crypto 密钥；该清理不可回滚到数据库，恢复来源只能是外部 Secret。

### 2. 配置活动 keyring 并切换新写入

为所有实例注入新的活动 keyId 和活动密钥，继续保留旧无版本密钥及全部历史版本化密钥。确认全节点配置一致后，将：

```text
FORGE_CRYPTO_PERSISTENCE_WRITE_VERSIONED=true
```

重新滚动发布。新写入从此只产生 `FPC1`，读取仍兼容活动、历史和旧无版本格式。迁移接口在该开关为 `false` 时会拒绝执行。

### 3. 只读盘点

迁移接口只允许超级管理员访问，沿用现有浏览器会话加密协议。先按当前租户调用：

```http
POST /api/config/manage/crypto/migration/inventory
Content-Type: application/json

{
  "configKeys": ["<explicit-lowcode-config-key>"],
  "includeDataConnections": true,
  "includeLowcode": true
}
```

不传 `configKeys` 的 inventory 会盘点当前租户所有非空低代码加密配置；execute 必须显式提供低代码 `configKeys`。报告只包含计数和定位元数据，不包含明文、密文、密钥或可逆摘要。

逐租户检查 `LEGACY`、`HISTORICAL`、`UNKNOWN`、`UNKNOWN_KEY`、`BLOCKED`、`FAILED` 和 `CONFLICT`。低代码表缺失、非单主键、只读数据源、租户/逻辑删除列缺失、字符列容量不足均必须先处理，迁移器不会自动执行 DDL。

### 4. dry-run 与执行

先调用 execute 且保持 `dryRun=true`：

```http
POST /api/config/manage/crypto/migration/execute
Content-Type: application/json

{
  "expectedActiveKeyId": "<active-key-id>",
  "batchSize": 200,
  "dryRun": true,
  "configKeys": ["<explicit-lowcode-config-key>"],
  "includeDataConnections": true,
  "includeLowcode": true
}
```

确认活动 keyId、容量和可迁移计数后，在已备份并进入维护窗口的环境把 `dryRun` 显式改为 `false`。只迁移数据连接时设置 `includeLowcode=false`；低代码迁移禁止省略 `configKeys`。

迁移使用当前租户、逻辑删除过滤和“主键 + 原密文”比较更新。并发冲突记为 `CONFLICT`，不会覆盖新值。失败后修复原因并重跑；活动格式会被跳过，因此执行可重入。

### 5. 归零门禁与旧钥退役

对所有租户、所有数据连接和所有非空低代码 `encrypt_config` 再次 inventory。只有以下计数全部为 0，才允许进入退役：

```text
LEGACY + HISTORICAL + UNKNOWN + UNKNOWN_KEY + BLOCKED + FAILED + CONFLICT = 0
```

先将 `FORGE_CRYPTO_PERSISTENCE_LEGACY_READ_ENABLED=false` 滚动发布并观察，再移除旧无版本密钥。只有 `HISTORICAL=0` 后才能移除历史 keyring。每次删除密钥前都要保存不含敏感值的盘点报告、租户清单、应用版本和变更单编号。

旧泄露传输根密钥必须单独轮换。确认所有调用方支持新传输密钥后替换 `FORGE_CRYPTO_SECRET_KEY`；不要再把持久化 legacy key 回退绑定到已退役的传输根密钥。

## 回滚约束

- 尚未开启 `write-versioned=true` 时，可以回滚到前一应用版本，但必须保持旧无版本密钥可用。
- 一旦产生任何 `FPC1` 密文，禁止回滚到不识别 `FPC1` 的旧版本；只能回滚到本变更的兼容版本。
- 迁移期间和迁移完成后的观察窗口内，必须保留活动、历史和 legacy 读取所需密钥。
- 已从数据库清理的密钥不得恢复到 `sys_config` 或 `sys_config_group`。
- UNKNOWN、BLOCKED、FAILED 或 CONFLICT 不得通过删除旧钥强行清零，必须先定位并处理数据。

## 开发验证

本地测试只能使用临时测试密钥，不连接生产数据库，不执行生产迁移。重点验证启动校验、FPC1 新旧双读、错误 keyId 失败关闭、管理员权限、默认 dry-run、租户隔离、容量阻塞和退役门禁。
