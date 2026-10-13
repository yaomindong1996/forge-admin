# 踩坑：安全 / 加密 / 租户 / 鉴权

> 从 `code-copilot/memory/pitfalls.md` 按主题拆出。新条目追加到本文件。共 20 条。

## 业务码 4091 不能当 HTTP status，否则登录选工作区会显示 500


**发现日期**：2026-09-28

`/auth/login` 密码校验通过后，多租户账号会抛 `BusinessException(4091, 请选择要进入的工作区, options)`。`GlobalExceptionHandler.setHttpStatus` 若把非 400–599 的业务码一律落成 HTTP 500，Network 面板会显示 500，但响应体仍是 `code:4091` 和租户列表。

处理原则：只有业务码本身是合法 HTTP 状态码时才写入 `response.setStatus`；像 4091 这类语义码应返回 HTTP 200，由响应体 `code` 表达业务含义。前端用 `applyWorkspaceChallenge` 识别 4091 弹工作区选择。

## 多租户拦截器会破坏 LIMIT 1 FOR UPDATE 顺序


**发现日期**：2026-08-21

MySQL 的锁定查询必须把 `LIMIT` 放在 `FOR UPDATE` 前，但 Forge 多租户 JSqlParser 在改写带租户 SQL 时可能把 `LIMIT 1 FOR UPDATE` 重排为 `FOR UPDATE LIMIT 1`，从而触发 MySQL 语法错误（常见于 `IgnoreTenantAspect` 调用栈）。

处理原则：带锁查询如果使用主键、租户 + 主键或其他唯一条件，不要再写冗余 `LIMIT 1`，直接使用 `... WHERE 唯一条件 FOR UPDATE`；新增锁查询应通过 Mapper 契约测试检查两种错误顺序均不存在。

## 登录密码 RSA 不能复用通用 API 传输加密开关


**发现日期**：2026-08-04

登录密码属于独立的凭据保护协议，通用 API 加解密属于业务报文传输协议。若前端根据硬编码或通用 `forge.crypto.enabled` 决定是否 RSA，而后端又使用另一份配置，关闭通用加密后会出现“一端发 RSA 密文、另一端按明文哈希校验”的必然登录失败。

处理原则：登录配置单独维护 `enablePasswordEncryption`，通过匿名 `/auth/loginConfig` 下发；前端开启时获取 `/crypto/public-key` 并失败关闭，后端两种密码认证策略统一复用同一个 RSA 解码器。服务端不得信任客户端自报的 `encrypted` 字段。H5、报表等公共浏览器客户端也不得硬编码 AppSecret，客户端身份只使用公开 appId，固定 Secret 仅适用于能安全保管密钥的服务端机密客户端。

---

## 登录前、Token 事件和定时任务访问租户表必须显式建立上下文


**发现日期**：2026-08-03

登录签发前、Sa-Token 登出/顶号/续期监听以及调度线程不一定经过 HTTP 租户拦截器。若这些路径直接访问 `sys_auth_online_user` 等租户表，严格租户模式会抛出“访问租户表时缺少租户上下文”。

处理原则：登录前使用认证结果中的可信 `tenantId`；Token 事件优先使用 Token Session 中的可信租户，必要时按不可伪造的精确 Token 受控跨租户定位记录后再回到单租户操作；明确的系统级全租户清理才使用 `executeIgnore`。所有路径都必须在最小数据库边界执行并在 `finally` 中恢复原租户和忽略标记，不能把业务表加入全局租户忽略名单。

在线会话登出属于状态流转和审计保留，应更新离线状态、时间和类型，不能误用 Mapper `delete(wrapper)` 物理删除记录。

---

## 租户拦截 SQL 不要使用 MySQL NULL 安全等号


**发现日期**：2026-08-03

MyBatis-Plus 租户拦截器会先用 JSqlParser 解析并改写租户表 SQL。当前解析器无法解析 MySQL `<=>` NULL 安全等号，表现为 Mapper 抛出 `MyBatisSystemException`，且日志中不会出现该语句的 `Preparing` 记录。

需要比较可空字段时，改用解析器兼容的标准等价表达式：

```sql
AND (nullable_column = #{value}
     OR (nullable_column IS NULL AND #{value} IS NULL))
```

此规则适用于查询和条件更新，不能通过关闭租户隔离规避。异常日志应附带堆栈方便确认解析失败，但不得打印 Token、密钥或业务报文。

---

## 5. 报表项目保存/读取接口缺少加解密注解导致配置不生效


**发现日期**: 2026-05-11

**问题描述**:
报表页面发布后重新进入，动态接口配置或组件删除等画布变更看起来没有保存成功。前端请求链路正常，但后台接口缺少 `@ApiEncrypt` / `@ApiDecrypt` 时，响应/请求与前端加密拦截链路不匹配，导致保存或读取结果异常。

**正确用法**:
报表项目保存、读取、发布等需要经过前端加密请求链路的接口，后端必须按项目规范补齐：

```java
@ApiDecrypt
@PutMapping
public RespInfo<Void> update(@RequestBody GoviewProject project) {
    ...
}

@ApiEncrypt
@GetMapping("/{id}")
public RespInfo<GoviewProject> getById(@PathVariable Long id) {
    ...
}
```

**解决方案**:
排查“前端已传参但后台保存/回显不生效”时，除字段映射、租户条件外，必须检查接口是否补齐 `@ApiEncrypt` / `@ApiDecrypt`。

---

## 8. 顶部菜单目录不应直接按自身 path 跳转


**发现日期**: 2026-05-31

**问题描述**:
布局设置为“顶部菜单”时点击“流程管理”，会从目录路径 `/flow` 落到无匹配路由，再进入报表 SSO 桥，最终打开 `/report/design`。

---

## 6. SSO 接口缺少 `@ApiDecrypt` 会导致请求参数为空


**发现日期**: 2026-05-13

**问题描述**:
admin-ui / report-ui 的请求拦截器默认会对未排除的 `POST` JSON 请求体做 SM4/AES 加密。如果后端 SSO 接口仍按普通 `@RequestBody` 接收、但没有补 `@ApiDecrypt`，后台拿到的业务字段会是空值，表现为：

```text
java.lang.RuntimeException: 目标客户端不能为空
```

或：

```text
java.lang.RuntimeException: SSO票据不能为空
```

**典型场景**:
- `POST /auth/sso/ticket`
- `POST /auth/sso/exchange`

**正确用法**:
```java
@ApiDecrypt
@PostMapping("/sso/ticket")
public RespInfo<SsoTicketResult> createSsoTicket(@RequestBody SsoTicketRequest request) {
    ...
}

@SaIgnore
@IgnoreTenant
@ApiDecrypt
@PostMapping("/sso/exchange")
public RespInfo<LoginResult> exchangeSsoTicket(@RequestBody SsoExchangeRequest request) {
    ...
}
```

**根本原因**:
- admin-ui 默认只排除了 `/auth/login`、`/auth/captcha`、`/crypto/exchange` 等少数路径
- report-ui 也会对 `/forge-report-api/auth/sso/exchange` 默认加密
- 没有 `@ApiDecrypt` 时，请求体实际是 `{ data, algorithm }`，不会映射到 `targetClient` / `ticket`

**解决方案**:
- 对接前端默认加密链路的 SSO `POST` 接口必须补 `@ApiDecrypt`
- 不要优先用前端 `encrypt: false` 规避，除非这个接口明确约定为明文

**影响范围**:
- admin-ui -> report-ui 单点登录
- 所有新增的跨端票据交换、临时令牌类接口

---

## 7. X-Inner-Call 只能由可信内部系统配置触发


**发现日期**: 2026-05-11

**问题描述**:
外部接口代理如果允许接口自定义请求头直接传 `X-Inner-Call: true`，调用 Forge 服务时可能绕过 API 加解密和重放校验。

**根本原因**:
`forge-starter-crypto` 在请求头 `X-Inner-Call=true` 时会跳过请求解密、响应加密和重放校验，这个头属于服务间内部调用信任边界，不应由普通接口配置任意设置。

**解决方案**:
在 `sys_external_system` 增加 `trusted_internal` 配置。只有该配置为 true 时，`ExternalProxyService` 才主动添加 `X-Inner-Call: true`；同时过滤接口自定义请求头里的 `X-Inner-Call`。

**影响范围**:
- 外部系统配置
- 外部接口代理调用
- 所有调用 Forge 内部服务并依赖 `X-Inner-Call` 跳过加解密的场景

## 9. 跨系统 SSO 首跳前的 `/crypto/exchange` 必须匿名放行


**发现日期**: 2026-05-13

**问题描述**:
从 `admin-ui` 单点进入 `report-ui` 时，目标系统会先请求 `/crypto/public-key` 和 `/crypto/exchange` 建立会话密钥，再调用 `/auth/sso/exchange`。如果 `/crypto/exchange` 仍要求已登录，会直接报：

```text
未登录异常：未能读取到有效 token，请求地址：/crypto/exchange
```

**根本原因**:
`report-ui` 的 SSO 登录页在拿到目标系统 token 之前，也要先走 API 加密链路。此时密钥交换接口只能依赖匿名 `X-Session-Id` 建立临时会话，不能被 `SaTokenConfig` 的登录拦截器拦住。

**解决方案**:
- 在 `forge-starter-auth` 的 Sa-Token 白名单里显式排除 `/crypto/public-key`、`/crypto/exchange`
- 在 `KeyExchangeController.exchangeKey` 上补 `@SaIgnore`，明确该接口允许匿名密钥协商

**影响范围**:
- `admin-ui -> report-ui` 单点登录
- 所有“未登录先协商动态密钥，再换发 token”的跨系统接入场景

## 21. 菜单路径与文件自动路由不一致导致 404


**发现日期**: 2026-05-27

**问题描述**:
`sys_resource.path` 配置为 `/app-center/engines`、`/app-center/suite/:suiteCode` 等业务路由，但前端页面文件使用 `engine-center.vue`、`suite-detail.vue` 等命名时，`unplugin-vue-router` 自动生成的路由分别是 `/app-center/engine-center`、`/app-center/suite-detail`，菜单点击会进入 404。

**解决方案**:
- 优先让页面文件路径匹配菜单路径，例如 `src/views/app-center/engines.vue`；动态二级路径用 `dotNesting` 扁平命名，例如 `src/views/app-center/suite.[suiteCode].vue` 对应 `/app-center/suite/:suiteCode`。
- 不要为业务菜单长期在 `src/router/index.js` 手写路由；该文件只保留白名单、兼容动态路由、SSO 桥接等少量特殊路由。
- 父级入口有子菜单时优先建成目录资源，另建可点击首页菜单，避免父菜单既当目录又当页面入口。

**影响范围**:
- 使用 `sys_resource` 创建的新业务菜单
- 文件自动路由和动态参数页面

## 71. 异步日志补全用户信息必须忽略租户条件


**发现日期**: 2026-06-22

**问题描述**:
开启租户业务数据源异步上下文传播后，操作日志异步线程可能出现：

```text
DefaultTenantLineHandler - 当前上下文中没有租户ID，请检查租户设置
SystemLogServiceImpl.saveOperationLog - sysUser is null
```

**根本原因**:
日志服务根据 `userId` 补全 `SysUser` 信息时，属于系统级主库查询。如果直接执行 `sysUserMapper.selectById`，会被当前线程的租户拦截器追加 `tenant_id` 条件；异步线程没有租户 ID 或租户上下文被切到业务场景时，就会查不到用户并触发空指针。

**解决方案**:
- 日志、在线用户、登录态补全等系统用户查询使用 `TenantContextHolder.executeIgnore`。
- 查询结果必须做 null 保护，查不到用户时保留日志原始信息并继续落库。
- 线程池 `TaskDecorator` 传播租户上下文时需要同时传播并恢复 `TenantContextHolder.isIgnore()` 状态。

**影响范围**:
- `SystemLogServiceImpl`
- `TenantBusinessDataSourceTaskDecorator`

## 72. 租户业务数据源切换后数据权限不能访问业务库里的平台表


**发现日期**: 2026-06-22

**问题描述**:
业务 Mapper 使用租户默认业务数据源后，数据权限拦截器可能报错：

```text
Table 'forge_admin_test.sys_data_scope_config' doesn't exist
```

**根本原因**:
`DataScopeInterceptor` 在拦截业务 Mapper 时需要读取 `sys_data_scope_config` 等数据权限控制面配置。如果这一步发生在 dynamic-datasource 已经切到租户业务库之后，平台配置查询会被错误路由到业务库。

类似问题不只限于 `sys_data_scope_config`：角色数据范围、自定义组织、组织子级和行政区划子查询也可能访问 `sys_role`、`sys_role_data_scope`、`sys_org`、`sys_region_code` 等平台表。

**解决方案**:
- 数据权限控制面元数据固定从平台主库加载，默认使用 `forge.datascope.metadata-datasource=master`。
- 启动和配置刷新时预加载数据权限配置、角色范围、组织层级和行政区划父子关系到内存快照。
- 业务 Mapper 查询期间只读取内存快照，不在拦截器里实时访问平台表。
- REGION 权限不要生成 `IN (SELECT code FROM sys_region_code ...)`，应提前解析为业务库可执行的字面量 `IN (...)` 条件。

**影响范围**:
- `DataScopeServiceImpl`
- `DataScopeInterceptor`
- 租户业务数据源下所有带数据权限控制的业务 Mapper

## 73. 超级管理员区划树被登录用户 regionCode 误裁剪


**发现日期**: 2026-06-23

**问题描述**:
超级管理员打开用户/组织编辑表单时，行政区划树只能看到当前登录用户 `regionCode` 对应范围，例如只能选内蒙古，无法选择其他省份。

**根本原因**:
`/system/region/treeAll?dataRight=true` 在进入数据权限拦截器之前，服务层先用当前登录用户 `regionCode` 解析默认 `rootCode`。即使数据权限拦截器对超级管理员是 `ALL` 放行，SQL 仍然已经被 `rootCode` 条件限制。

**解决方案**:
- 超级管理员请求区划树时，不启用默认 `regionCode` 根节点裁剪，直接走无数据权限查询。
- 普通用户和租户用户继续按当前行政区划/角色数据权限过滤。

**影响范围**:
- `SysRegionServiceImpl.selectRegionTreeAll`
- 用户/组织编辑表单中的行政区划树

## 104. DashScope Core、Starter 和 Compatible 地址不能混为一体


**发现日期**: 2026-07-10

**问题描述**:
在多租户 AI 供应商系统中，如果直接引入 `spring-ai-alibaba-starter-dashscope`，或者只根据供应商品牌/Base URL 猜测协议，容易同时产生自动配置冲突和请求路径错误。官方根 README 的示例版本还可能落后于 release POM/BOM，不能作为最终依赖基线。

**根本原因**:
- DashScope Starter 的自动配置条件允许缺省启用，会尝试读取 `spring.ai.dashscope.*` 或环境变量创建全局模型 Bean，与 Forge 按租户从数据库动态读取 API Key 的模式冲突；
- DashScope Native 使用官方根地址和原生 generation path，OpenAI Compatible 使用 `/compatible-mode`，同一品牌不代表同一协议；
- 旧应用只会按 OpenAI Compatible 构建模型，无法理解新增的 Native Adapter；
- 项目 release 的真实版本关系由发布 POM/BOM 决定，README 示例可能仍保留旧版本号。

**解决方案**:
- 多租户动态凭据场景只依赖 `spring-ai-alibaba-dashscope` Core，通过运行时 Builder 创建模型，不引入 Starter；
- 用独立 `adapter_code` 显式区分 `openai_compatible` 与 `dashscope_native`，官方 DashScope 域名执行双向 URL 校验，不根据品牌或 URL 自动切协议；
- 历史记录保持 Compatible，切换 Native 必须由管理员显式操作并测试；
- 回退旧应用前先检查 Native 记录，逐条切回 Compatible URL/config 并通过连接测试；
- 依赖版本以 release POM/BOM 和实际 `dependency:tree` 为准。当前验证基线是 Spring AI `1.1.2`、Spring AI Alibaba/Extensions `1.1.2.3`。

**影响范围**:
- `forge-plugin-ai` 的供应商模型构建、连接测试和缓存；
- 多租户 API Key 配置；
- DashScope Native/Compatible 协议切换；
- AI 依赖升级与旧应用回退。

## 114. 加解密总开关关闭时不能在 Bean 构造阶段校验密钥


**发现日期**: 2026-07-16

**问题描述**:
`SM4Encryptor`、`AESEncryptor` 在构造函数中解析并校验 `secretKey` 时，即使 `forge.crypto.enabled=false`，数据库中残留的掩码、占位符或错误长度密钥仍会导致 Spring Bean 创建失败，服务无法启动。配置中的 RSA 公私钥也可能在关闭状态触发同类初始化异常。

**解决方案**:
- SM4/AES Bean 构造只保存动态 `CryptoProperties` 引用，不解析默认密钥。
- 只有实际调用无显式会话密钥的静态加解密方法时，才读取并校验当前默认密钥；这样运行时刷新后也不会继续使用旧缓存密钥。
- 全局关闭时忽略配置中的 RSA 密钥对并生成内部临时密钥，防止无效历史配置阻断启动；重新开启动态密钥协商仍使用服务端当前公钥正常工作。
- 回归测试必须同时覆盖关闭状态无效 SM4/AES/RSA 值，以及重新开启后使用最新有效密钥完成往返。

**影响范围**:
- `forge-starter-crypto` 的 Spring 启动装配。
- 配置中心关闭、重新开启或更新加密密钥后的运行时行为。

## 127. 外部化 Secret 必须同时封堵高优先级数据库配置源


**发现日期**: 2026-07-26

**问题描述**:
只把 YAML 密钥替换为环境变量并不能保证外部 Secret 生效。Forge 的数据库 PropertySource 使用 `addFirst`，`sys_config` 和 `sys_config_group` 中的历史键仍可覆盖环境变量；Spring Map 绑定还支持 `keys.previous` 与 `keys[previous]` 两种写法，精确拦截点号形式会留下方括号旁路。

**解决方案**:
- 外部化部署密钥时同时审计所有 PropertySource、配置转换器、通用配置 API、散配置 API 和初始化数据。
- 使用共享的 relaxed-binding 归一化策略拦截部署级键，并同时识别 Map 点号和方括号写法。
- Flyway 物理清除历史数据库值；管理响应清洗、写入拒绝和非法 JSON 失败关闭必须形成完整边界。

**影响范围**:
- 所有由环境变量、挂载文件或 Secret Manager 注入，但系统同时支持数据库动态配置的敏感属性。

## 159. REQUIRES_NEW 建单后外层 REPEATABLE_READ 可能仍看不到新记录


**发现日期**: 2026-08-03

**问题描述**:
申请提交为了幂等恢复，会在 `REQUIRES_NEW` 事务中创建业务记录并提交 `recordId` 检查点，再由外层事务启动流程。如果外层事务在内层提交前已经执行过一致性读取，MySQL `REPEATABLE-READ` 会固定旧快照；即使数据库中已经存在新记录，外层紧接着按主键查询仍返回空，最终被误报为“记录不存在或无权限”。这类问题具有迷惑性：审计字段、租户、组织和真实物理数据全部正确。

**解决方案**:
- 明确跨事务可见性契约：需要消费内层已提交检查点的外层动作使用 `READ_COMMITTED`，不要依赖“把元数据查询挪到后面”这种脆弱的语句顺序；
- 建单和 `recordId` 检查点继续在同一 `REQUIRES_NEW` 事务中提交，流程失败后相同幂等键复用记录，不能为修复可见性而取消独立检查点；
- 排查时同时查看流程动作日志中的检查点 recordId、业务表审计字段、数据库隔离级别和外层事务首次 SELECT 的时机；
- “不存在或无权限”对外继续使用防枚举统一文案，但内部安全日志应记录 tenant/object/config/record/user/org 等定位元数据；
- 外围 USER 委托的数据权限上下文直接使用可信 `ExecutionIdentity`，不能先要求 Sa-Token 已登录再读取委托用户。

**影响范围**:
- 任何采用“外层编排事务 + 内层 REQUIRES_NEW 创建资源/检查点 + 外层立即读取”的幂等工作流、能力提交和流程启动链路。

## 171. 动作路径 `record.*` 的单测必须构造服务端权威记录上下文


**发现日期**：2026-08-11

`BusinessActionStepConfigHelper.resolvePath("record.id", context)` 读取的是执行服务按 `recordId` 和数据权限重新查询后写入的 `recordData`，不是客户端请求 DTO 的 `recordId`。单测若只设置 `context.request.recordId`，却配置 `targetRecordIdField=record.id`，会得到“缺少目标记录 ID”；把解析器降级为读取客户端 request 会破坏可信数据边界。

测试执行器时应显式构造与 `BusinessActionExecutionService.buildContext` 一致的 `recordData`；测试请求回退语义时则不要配置 `targetRecordIdField`。主子表动作还应分别构造权威 `parentRecordData` 和子行 `recordData`，避免用浏览器自报父子记录模拟服务端关系校验。

## 系统管理员保护不能只挡「用户删除」接口

**发现日期**: 2026-10-13

**问题描述**:
用户删除 API 已禁止删 `userType=0`，但租户「移出用户」在最后一条成员关系时会 `userMapper.deleteById`，旁路物理删除超管。同类缺口还有：停用/降级唯一超管、在线封禁超管、删除/停用 `isSystem` 角色、禁用默认租户；前端曾用 `id===1` 做保护，其它超管账号仍暴露危险操作。

**解决方案**:
- `removeTenantUser` 与 `removeUserFromTenant` 对齐：超管只解绑不硬删。
- `SysUserAccessPolicy` 增加停用/降级「最后一名启用超管」校验与封禁禁令。
- 内置角色禁删/禁停；默认租户禁禁用/禁设为已过期。
- 前端按 `userType===0` / `isSystem===1` 隐藏危险操作，不再写死 id。

## 超级管理员不能全局忽略租户隔离

**发现日期**: 2026-08-31

**问题描述**:
`TenantInterceptor` 对超级管理员 `setIgnore(true)` 后，右上角切换租户只改 Session，用户/组织/角色列表仍可能看到全部租户。页面再铺「所属租户」筛选和下拉，普通用户也无法理解。

**解决方案**:
- 超级管理员也把当前 Session 租户当作工作区，SQL 按该租户隔离。
- 平台表（如 `sys_tenant`）继续走 ignoreTables；跨租户绑定只留在租户管理和用户高级关系。
- 登录页和顶栏仅在可切换租户数 > 1 时展示选择器。

## 初始化 SQL 不能从开发库直接导出存储凭据

**发现日期**: 2026-09-30

**问题描述**:
`全量初始化SQL.sql` 和 `docker-forge-admin/init-sql/01-init.sql` 的 `sys_file_storage_config` 带着一条腾讯 COS 配置，里面有真实 SecretId/SecretKey。测试数据里的预签名 URL（`ai_report_project`、`sys_file_metadata`）也带着 `q-ak=AKID...`。

**解决方案**:
- 初始化 SQL 只保留本地存储 `local`，凭据字段为 NULL；泄露过的密钥必须到云控制台轮换，只删文件不够，git 历史仍在。
- 从开发库重新导出全量 SQL 后，先搜 `AKID`、`LTAI`、`sk-`、`secret` 再提交。
