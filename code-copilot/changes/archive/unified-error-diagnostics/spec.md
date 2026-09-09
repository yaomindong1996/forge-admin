# Forge Admin 统一错误管理与故障诊断中心

> status: propose
> created: 2026-08-25
> complexity: 🔴复杂
> change-id: `unified-error-diagnostics`
> implementation-mode: 分阶段实施，每阶段独立验收、发布与回滚

## 1. 背景与目标

Forge Admin 当前已经有全局异常处理、统一响应对象、前端请求失败弹窗以及分散的故障排查文档，
但“程序报错”和“用户如何解决”之间还没有形成稳定契约。典型问题包括：

- 后端大量直接抛出中文字符串，缺少可检索、可长期引用的稳定错误编号；
- 用户看到“系统异常”后，不知道原因、配置入口、期望值和下一步操作；
- 应用尚未启动时，HTTP 全局异常处理和前端弹窗都无法工作；
- 前端、日志和响应中的 TraceId 尚未形成统一协议；
- 文档中的错误码、响应字段和源码存在漂移，故障说明散落在多个栏目；
- 对原始异常开放过多会泄漏 SQL、表名、文件路径、Token、密码等敏感信息，开放过少又无法排障。

本变更建设一套平台级、可渐进迁移的统一错误体系，使用户在遇到可识别故障时能得到：

1. 稳定的符号错误编号 `errorKey`；
2. 面向当前角色的安全提示；
3. 可公开的原因摘要；
4. 明确的配置位置、配置键和期望配置；
5. 可执行的解决步骤和故障文档入口；
6. 可供管理员关联服务日志的 TraceId；
7. 应用启动失败时同样结构化的控制台诊断信息。

### 1.1 可验证目标

- 新增错误可以通过编译期类型安全的错误定义使用，不再复制错误编号和帮助文案。
- 现有 `RespInfo.code/message/data/timestamp` 保持兼容，新增字段均为可选字段。
- 已识别错误的 HTTP 响应、前端弹窗、日志和文档使用同一个 `errorKey`。
- 启动失败不依赖 Spring ApplicationContext 完成初始化，也能输出错误编号、原因、检查位置和文档链接。
- 首批覆盖数据库、Flyway、Redis、端口、Flow 地址、文件存储、AI、企业协同和开放平台配置故障。
- 普通业务校验可以继续使用旧异常方式，允许按模块迁移，不要求一次改完全部调用点。
- CI 能阻止错误编号重复、缺少文档、虚构错误码、错误配置键和敏感信息进入公开文档。

### 1.2 成功指标

首批上线后，以以下指标判断方案是否有效：

| 指标 | 目标 |
|---|---|
| 首批公开错误的错误定义与文档对应率 | 100% |
| `errorKey` 全局重复数 | 0 |
| 已识别错误响应中的 TraceId 可关联日志比例 | 100% |
| 未知 500 响应泄漏堆栈、SQL、密码、Token 的用例数 | 0 |
| 首批启动故障输出“编号/原因/位置/建议/文档”完整率 | 100% |
| 旧前端消费新增响应的回归失败数 | 0 |

## 2. 代码现状（Research Findings）

> 本节结论基于 2026-08-25 工作区静态扫描；数量用于说明迁移规模，不作为长期常量。

### 2.1 后端异常处理链路

1. `forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/exception/GlobalExceptionHandler.java`
   中的 `GlobalExceptionHandler` 统一处理业务、参数、数据库、运行时和未知异常。
2. 同目录 `BusinessException.java` 中的 `BusinessException` 只有 `code/message/data`，调用方主要直接传中文消息。
3. `forge-starter-core/.../domain/RespInfo.java` 中的 `RespInfo` 只有 `code/message/data/timestamp`。
4. `forge-starter-auth/.../SaTokenExceptionHandler.java` 另有鉴权异常转换链路，后续必须与统一协议对齐，不能只修改 Core。
5. 工作区静态扫描约有 2455 处 `new BusinessException(`、190 处 `RespInfo.error(`。一次性迁移风险过高。
6. `GlobalExceptionHandler` 已对 SQL/数据库细节进行检测并统一返回安全文案，安全方向正确；但用户只能看到
   “数据访问异常，请联系管理员”，缺少可行动信息。
7. 源码中未发现通用的 `FailureAnalyzer`、`ApplicationFailedEvent` 诊断器或统一错误目录。

### 2.2 前端错误处理链路

1. `forge-admin-ui/src/utils/http/interceptors.js` 为请求生成 `traceId`，并汇总请求、响应和网络异常。
2. `forge-admin-ui/src/utils/http/helpers.js` 按 401、403、404、500 等 `code` 选择提示行为。
3. `forge-admin-ui/src/utils/http/error-dialog.js` 已展示错误摘要、数字错误码、请求地址、TraceId 和诊断详情。
4. 当前前端还不识别 `errorKey`、安全原因、配置位置、解决动作或文档链接。
5. 当前诊断详情可能包含 `responseData` 和前端异常对象。新协议实施时必须限定生产环境展示内容，不能把后端脱敏边界
   重新从前端绕开。
6. 前端 TraceId 目前主要取请求头；后端响应体、响应头、日志 MDC 之间尚无统一优先级与生命周期。

### 2.3 文档现状

1. `forge-website/forge-docs/support/error-index.md` 是人工维护的单页错误索引。
2. `forge-website/forge-docs/reference/api/error-codes.md` 记录的 `1001`～`1303` 未在当前源码形成真实、稳定的错误契约；
   示例字段使用 `msg`，实际响应字段为 `message`。
3. `forge-website/forge-docs/development/backend/error-handling.md` 仍建议维护纯数字常量，与当前需要的稳定符号错误编号不一致。
4. `start/troubleshooting.md`、`workflow/troubleshooting.md`、`lowcode/troubleshooting.md` 等页面各自维护故障答案，
   容易出现同一问题多个版本。
5. 免费版、付费版可以共享通用错误编号和公共排查页；付费能力的错误页需要版本/版本权益标识，不能把付费配置步骤混入
   免费版快速入门。

### 2.4 已发生故障样本

Flyway 执行 `V1.0.114__seed_presale_business_process.sql` 时出现：

```text
Illegal mix of collations (utf8mb4_0900_ai_ci, IMPLICIT)
and (utf8mb4_unicode_ci, IMPLICIT) for operation '='
```

根因是新环境不同入口使用了不同排序规则。`mysql-collation-consistency` 变更已经把活动初始化入口统一为：

```text
utf8mb4 + utf8mb4_0900_ai_ci
```

该故障将作为本体系第一条端到端样板错误：`START-DB-COLLATION-MIXED`。

### 2.5 关键约束与风险

- 当前 `RespInfo.code` 同时承载 HTTP 风格状态码和历史业务码。本变更不直接重构为另一套 HTTP 状态体系。
- 未识别的数据库/系统异常必须继续隐藏原始原因；不能为了“帮助用户”直接返回 `Throwable.getMessage()`。
- 启动诊断可能发生在 Bean 创建、数据源、Flyway 或 Web Server 初始化之前，不能依赖数据库、Redis、Controller 或完整容器。
- Forge Project 与 Forge Website 是两个 Git 仓库，跨仓库一致性需要可复制的清单产物和独立校验命令。
- `FlowClientProperties` 的类内默认地址与 Admin 实际覆盖地址存在端口差异，需列入诊断和后续配置一致性治理，
  但本 Spec 不顺带修改该业务默认值。
- 当前服务端口源码基线为 Admin `8580`、Report `8581`、Flow `8081`、App `8583`；错误文档不得给出相互冲突的改端口示例。

## 3. 范围与非目标

### 3.1 本次范围

- 后端错误定义领域模型、目录注册、异常到响应的解析规则；
- `RespInfo` 兼容扩展和 `BusinessException` 渐进式扩展；
- HTTP 请求 TraceId 的生成、传播、日志关联和响应回传；
- Spring Boot 启动失败分析器；
- Admin 前端统一错误模型和可行动弹窗；
- Forge Docs 故障诊断中心、错误页模板、旧入口收敛和版本标识；
- 错误清单导出、文档交叉校验和治理门禁；
- 首批基础设施与管理员配置类故障迁移；
- 后续业务模块迁移规则。

### 3.2 非目标

- 不在一个阶段内重写全部 2455 处 `BusinessException`；
- 不把“记录不存在”“字段不能为空”“当前状态不能删除”等普通业务提示全部改成配置故障；
- 不在本变更中修改现有 API 的成功响应格式；
- 不在本变更中强制切换所有接口的真实 HTTP Transport Status；
- 不建设数据库可配置的错误文案后台，不允许运营人员动态修改错误语义；
- 不向前端或公开文档输出完整堆栈、SQL、数据库对象、绝对路径、密码、密钥、Token 或个人敏感数据；
- 不自动修改用户配置、数据库排序规则或端口；系统只诊断并给出步骤；
- 不替代日志平台、APM 或告警平台，只提供稳定关联字段；
- 不自动启动真实 MySQL、Redis、Forge 服务或执行真实数据库迁移。真实环境验收由用户执行。

## 4. 用户角色、错误分类与展示原则

### 4.1 用户角色

| 角色 | 需要看到的内容 | 不应看到的内容 |
|---|---|---|
| 最终用户 | 简明原因、可自行修正的动作、TraceId | 服务地址、内部配置、堆栈、SQL |
| 系统管理员 | 安全原因、配置入口/键、期望值、排查步骤、TraceId | 密码、密钥、Token 的真实值 |
| 部署运维 | 启动诊断、依赖位置、服务名/端口、文档、TraceId | 未脱敏凭据、完整 SQL 参数 |
| 开发者 | `errorKey`、日志上下文、异常链、源码定义位置 | 生产环境敏感值 |
| 文档维护者 | 导出的公开清单、配置来源、版本归属 | 内部异常详情和真实用户配置 |

### 4.2 错误分类

| 分类 | 示例 | 默认展示策略 | 是否要求配置位置 |
|---|---|---|---|
| `VALIDATION` 用户输入 | 必填字段为空、格式错误 | 直接告诉用户修正字段 | 否 |
| `BUSINESS_RULE` 业务状态 | 已发布不能删除、余额不足 | 告诉用户当前限制和可选动作 | 否 |
| `ADMIN_CONFIGURATION` 管理员配置 | 存储未配置、AI Key 缺失 | 管理员可见配置入口、键和期望 | 是 |
| `STARTUP_DEPLOYMENT` 启动部署 | 端口占用、Flyway 失败 | 控制台输出完整安全诊断 | 是 |
| `DEPENDENCY` 外部依赖 | Redis、Flow、企微不可达 | 区分临时不可用和配置错误 | 通常是 |
| `AUTHORIZATION` 鉴权权限 | Token 失效、无权限 | 保持登录跳转和权限提示语义 | 否 |
| `SYSTEM_INTERNAL` 未知系统异常 | NPE、未知 RuntimeException | 安全文案 + TraceId | 否 |

### 4.3 展示原则

1. 错误分类不等于 HTTP 状态码；`errorKey` 不使用纯数字代替。
2. 只有调用方能够采取行动的信息才进入响应；仅开发者可用的信息进入日志。
3. “配置位置”可以是管理后台菜单、配置文件路径、环境变量或数据库级对象，但不能包含当前秘密值。
4. 原始异常只用于识别错误和写日志，绝不能直接作为 `safeCause`。
5. 同一根因只发布一个规范错误页，快速入门和模块页作为场景入口链接过去。
6. 无法安全确认具体根因时，宁可回退为未知错误，也不输出猜测性建议。

## 5. 总体架构

```text
模块错误定义（Java，单一语义来源）
        │
        ├── HTTP 异常解析 ──> RespInfo 扩展字段 ──> Admin 统一错误弹窗
        │                          │
        │                          └── errorKey + traceId + 安全帮助
        │
        ├── 启动 FailureAnalyzer ──> 控制台安全诊断
        │
        ├── 结构化日志 ──> errorKey + traceId + 原始异常（仅服务端）
        │
        └── 构建期 Manifest ──> Forge Docs 错误页校验 ──> CI 门禁
```

架构分为五层：

1. **定义层**：模块内编译期类型安全的错误定义，描述稳定语义和公开帮助元数据。
2. **识别层**：业务显式抛出定义，或通过异常链解析器识别数据库、Redis、端口等已知根因。
3. **呈现层**：HTTP、启动控制台、前端分别按媒介呈现，但共享同一个错误定义。
4. **关联层**：TraceId 贯穿请求、响应和日志；启动错误使用独立的 `startupIncidentId`。
5. **治理层**：导出错误清单，校验编号、配置键、文档和安全规则。

### 5.1 模块边界

| 模块 | 责任 |
|---|---|
| `forge-starter-core` | 错误契约、错误定义接口、响应模型、BusinessException 兼容扩展、HTTP 解析基础 |
| `forge-starter-log` | 请求 TraceId 过滤器、MDC 生命周期、响应头回传、异步上下文传播规范 |
| 新增 `forge-starter-diagnostics` | 启动失败识别、基础设施异常解析、控制台输出、启动错误目录 |
| 各 starter/plugin/business 模块 | 维护本模块错误定义和异常映射，不复制 Core 的定义 |
| `forge-admin-ui` | 标准化响应错误、按受众展示帮助、复制诊断信息、打开安全文档链接 |
| `forge-docs` | 故障中心、规范错误页、版本/权益提示、场景入口 |
| 构建脚本/CI | Manifest 导出、跨仓库一致性、安全与链接校验 |

### 5.2 依赖方向

- 各业务模块可以依赖 Core 的错误契约；Core 不反向依赖业务模块。
- Diagnostics 依赖 Core，不依赖 Admin/Flow/Report 应用模块。
- 模块通过 `ForgeErrorCatalogProvider` 向注册中心提供定义；运行时发现不得依赖数据库。
- 启动分析器可直接引用 Diagnostics 自己的静态定义，不依赖 Spring Bean 已创建。
- Docs 消费构建期导出的公开 Manifest，不在生产运行时调用后端接口拉取错误定义。

## 6. 错误定义领域模型

### 6.1 核心接口

建议在 Core 中提供不可变接口，模块枚举实现该接口：

```java
public interface ForgeErrorDefinition {
    String errorKey();
    int legacyCode();
    HttpStatus httpStatus();
    String module();
    ErrorCategory category();
    ErrorSeverity severity();
    ErrorAudience audience();
    String title();
    String userMessage();
    String safeCause();
    List<ConfigReference> configRefs();
    List<ErrorAction> actions();
    String docsSlug();
    boolean retryable();
    ExposureLevel exposure();
    Set<String> allowedArguments();
    boolean deprecated();
    String replacedBy();
}
```

字段约束：

| 字段 | 规则 |
|---|---|
| `errorKey` | 全局唯一、发布后不可改名或复用，格式见 6.2 |
| `legacyCode` | 写入现有 `RespInfo.code`，默认使用 400/401/403/404/409/500/503 等现有语义 |
| `httpStatus` | 描述推荐 Transport Status；初期不自动改变历史接口的真实 HTTP 状态 |
| `module` | 稳定模块标识，如 `core/auth/flow/lowcode/ai/file/collab/openapi` |
| `category` | 必须属于 4.2 的分类枚举 |
| `severity` | `INFO/WARN/ERROR/CRITICAL`，决定日志和界面等级，不由调用方随意覆盖 |
| `audience` | `END_USER/ADMIN/OPS/DEVELOPER`，用于裁剪帮助信息 |
| `title` | 短标题，不含变量和敏感值 |
| `userMessage` | 默认用户文案，可使用受控占位符 |
| `safeCause` | 人工编写的安全原因，不得来自原始异常 |
| `configRefs` | 零到多个配置引用；配置故障至少一个 |
| `actions` | 有顺序、可执行、可验证的操作，不允许包含秘密示例 |
| `docsSlug` | 站内相对路径，不在定义中硬编码域名 |
| `retryable` | 仅表示重试是否可能有效，不代表前端必须自动重试 |
| `exposure` | `PUBLIC/AUTHENTICATED/ADMIN_ONLY/INTERNAL` |
| `allowedArguments` | 允许插入安全文案的参数白名单 |
| `deprecated/replacedBy` | 旧错误兼容使用；弃用后仍保留编号，禁止复用 |

### 6.2 错误编号规范

格式：

```text
<DOMAIN>-<AREA>-<CAUSE>
```

正则：

```text
^[A-Z][A-Z0-9]*(-[A-Z0-9]+){2,7}$
```

首批前缀：

```text
START-DB-*       START-FLYWAY-*    START-REDIS-*    START-PORT-*
AUTH-*           TENANT-*          FLOW-*           LOWCODE-*
AI-*             FILE-*            COLLAB-*         OPENAPI-*
CORE-*           VALIDATION-*
```

编号规则：

- 编号表达稳定原因，不包含版本号、数据库表名、客户名或中文拼音。
- 同一原因跨 Admin/Flow/Report/App 复用同一编号，通过安全参数表示服务名。
- 不使用 `ERROR-001`、`SYS-500` 等缺少语义的编号。
- 已发布编号即使实现删除也进入 deprecated，不能分配给其他含义。
- 未识别错误统一使用 `CORE-SYSTEM-UNEXPECTED`，但日志仍记录真实异常。

### 6.3 配置引用

```java
public record ConfigReference(
    ConfigLocationType type,
    String location,
    String key,
    String expectedValue,
    boolean secret,
    String sourceRef
) {}
```

`type` 支持 `ADMIN_MENU`、`YAML`、`ENVIRONMENT`、`DATABASE`、`COMMAND`。

- `location`：例如“系统管理 → 文件存储”或 `application-dev.yml`。
- `key`：必须是源码真实配置键；支持多键时使用多个 `ConfigReference`，不拼接模糊文本。
- `expectedValue`：只描述格式、范围或公开常量，例如 `http(s)://host:port`、`utf8mb4_0900_ai_ci`。
- `secret=true` 时，响应和文档只允许说明“不能为空/格式要求”，不得显示默认值、当前值或示例真凭据。
- `sourceRef`：仅构建校验使用，指向配置属性类、YAML 或后台菜单源码，不返回前端。

### 6.4 解决动作

```java
public record ErrorAction(
    int order,
    String instruction,
    String verification,
    ErrorAudience audience
) {}
```

动作必须满足：

- 使用命令时说明适用平台和影响范围；
- 涉及数据库结构修改时必须先提示备份，且不能由系统自动执行；
- 最后一个动作应给出验证方法；
- “联系管理员”只能作为兜底动作，不能替代可发现的配置步骤；
- 不在响应中携带任意 HTML、脚本或可执行命令片段。

### 6.5 受控参数

错误文案可以使用 `{serviceName}`、`{port}`、`{maxSize}` 等白名单参数。运行时遵守：

1. 只有定义在 `allowedArguments` 中的参数可进入用户文案；
2. 值必须通过类型、长度和字符集校验；
3. URL、文件路径、SQL、请求体和异常消息默认禁止作为参数；
4. 非法参数忽略并记录内部告警，不能回退为原样拼接；
5. 秘密值永远不属于允许参数。

### 6.6 目录注册与导出

- 每个模块实现 `ForgeErrorCatalogProvider`，返回本模块定义集合。
- Provider 通过类路径资源注册，注册机制必须能在无 Spring ApplicationContext 时加载。
- Core 运行时注册中心启动时校验本应用类路径内的 `errorKey` 唯一性；重复时构建测试失败，生产运行时记录严重错误并拒绝注册冲突定义。
- 构建工具从相同 Provider 导出 `forge-errors-manifest.json`；只导出允许公开的安全字段，不导出 `sourceRef`、内部动作或异常匹配规则。
- Java 定义是错误语义的单一来源；Markdown 是完整解决步骤的单一来源。二者通过 `errorKey/docsSlug` 双向校验。

## 7. HTTP 错误响应协议

### 7.1 兼容扩展

保留字段：

```text
code, message, data, timestamp
```

新增可选字段：

```text
errorKey, traceId, help
```

`help` 结构：

```text
cause, configLocations[], actions[], docsUrl, retryable
```

完整示例：

```json
{
  "code": 500,
  "errorKey": "START-DB-COLLATION-MIXED",
  "message": "数据库排序规则不一致，初始化失败",
  "help": {
    "cause": "目标数据库、表或字符串列混用了不同的排序规则。",
    "configLocations": [
      {
        "type": "DATABASE",
        "location": "MySQL 数据库 forge_admin",
        "key": "DEFAULT CHARACTER SET / DEFAULT COLLATE",
        "expectedValue": "utf8mb4 / utf8mb4_0900_ai_ci"
      }
    ],
    "actions": [
      "确认数据库、活动初始化 SQL 与新建临时表使用同一排序规则。",
      "历史库先备份，再由 DBA 检查表和列级排序规则。"
    ],
    "docsUrl": "/support/errors/database/start-db-collation-mixed",
    "retryable": false
  },
  "traceId": "01J63Q5Q8KJ9M7A2E4T7W1X3YC",
  "timestamp": 1787623200000
}
```

### 7.2 兼容规则

1. 成功响应不增加错误字段。
2. 未迁移的 `BusinessException` 保持原 `code/message/data/timestamp` 行为，可额外带 `traceId`。
3. 已迁移错误继续写入兼容 `code`，旧前端会忽略新增 JSON 字段。
4. `message` 仍是首要用户提示；前端不能要求 `help` 必定存在。
5. `data` 不承载错误帮助，避免业务数据和诊断元数据混用。
6. 本变更首轮不强制修改真实 HTTP Transport Status。后续如要规范化，必须另开 Spec 并评估 Axios、网关和第三方调用方。
7. 鉴权异常继续保留自动退出/跳转行为，只补齐可兼容的 `errorKey/traceId`。

### 7.3 暴露策略

| `exposure` | 响应规则 |
|---|---|
| `PUBLIC` | 返回全部已审核安全字段 |
| `AUTHENTICATED` | 登录用户返回帮助，匿名用户只返回安全 message/errorKey/traceId |
| `ADMIN_ONLY` | 有管理权限返回配置帮助，其他用户只返回安全 message/errorKey/traceId |
| `INTERNAL` | 只返回通用 message、通用或内部映射后的 errorKey、traceId |

权限判断失败时默认收紧展示，不得默认放开。

### 7.4 未知错误

未知系统错误固定响应：

```json
{
  "code": 500,
  "errorKey": "CORE-SYSTEM-UNEXPECTED",
  "message": "系统异常，请联系管理员",
  "traceId": "...",
  "timestamp": 1787623200000
}
```

响应中不得包含：

- `Throwable.getMessage()` 原文；
- 堆栈、类名和包名；
- SQL、表名、列名、数据库连接串；
- 服务器绝对路径、Flyway 实际 SQL 语句；
- 请求体、Authorization、Cookie、Token、密码、密钥；
- 内部 IP 或不适合公开的依赖拓扑。

## 8. 后端异常处理链路

### 8.1 显式业务错误

保留 `BusinessException` 现有构造器，新增接收 `ForgeErrorDefinition` 和安全参数的构造器：

```java
throw new BusinessException(
    FileErrors.STORAGE_CONFIG_INCOMPLETE,
    Map.of("storageType", "S3")
);
```

调用链：

```text
业务代码选择类型安全定义
  -> BusinessException 保存定义和安全参数
  -> GlobalExceptionHandler 交给 ErrorResponseFactory
  -> ExposurePolicy 按用户角色裁剪
  -> RespInfo 返回兼容字段和可选帮助
  -> 日志写 errorKey + traceId + 原始 Throwable
```

现有 `new BusinessException(String)` 不自动根据中文消息猜测错误编号。

### 8.2 异常链识别

对无法在业务代码显式选择定义的基础设施异常，提供有顺序的 `ErrorCauseResolver`：

```java
Optional<ResolvedForgeError> resolve(Throwable throwable, DiagnosticContext context);
```

规则：

- 遍历 cause chain，但设置最大深度并处理循环引用；
- 优先使用异常类型、SQLState、MySQL error code 等稳定信号；
- 文本关键词只作为补充，并集中在 Resolver 中测试，禁止散落在 Handler；
- 具体根因优先于包装异常，例如 Collation 错误优先于通用 Flyway 迁移失败；
- 多个 Resolver 命中时按显式优先级选择，并在 debug 日志记录候选，不向用户展示猜测过程；
- 不能确定时回退为安全的通用错误。

建议优先级：

```text
显式 ForgeErrorDefinition
  > 凭据/权限
  > 排序规则/数据库不存在/端口占用等具体原因
  > 连接不可达
  > Flyway/依赖通用失败
  > CORE-SYSTEM-UNEXPECTED
```

### 8.3 日志级别

| 场景 | 级别 | 必需结构化字段 |
|---|---|---|
| 用户输入/预期业务限制 | WARN 或 INFO | `errorKey, traceId, module, uri` |
| 管理员配置/依赖故障 | ERROR | `errorKey, traceId, module, dependency` |
| 未知系统错误 | ERROR | `errorKey, traceId, exceptionClass` |
| 启动失败 | ERROR | `errorKey, startupIncidentId, service` |

日志 URI 默认只记录路径，不记录可能含凭据的 Query String。原始异常仅写受保护服务端日志。

## 9. TraceId 与日志关联

### 9.1 HTTP 协议

标准名称：

```text
请求头/响应头：X-Trace-Id
MDC：traceId
响应体：traceId
```

兼容期读取前端旧请求头 `traceId`，但响应统一返回 `X-Trace-Id`。前端完成迁移后停止主动使用非标准头。

### 9.2 生命周期

1. 请求进入最前置 Servlet Filter 时读取 `X-Trace-Id` 或旧 `traceId`。
2. 仅接受 `^[A-Za-z0-9_-]{8,64}$`；非法、过长或缺失时由服务端生成新的不可预测 ID。
3. 将最终 ID 放入 MDC，并写入响应头。
4. `RespInfo` 错误响应从统一 `TraceContext` 读取相同 ID。
5. `finally` 中清理 MDC，防止线程复用污染。
6. 线程池、异步事件和远程调用需要显式 TaskDecorator/请求拦截器传播；无法传播时生成子请求 ID 并记录父 ID。
7. 前端展示优先级：响应体 `traceId` > 响应头 `X-Trace-Id` > 本次请求头。

### 9.3 信任边界

- 接受客户端 TraceId 仅用于相关性，不作为认证、幂等或审计主键。
- 日志平台应同时记录服务名、环境、时间和 TraceId，避免仅凭客户端值检索产生误判。
- 不把租户 ID、用户 ID、手机号等编码进 TraceId。

### 9.4 启动事件编号

应用启动失败没有 HTTP TraceId。Diagnostics 在进程内生成 `startupIncidentId`，格式和长度与 TraceId 相同，
同一次启动失败的摘要和详细日志使用同一个值。

## 10. Spring Boot 启动失败诊断

### 10.1 实现方式

新增 Diagnostics Starter，使用 Spring Boot `FailureAnalyzer`/`AbstractFailureAnalyzer` 或等价的
早期 `SpringBootExceptionReporter` 扩展。优先使用标准 `FailureAnalysis` 输出，不替换 Spring Boot 原始失败流程。

必须满足：

- 不依赖 Controller、数据库、Redis、业务 Bean 或完整 ApplicationContext；
- 异常识别器为纯 Java，可用构造的异常链做单元测试；
- 对缺失的可选依赖使用类名/反射或隔离模块，避免 `NoClassDefFoundError`；
- 分析器自身异常必须被吞并并记录，不能覆盖原始启动异常；
- 可通过 JVM/System Property `forge.diagnostics.startup.enabled=false` 早期关闭，便于紧急回滚。

### 10.2 输出格式

```text
APPLICATION FAILED TO START

错误编号：START-DB-COLLATION-MIXED
事件编号：01J63Q5Q8KJ9M7A2E4T7W1X3YC
错误原因：数据库对象混用了不同排序规则，Flyway 无法完成字符串比较。
检查位置：MySQL 数据库 forge_admin 的默认、表级和列级排序规则
期望配置：utf8mb4 / utf8mb4_0900_ai_ci
建议操作：历史库先备份，再按文档检查并统一相关对象；不要修改已执行 Flyway 文件。
解决文档：https://<docs-host>/support/errors/database/start-db-collation-mixed
```

控制台诊断是面向管理员/运维的安全摘要。完整异常链仍由 Spring Boot 写日志，但文档不得要求用户把完整生产日志公开粘贴。

### 10.3 首批启动识别规则

| errorKey | 稳定识别信号 | 说明 |
|---|---|---|
| `START-DB-CONNECTION-FAILED` | JDBC 连接异常/稳定 SQLState | 主机、端口、网络或服务状态 |
| `START-DB-AUTH-FAILED` | 数据库认证 SQLState/vendor code | 用户名、密码或授权失败 |
| `START-DB-NOT-FOUND` | unknown database 等稳定 code | 目标数据库未创建或名称错误 |
| `START-DB-COLLATION-MIXED` | MySQL collation vendor code + 受测关键词 | 本 Spec 样板 |
| `START-FLYWAY-MIGRATION-FAILED` | Flyway 迁移异常且无更具体命中 | 通用迁移失败，不返回 SQL |
| `START-REDIS-CONNECTION-FAILED` | Redis/Redisson 连接异常 | 服务、主机、端口或网络 |
| `START-REDIS-AUTH-FAILED` | Redis 认证异常 | 密码/ACL，不返回凭据 |
| `START-PORT-IN-USE` | Web Server 包装的 `java.net.BindException` | 使用安全参数显示服务名和端口 |

### 10.4 Collation 专项规则

- 首选 MySQL vendor error code/SQLState；只有驱动未提供稳定代码时才使用规范化关键词 `Illegal mix of collations`。
- 匹配时可识别排序规则名称用于内部日志，但响应与控制台只展示审核过的目标值。
- 文档明确：新环境统一 `utf8mb4_0900_ai_ci`；MySQL 5.7 不支持该排序规则。
- 不建议直接修改已发布 `V*.sql`，避免 Flyway checksum mismatch。
- 历史有数据环境必须先备份并由 DBA 评估表/列转换；诊断器不自动执行 `ALTER`。

## 11. Admin 前端错误体验

### 11.1 前端标准错误模型

响应拦截器先规范化为内部模型：

```ts
interface NormalizedRequestError {
  code?: number | string
  errorKey?: string
  message: string
  traceId?: string
  cause?: string
  configLocations: ConfigLocation[]
  actions: string[]
  docsUrl?: string
  retryable?: boolean
  method?: string
  url?: string
  transportStatus?: number
}
```

旧响应、Blob JSON、网络错误和 Axios 错误都进入同一标准化函数；认证静默逻辑继续优先执行。

### 11.2 弹窗结构

已识别错误按以下顺序展示：

1. 用户提示 `message`；
2. 标签：`errorKey`、兼容 `code`、TraceId；
3. “为什么发生”：只展示 `help.cause`；
4. “检查这里”：展示管理员可见配置位置和期望值；
5. “如何处理”：有序动作；
6. “查看解决文档”按钮；
7. “复制诊断信息”按钮，只复制安全字段、请求方法和去 Query 的路径。

普通校验错误仍使用轻量消息提示；网络错误显示网络排查建议；未知 500 显示安全文案和 TraceId，不伪造配置建议。

### 11.3 前端安全约束

- 生产构建不在弹窗中序列化完整 `responseData`、Error stack 或任意异常对象。
- 后端未返回 `help` 时，前端不根据消息关键词自行拼配置位置。
- `docsUrl` 只允许站内相对路径或配置的 Forge Docs 域名，拒绝 `javascript:`、`data:` 和未知外域。
- URL 展示和复制时移除 Query/Hash 中的 Token、OAuth code、state 等敏感参数。
- 配置位置由后端权限策略裁剪，前端只能进一步隐藏，不能自行放宽。
- 保持单实例/去抖，避免并发请求失败产生弹窗风暴。
- 401/Token 失效继续执行现有退出和登录跳转，不显示普通 500 弹窗。

### 11.4 可用性

- 错误编号和 TraceId 支持一键复制；
- 键盘可以访问展开、复制和文档按钮；
- 文档按钮说明会打开新页面；
- 移动端宽度下配置键和长错误编号可以换行；
- 颜色不是区分严重等级的唯一手段。

## 12. Forge Docs 故障诊断中心

### 12.1 目录结构

```text
forge-docs/support/errors/
├── index.md
├── startup/
├── database/
├── redis/
├── auth/
├── flow/
├── lowcode/
├── ai/
├── storage/
└── integration/
```

说明：目录按用户寻找问题的方式分类，`errorKey` 的技术前缀不要求与目录一一对应。

### 12.2 错误页 Frontmatter

每篇规范错误页必须包含：

```yaml
---
errorKey: START-DB-COLLATION-MIXED
title: 数据库排序规则不一致
module: startup
category: STARTUP_DEPLOYMENT
editions: [free, pro]
since: 1.1.2
keywords:
  - Illegal mix of collations
  - utf8mb4_0900_ai_ci
---
```

`editions` 用于区分 `free/pro`，不是复制两份错误文档。仅付费能力的页面标记 `[pro]`，并在正文顶部说明适用版本。

### 12.3 固定正文模板

每篇错误文档按以下顺序编写：

1. 错误编号；
2. 适用版本/版本权益；
3. 典型报错关键词；
4. 用户会看到什么；
5. 发生原因；
6. 配置位置；
7. 正确配置；
8. 解决步骤；
9. 验证方法；
10. 仍未解决时需要提供的安全信息；
11. 相关文档。

### 12.4 内容边界

- 典型关键词可以包含异常短语，但不得粘贴真实连接串、用户名、路径或业务数据。
- 配置键必须来自源码配置属性、活动 YAML、管理菜单或初始化脚本。
- 示例域名使用保留域名，凭据使用明显占位符。
- 数据库变更先写备份和适用版本；高风险命令不得提供“一键复制直接执行”的误导性形式。
- 免费/付费只影响适用范围，不影响公共基础设施错误的真实性。
- 文档页面不得发明源码不存在的数字错误码。

### 12.5 现有页面收敛

| 当前页面 | 调整方式 |
|---|---|
| `support/error-index.md` | 改为错误中心入口/兼容重定向，不再手工维护另一份答案 |
| `reference/api/error-codes.md` | 改为响应协议和真实清单说明，删除虚构 `1001`～`1303` |
| `development/backend/error-handling.md` | 改为错误定义使用规范和迁移示例 |
| `start/troubleshooting.md` | 保留启动场景导航，具体答案链接错误页 |
| `workflow/troubleshooting.md` | 保留流程场景导航，具体答案链接错误页 |
| `lowcode/troubleshooting.md` | 保留低代码场景导航，具体答案链接错误页 |
| `start/configure-admin.md`、`start/quick-start.md` | 只保留一致的初始化主路径，失败处链接相应错误页 |

### 12.6 搜索体验

错误中心索引至少支持用户按以下信息查找：

- 完整 `errorKey`；
- 报错关键词，例如 `Illegal mix of collations`；
- 模块/依赖，例如 MySQL、Redis、Flow、AI；
- 场景，例如启动失败、登录失败、文件上传失败；
- 免费版/付费版适用范围。

首阶段使用 VitePress 现有搜索和生成索引，不新增需要后端服务的动态搜索。

## 13. 清单导出与自动校验

### 13.1 公开 Manifest

构建期输出示意：

```json
{
  "schemaVersion": "1.0",
  "generatedAt": "2026-08-25T00:00:00Z",
  "errors": [
    {
      "errorKey": "START-DB-COLLATION-MIXED",
      "module": "startup",
      "category": "STARTUP_DEPLOYMENT",
      "legacyCode": 500,
      "docsSlug": "/support/errors/database/start-db-collation-mixed",
      "exposure": "ADMIN_ONLY",
      "deprecated": false
    }
  ]
}
```

为保证可重复构建，正式 CI 比对时忽略 `generatedAt` 或支持不输出时间戳，清单项按 `errorKey` 排序。

### 13.2 后端校验

- `errorKey` 全局唯一且符合格式；
- `legacyCode/httpStatus/category/audience/exposure` 合法；
- 配置错误至少一个 ConfigReference 和一个验证动作；
- `secret=true` 的配置引用没有真实/默认秘密值；
- `docsSlug` 唯一、为站内规范路径；
- 占位符全部在 `allowedArguments` 声明；
- deprecated 错误存在合法 `replacedBy` 或明确终止原因；
- 未知 500 定义不能公开 `safeCause` 中的内部实现细节。

### 13.3 文档校验

- 每个公开、非 deprecated 错误定义都有唯一 Markdown 页面；
- 每篇错误页的 `errorKey` 在 Manifest 中真实存在；
- Frontmatter 的 module/category/docsSlug/editions 与清单及版本规则一致；
- 配置键能在导出的配置元数据/人工批准白名单中找到；
- 内部错误不能生成公开页；
- 页面不存在死链，侧边栏包含错误中心入口；
- 扫描密码、Token、AccessKey、私网地址、真实 JDBC URL 等敏感模式；
- `reference/api/error-codes.md` 不再出现未注册的业务错误码表。

### 13.4 跨仓库执行

1. Forge Project 构建生成确定性的 `forge-errors-manifest.json`。
2. 发布/同步该文件到 Forge Website 的校验输入目录，或在同一 Workspace CI 中以路径参数传入。
3. Forge Docs 构建前运行校验脚本。
4. 清单和文档任一侧不一致则 CI 失败。
5. 紧急后端回滚时允许错误定义保留为 deprecated，避免已发布文档立即失效。

不采用生产时跨仓库网络调用，避免文档构建依赖运行中的 Admin 服务。

## 14. 首批错误迁移范围

### 14.1 P0：启动与基础设施

| 领域 | 首批错误 | 配置/文档重点 |
|---|---|---|
| MySQL | 连接失败、认证失败、数据库不存在、Collation 混用 | 数据源、建库规则、MySQL 8.0+ |
| Flyway | 通用迁移失败、校验失败 | migration 位置、checksum 边界、禁止改已执行脚本 |
| Redis/Redisson | 连接失败、认证失败 | host/port/password/ACL，秘密不回显 |
| 服务端口 | Admin/Report/Flow/App 端口占用 | 8580/8581/8081/8583 基线及调用方同步 |
| Flow | 服务地址不可达、Token 获取失败 | `forge.flow.client.*`、`forge.flow.job.remote.*` |

### 14.2 P1：管理员配置

| 领域 | 首批错误 | 配置入口 |
|---|---|---|
| 文件存储 | 存储配置不完整、Endpoint 不可达、凭据无效 | 文件存储后台/YAML |
| AI Provider | API Key 缺失、Base URL 非法、模型未配置、Provider 不可达 | AI 中心 |
| 企业协同 | 企微等连接参数不完整、认证失败 | 企业协同配置 |
| OpenAPI/MCP/OAuth2 | Client、回调地址、Token/能力配置错误 | 开放平台配置 |

### 14.3 暂不迁移

- 记录不存在；
- 重复名称/编码；
- 普通字段校验；
- 当前状态不可编辑/删除；
- 面向最终用户且现有文案已经明确可行动的业务规则。

这些异常仍可保留旧构造器；后续只有在需要跨日志/文档检索或多个模块重复定义时再迁移。

## 15. 分阶段实施计划

> 每阶段完成后先验收再进入下一阶段。阶段编号是实施顺序，不要求一次发布全部阶段。

### 阶段 0：基线扫描与协议冻结

**输入**

- 本 Spec；
- 现有后端异常、前端弹窗、文档和配置元数据；
- `mysql-collation-consistency` 已完成变更。

**实施内容**

- 固化错误分类、字段命名、`errorKey` 格式、TraceId 头名和安全矩阵；
- 生成旧异常调用点基线报告，按模块/消息/异常类型聚类；
- 建立首批 errorKey 登记表和所有者；
- 明确跨仓库 Manifest Schema `1.0`；
- 为每个阶段创建独立 `tasks.md`/`test-spec.md` 或在总任务中按阶段硬隔离。

**产出**

- 已确认的协议 Schema；
- 首批错误登记表；
- 迁移基线和排除清单；
- 测试用异常样本库，至少包含本次 Collation 堆栈。

**验收**

- Schema 无未决字段；
- 首批 errorKey 无重复且命名通过规则；
- 明确哪些字段能返回用户、管理员、日志和文档；
- 旧异常总量和首批范围有可重复扫描命令。

**回滚**

- 本阶段不改变运行时；删除未采用的生成物即可。

### 阶段 1：Core 错误模型与兼容响应

**依赖**：阶段 0 完成。

**实施内容**

- 在 `forge-starter-core` 增加错误定义、帮助模型、受控参数、暴露策略和目录接口；
- 兼容扩展 `RespInfo`、`BusinessException` 和 `GlobalExceptionHandler`；
- 建立 `ErrorResponseFactory`，统一已识别、旧业务、数据库和未知错误响应；
- 为 `SaTokenExceptionHandler` 预留或接入相同响应工厂；
- 实现确定性 Manifest 导出和后端契约测试；
- 首阶段只接入少量样板定义，不批量改业务异常。

**产出**

- Core 错误契约和注册机制；
- 可选增强响应；
- `CORE-SYSTEM-UNEXPECTED` 等基础定义；
- Manifest Schema 和导出工具；
- 单元/契约测试。

**验收**

- 现有 `RespInfo` JSON 反序列化/前端消费不受影响；
- 旧 `BusinessException` 构造器行为和二进制/源码兼容性通过测试；
- 已识别错误正确返回 `errorKey/help`；
- 未知、SQL 和包装数据库异常均不泄漏原始消息；
- Manifest 两次生成内容一致且顺序稳定；
- 不改变成功响应和真实 HTTP 状态行为。

**发布开关与回滚**

- 提供 `forge.error.enriched-response-enabled`，默认按目标环境开启；关闭后只返回旧字段和 TraceId。
- 回滚运行时接入时保留新增可选字段类，避免跨模块二进制不一致。

### 阶段 2：TraceId 与前端统一展示

**依赖**：阶段 1 完成。

**实施内容**

- 在日志 Starter 增加最前置 TraceId Filter 和 MDC 清理；
- 响应头、错误响应体和日志统一 TraceId；
- 前端改用 `X-Trace-Id` 并兼容旧 `traceId`；
- 实现 `NormalizedRequestError`、增强弹窗、复制安全诊断信息和文档 URL 白名单；
- 去除生产环境原始 `responseData/Error stack` 展示；
- 保持认证静默、Blob 下载、网络错误和弹窗去抖逻辑。

**产出**

- HTTP TraceId 闭环；
- 新旧响应兼容的前端标准化层；
- 可行动错误弹窗；
- 前后端单元测试和前端组件/拦截器测试。

**验收**

- 同一请求在请求头、响应头、响应体、MDC 日志中使用同一 TraceId；
- 非法 TraceId 被替换，MDC 在线程复用后无污染；
- 新、旧、网络、Blob、401、403、404、500 场景均符合既有语义；
- 生产弹窗不显示堆栈、完整响应对象、秘密或带敏感 Query 的 URL；
- 文档外链白名单和 `javascript:` 拒绝测试通过；
- 可访问性和移动端布局通过人工验证清单。

**回滚**

- 前端可以回退到只显示 `message/code/traceId`；后端新增字段仍兼容。
- Trace Filter 可通过 `forge.error.trace-enabled=false` 关闭，恢复现有日志行为。

### 阶段 3：Forge Docs 错误中心与校验门禁

**依赖**：阶段 1 Manifest 可用；阶段 2 非硬依赖。

**实施内容**

- 建立 `support/errors/` 目录、模板、索引和侧边栏；
- 完成 `START-DB-COLLATION-MIXED` 第一篇样板页；
- 修正 `reference/api/error-codes.md` 的虚构码和 `msg` 字段；
- 修正 `development/backend/error-handling.md`；
- 将现有 troubleshooting 页面改为场景导航；
- 建立 Manifest ↔ Markdown 双向校验、死链、配置键和敏感信息扫描；
- 为免费/付费页面增加 `editions` 规则。

**产出**

- 故障诊断中心；
- 错误页模板和样板；
- 跨仓库校验脚本；
- 文档构建门禁。

**验收**

- `errorKey`、异常关键词和模块名都能找到样板页；
- 所有页面 Frontmatter 与 Manifest 一致；
- 旧链接有有效重定向或导航页，不产生 404；
- 免费用户不会被引导执行仅付费版步骤；
- VitePress 构建、链接检查和敏感扫描通过；
- 文档中的数据库排序规则和服务端口与源码基线一致。

**发布与回滚**

- 先发布文档页，再启用返回相应 `docsUrl` 的后端错误定义，避免短暂 404。
- 回滚目录结构时保留旧 URL 重定向；不能删除已发布 errorKey 的可访问说明页。

### 阶段 4：启动失败诊断

**依赖**：阶段 1、阶段 3 完成。

**实施内容**

- 新增 `forge-starter-diagnostics`；
- 实现数据库、Flyway、Redis、端口 Resolver 和 FailureAnalyzer；
- 为 Admin/Flow/Report/App 引入 Starter；
- 加入启动事件编号和 Docs Base URL 配置；
- 为 10.3 的每个公开错误补齐规范文档；
- 使用构造异常链和最小 ApplicationContext 测试，不连接真实依赖。

**产出**

- 启动失败结构化诊断；
- 第一批 Startup Error Catalog；
- 对应错误文档；
- 分析器与自动配置测试。

**验收**

- Collation 样本被识别为具体错误，而不是通用 Flyway 错误；
- 数据库认证/不存在/不可达、Redis、端口等样本互不误判；
- 未知启动异常仍保留 Spring Boot 原失败报告；
- 分析器自身失败不遮蔽原异常；
- 输出不含密码、JDBC 完整 URL 和 SQL；
- 四个服务模块静态/聚合构建通过。

**回滚**

- 通过早期属性关闭启动分析；必要时从具体应用移除 Diagnostics 依赖。
- 分析器只增强输出，不修改迁移和启动控制流，因此回滚不涉及数据恢复。

### 阶段 5：运行时基础设施与管理员配置迁移

**依赖**：阶段 1～4 完成。

**实施内容**

- 迁移 14.1 和 14.2 中的高价值错误；
- 每迁移一个错误，同步定义、异常映射、前端验收和错误文档；
- 对 Flow 默认地址与实际端口差异建立专项配置一致性检查；
- 管理后台配置错误按角色返回菜单位置，最终用户不返回管理员细节；
- 对临时依赖故障标注 `retryable`，但不默认自动重试非幂等请求。

**产出**

- 首批基础设施/配置 Error Catalog；
- 对应的 Resolver 或显式 BusinessException；
- 配置入口文档和契约测试；
- 首批迁移覆盖报告。

**验收**

- 每个首批错误有唯一 errorKey、真实配置位置、文档和安全测试；
- 权限不同的用户获得正确裁剪结果；
- Flow/文件/AI/协同/OpenAPI 的秘密值无任何回显路径；
- 非幂等请求不会因 `retryable=true` 被前端擅自重放；
- 未迁移普通业务异常行为无回归。

**回滚**

- 按模块关闭增强响应或回退到旧异常构造器；
- 保留错误定义和文档为 deprecated，不复用 errorKey；
- 本阶段不包含数据表变更，无数据库回滚。

### 阶段 6：按模块渐进迁移与长期治理

**依赖**：阶段 5 的样板和门禁稳定。

**实施内容**

- 按 `auth/tenant/flow/lowcode/ai/file/collab/openapi` 建立模块 Owner；
- 优先迁移重复出现、需要客服检索、需要跨系统关联的业务错误；
- 新增代码规则：需要文档的配置/系统错误禁止使用裸字符串 BusinessException；
- 将错误清单、文档覆盖率和敏感扫描纳入合并门禁；
- 定期审查 deprecated、孤立文档、未命中定义和高频未知错误。

**产出**

- 模块迁移看板；
- 新增错误开发规范；
- CI 治理规则；
- 持续质量报告。

**验收**

- 新增管理员配置/启动/依赖错误 100% 使用统一定义；
- 新增公开 errorKey 100% 同 PR/同发布批次提供文档；
- 存量迁移不以“全部 2455 处清零”为目标，而以高频、重复和可行动错误覆盖率为目标；
- 连续两个发布周期无错误码重复、文档孤岛或敏感信息回归。

**回滚**

- 单模块规则可以暂时降级为警告，但全局唯一性和敏感信息检查不可关闭。
- 已发布 errorKey 只能 deprecated，不能删除后复用。

## 16. 文件影响清单

> 以下是预计范围；进入每阶段 `/apply` 前在对应 tasks 中落实到准确文件。未列出不代表可无审查扩展范围。

### 16.1 Forge Project

| 范围 | 预计变更 |
|---|---|
| Core | `RespInfo.java`、`BusinessException.java`、`GlobalExceptionHandler.java`、新增 `error/` 契约与工厂 |
| Auth | `SaTokenExceptionHandler.java` 及鉴权错误定义 |
| Log | TraceId Filter、MDC/异步传播配置及测试 |
| Diagnostics | 新 Starter 模块、自动配置、FailureAnalyzer、Resolver、错误目录和测试 |
| 应用 | Admin/Flow/Report/App 的 POM/自动配置接入与 Docs Base URL 配置 |
| Flow | `FlowClientProperties`、`JobFlowRemoteProperties` 相关诊断与一致性测试 |
| 配置模块 | 文件、AI、协同、OpenAPI/MCP/OAuth2 的错误定义和调用点 |
| Admin UI | `interceptors.js`、`helpers.js`、`error-dialog.js` 及相应测试 |
| Scripts/CI | Manifest 导出、错误目录检查、敏感信息扫描入口 |

### 16.2 Forge Website

| 范围 | 预计变更 |
|---|---|
| 错误中心 | 新增 `forge-docs/support/errors/**` |
| 导航 | `.vitepress/sidebar.js`、必要的 redirects/config |
| 旧文档 | `support/error-index.md`、`reference/api/error-codes.md`、`development/backend/error-handling.md` |
| 场景页 | `start/**`、`workflow/troubleshooting.md`、`lowcode/troubleshooting.md` 等链接收敛 |
| 构建 | `package.json` scripts、Manifest/Markdown 校验脚本与测试夹具 |

## 17. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|---|---|---|---|
| 无 | 无 | 无 | 错误定义采用代码和文档管理，不新增数据库表 |

明确不建设“错误码配置表”。如果以后需要国际化或租户品牌文案，需单独 Spec 设计版本、缓存、权限、审核和安全边界。

## 18. 接口与配置变更

### 18.1 响应接口

| 操作 | 接口 | 变更 |
|---|---|---|
| 兼容新增 | 所有失败的 `RespInfo` 响应 | 可选 `errorKey/traceId/help` |
| 兼容新增 | HTTP 响应头 | `X-Trace-Id` |
| 不变 | 成功响应 | 无错误帮助字段 |
| 不变 | 真实 HTTP 状态策略 | 本变更不统一切换 |

### 18.2 配置项

建议统一前缀：

```yaml
forge:
  error:
    enriched-response-enabled: true
    trace-enabled: true
    docs-base-url: https://docs.example.com
  diagnostics:
    startup:
      enabled: true
```

规则：

- `docs-base-url` 不包含 error slug，前端/后端统一安全拼接；
- 生产配置不使用示例域名；
- 所有配置提供 `@ConfigurationProperties` 元数据和默认值测试；
- 关闭增强响应不影响 TraceId 日志关联；
- 启动诊断开关必须能在 ApplicationContext 创建前读取。

## 19. 测试策略

### 19.1 测试层级

| 层级 | 重点 |
|---|---|
| 纯单元测试 | 错误定义约束、参数渲染、暴露策略、Resolver 优先级、脱敏 |
| Web 契约测试 | 新旧 RespInfo、鉴权、未知 500、响应头/体 TraceId、一致性 |
| 自动配置测试 | Starter 启用/禁用、缺少可选依赖、FailureAnalyzer 自身失败 |
| 前端单元/组件测试 | 标准化、弹窗字段、401、Blob、网络错误、URL 白名单、复制内容 |
| 静态/构建测试 | Manifest 确定性、唯一性、配置键、文档覆盖、死链、敏感扫描 |
| 用户侧真实验收 | MySQL/Redis/端口/服务启动及浏览器 E2E |

### 19.2 必测异常样本

- 当前 `Illegal mix of collations` 完整 cause chain；
- Collation 被 Flyway/SQLScriptExecutor 多层包装；
- 数据库连接拒绝、认证失败、unknown database；
- Flyway checksum/migration 通用失败；
- Redis 连接和认证失败；
- `java.net.BindException: Address already in use`；
- 旧 `BusinessException(String)`、带 `data` 的旧异常；
- BusinessException 包装 SQL 异常；
- RuntimeException 包装已识别 BusinessException；
- NullPointerException 和未知 Exception；
- 伪造超长 TraceId、带换行 TraceId、线程池复用；
- 恶意 docsUrl、带 Token Query 的 URL、响应体含密码字段。

### 19.3 安全断言

测试对所有公开响应、复制文本、启动摘要和生成文档扫描以下内容：

```text
password, passwd, secret, token, authorization, cookie,
jdbc:, SELECT/INSERT/UPDATE/DELETE, stack trace,
本机绝对路径、私钥头、AccessKey/SecretKey 模式
```

关键词扫描只是兜底；仍需针对每种异常验证精确 JSON 快照和日志/响应分离。

### 19.4 覆盖目标

- Core 新增错误契约、响应工厂、暴露策略：分支覆盖率 ≥ 90%；
- 启动 Resolver 和 FailureAnalyzer：分支覆盖率 ≥ 90%；
- 前端标准化和 URL/脱敏函数：分支覆盖率 ≥ 90%；
- 每个首批 errorKey 至少一个正向命中、一个近似但不应命中的反向用例；
- 所有公开定义 100% 通过 Manifest/Docs 契约测试。

### 19.5 执行责任

- Codex/实施者：代码、单元测试、静态检查、前后端构建、文档构建和契约校验；
- 用户：真实 MySQL/Redis、真实服务启动、Flyway、浏览器端 E2E 和部署环境验证；
- 实施阶段不得擅自修改真实数据库或启动长期驻留服务。

本变更必须创建独立 `test-spec.md`，并按阶段记录命令、结果和用户侧验收项。

## 20. 总体验收标准

- [ ] 现有成功响应、旧业务异常和认证跳转无兼容性回归。
- [ ] 新错误定义编译期可引用，errorKey 全局唯一、稳定、可弃用但不可复用。
- [ ] HTTP 响应、前端、日志、启动输出、Manifest、文档使用相同 errorKey。
- [ ] TraceId 在请求、响应头、错误体和 MDC 中一致，生命周期无串线。
- [ ] 已识别配置错误向正确角色展示原因、配置位置、期望值、动作和文档。
- [ ] 最终用户看不到管理员配置细节；任何用户都看不到秘密和原始系统异常。
- [ ] 未知 500、数据库异常和嵌套异常均通过脱敏测试。
- [ ] Collation 故障能稳定识别为 `START-DB-COLLATION-MIXED`，并链接正确文档。
- [ ] Admin/Report/Flow/App 端口文档使用 8580/8581/8081/8583 基线。
- [ ] Forge Docs 不再维护虚构 `1001`～`1303` 错误码，响应示例使用 `message`。
- [ ] 免费/付费适用范围清晰，公共错误答案不重复维护。
- [ ] CI 能阻止重复错误、缺失文档、孤立错误页、错误配置键、死链和敏感信息。
- [ ] 所有阶段均有独立发布与回滚记录，不要求一次迁移全部存量异常。

## 21. 发布、灰度与回滚策略

### 21.1 发布顺序

单个错误从无到有的标准发布顺序：

```text
错误定义与兼容代码（未启用/不返回 docsUrl）
  -> 文档页面与校验上线
  -> 后端启用增强响应/启动诊断
  -> 前端增强展示
  -> 观察未知错误率和用户反馈
```

阶段 2 已发布前端基础能力后，后续错误可采用“文档先行，后端定义随后”的小批次发布。

### 21.2 灰度观察

只统计不含敏感内容的聚合指标：

- 各 errorKey 次数和服务分布；
- 未知错误比例；
- Resolver 命中/回退比例；
- 文档链接点击失败率；
- 前端重复弹窗和认证流程回归；
- 启动分析器自身失败次数。

### 21.3 回滚原则

- 优先关闭增强展示/分析器开关，不删除已发布错误定义；
- 新增 JSON 字段均为可选，旧前端可继续工作；
- 错误页 URL 发布后保留重定向；
- 已发布 errorKey 回滚为 deprecated，不复用；
- 无数据库 Schema 变更，不需要数据库回滚；
- 若响应脱敏出现风险，立即关闭 `enriched-response-enabled`，保留通用 message + TraceId。

## 22. 风险与缓解措施

| 风险 | 影响 | 缓解 |
|---|---|---|
| 一次迁移过多 | 大面积行为回归 | P0/P1 分批，只迁移高价值错误 |
| 新旧 `code` 语义混乱 | 前端/第三方兼容失败 | `errorKey` 承担稳定语义，不在本变更重构 Transport Status |
| 原始异常误入 help | 泄密 | 静态安全文案、ExposurePolicy、快照和敏感扫描 |
| Resolver 误判 | 给出错误操作建议 | 稳定 code 优先、反向样本、无法确认则回退 |
| 启动分析器依赖容器 | 启动时再次失败 | 纯 Java、隔离依赖、分析器异常不遮蔽原异常 |
| 文档与代码跨仓库漂移 | 链接 404/说明错误 | 确定性 Manifest 和双向 CI |
| 免费/付费文档混杂 | 用户无法判断能否使用 | `editions` Frontmatter 和构建校验 |
| docsUrl 被利用 | 前端开放跳转/注入 | 站内 slug + 域名白名单 |
| TraceId 被伪造 | 日志检索误判/注入 | 格式校验、只用于相关性、记录服务/环境 |
| 前端自动重试 | 非幂等操作重复 | `retryable` 只展示，不直接触发自动重放 |
| Flow 默认端口不一致 | 新环境配置误导 | 阶段 5 做专项一致性检查，业务修改另行确认 |

## 23. 技术决策

| 决策 | 选择 | 原因 |
|---|---|---|
| 错误来源 | Java 类型安全定义 | 可审查、可测试、无需数据库、适合启动阶段 |
| 稳定编号 | 符号 `errorKey` | 比纯数字可读，避免与 HTTP/历史业务码冲突 |
| 兼容方式 | 保留 `code/message/data/timestamp`，新增可选字段 | 降低旧前端和第三方调用方风险 |
| 未知异常 | 安全文案 + errorKey + TraceId | 兼顾安全和可关联性 |
| 启动诊断 | Spring Boot FailureAnalyzer/早期 Reporter | HTTP 服务未就绪时仍可工作 |
| 文档同步 | 构建期 Manifest 双向校验 | 不依赖生产服务，跨仓库可重复 |
| 文档组织 | 一个规范错误页 + 多个场景入口 | 避免重复答案漂移 |
| 数据存储 | 不新增错误配置表 | 错误语义不应被运行时随意修改 |
| 迁移策略 | 高价值错误优先、模块渐进 | 控制 2455 个存量调用点的风险 |
| HTTP 状态 | 本变更不强制统一 | 属于独立兼容性议题 |

## 24. 待澄清

当前没有阻塞 Spec 的未决项。以下事项采用默认决策，若实施前业务方提出不同要求，再通过 Spec 修订记录调整：

1. 文档正式域名由部署配置 `forge.error.docs-base-url` 注入，代码只保存站内 slug。
2. 首批错误文档默认同时适用于免费版和付费版；仅付费功能标记 `editions: [pro]`。
3. Transport HTTP Status 保持现状，不纳入本变更。
4. 国际化不纳入本期；字段结构预留，但不引入动态翻译表。
5. 前端只展示服务端已经裁剪的帮助，不在浏览器内维护第二份错误知识库。
6. Flow 默认地址与端口不一致只列为阶段 5 专项，是否修改默认值需要独立影响确认。

## 25. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|---|---|---|---|
| 完整架构 Spec | complete | `code-copilot/changes/unified-error-diagnostics/spec.md` | 本轮只编写 Spec，未修改业务代码 |
| 阶段 0～6 实施 | pending | — | HARD-GATE 确认后按阶段创建任务和测试规格 |

## 26. 审查结论

### 26.1 实施者视角自检

- 架构责任已经拆到后端 Core、Log、Diagnostics、各业务模块、Admin UI、Docs 和 CI；
- 每个阶段均定义输入/依赖、实施内容、产出、验收和回滚；
- 响应兼容、Transport Status 非目标和存量渐进迁移边界明确；
- 启动错误不依赖应用成功启动；
- 文档发布顺序避免 `docsUrl` 先上线造成 404；
- 安全边界覆盖原始异常、SQL、凭据、URL、TraceId 和角色裁剪；
- 没有要求一次整改全部异常，也没有引入错误码数据库；
- 真实数据库、服务启动和 E2E 明确由用户侧执行。

### 26.2 进入实施前必须补齐

HARD-GATE 确认后，阶段 0 开始前创建：

- `tasks.md`：按阶段拆分可勾选任务、准确文件和依赖；
- `test-spec.md`：自动测试命令、样本、预期结果和用户侧真实验收清单；
- `execution-log.md`：每阶段实际改动、测试结果、偏差和回滚点。

## 27. 确认记录（HARD-GATE）

- **当前状态**：等待用户确认 Spec；不得进入 `/apply` 或修改业务代码。
- **确认范围**：总体架构、响应协议、安全边界、首批错误范围、阶段 0～6 顺序。
- **确认时间**：待用户确认
- **确认人**：待用户确认

