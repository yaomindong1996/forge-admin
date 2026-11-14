# Forge 安全审计问题整改 Tasks

> 关联 Spec：`spec.md`  
> 状态：未开始  
> 执行规则：每个任务遵循“先测试、再实现、再验证、人工审查、记录证据”；不得覆盖工作区既有改动。

## Phase 0：边界冻结与基线

### T0.1 建立问题基线和配置清单

- [ ] 读取本变更 `spec.md`、`test-spec.md`、`execution-log.md`，并记录当前 `git status --short`。
- [ ] 导出 Controller 路由、API 配置、`sys_resource` 资源、外部 API `permission_check_enabled`、网关环境变量和默认 client/grant 清单。
- [ ] 生成报表脚本、后端 ScriptAdapter、`v-html`、JDBC 临时测试、SQL 拼接和文件入口清单；每项标记数据来源和可控角色。
- [ ] 建立 P0/P1 人工审查表，未经确认不得把“配置待确认”问题标为已修复。

### T0.2 固化测试运行环境

- [ ] 使用 JDK 17 Maven Toolchain，修复定向测试对 `forge-admin-server/sql/初始化脚本.sql` 的过期引用，改为仓库实际初始化脚本路径或测试专用夹具。
- [ ] 在 CI 固定 Node 20 和 `pnpm --ignore-workspace`，保留既有前端构建命令。
- [ ] 在测试记录中保留本机 Mockito/Byte Buddy 限制，不得将环境失败记录为代码通过。

## Phase 1：P0/P1 安全边界

### T1.1 隔离报表动态脚本和危险 HTML

**修改范围：**

- `forge-report-ui/src/hooks/useLifeHandler.hook.ts`
- `forge-report-ui/src/api/http.ts`
- `forge-report-ui/src/utils/utils.ts`
- `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartData/components/ChartDataMonacoEditor/index.vue`
- `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartEvent/components/ChartEventBaseHandle/index.vue`
- `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartEvent/components/ChartEventAdvancedHandle/index.vue`
- `forge-report-ui/src/packages/components/Tables/Tables/TableScrollBoard/index.vue`
- `forge-report-ui/src/packages/components/Tables/Tables/TableList/index.vue`
- `forge-report-ui/src/components/FgAI/AIChatPanel.vue`

- [ ] 先新增脚本执行器契约测试：禁止主线程 `Function` 构造器、禁止访问 `window/document/fetch/storage/cookie`、限制脚本长度/执行时间/调用次数。
- [ ] 以受限 DSL/JSONPath 替代数据过滤和 URL 模板执行；URL 只能由服务端提供的参数模板生成，禁止模板读取全局对象。
- [ ] 确需脚本的事件迁移到 Worker 或 sandbox iframe，仅暴露冻结的最小 API，并在销毁组件时终止执行上下文。
- [ ] 所有动态 HTML 经过 allowlist 清洗或改为文本渲染；为恶意属性、SVG、事件属性和 URL 协议增加用例。
- [ ] 运行前端定向测试、ESLint、生产构建和 Playwright 恶意脚本验证。

### T1.2 移除主 JVM ScriptAdapter 任意执行

**修改范围：**

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/adapter/impl/ScriptAdapter.java`
- external adapter DTO、配置校验、Mapper XML、迁移脚本和相关单测

- [ ] 新增 DSL/字段映射适配器测试，覆盖允许操作、未知函数、超长配置、循环和超时。
- [ ] 将 `ScriptAdapter` 改为白名单转换器；旧脚本配置返回明确迁移错误，不得静默执行。
- [ ] 若业务确认必须保留 JavaScript，另建独立执行服务/容器协议，明确 CPU、内存、网络、超时和结果大小上限；主 JVM 只进行 RPC 调用和结果校验。
- [ ] 审计日志记录脚本版本、操作者、结果摘要，不记录脚本中的密钥和响应原文。

### T1.3 API 权限改为 fail-closed

**修改范围：**

- `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/interceptor/ApiPermissionInterceptor.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/PermissionServiceImpl.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysResourceMapper.xml`、用户权限加载服务、API 配置缓存
- Controller 路由扫描器、权限资源 Flyway 脚本

- [ ] 新增未配置资源、缓存异常、隐藏 API、通配路径和匿名白名单测试；默认结果必须为 403。
- [ ] 启动/CI 输出 Controller 路由与资源覆盖报告，缺失敏感接口阻止发布。
- [ ] 隐藏 API 参与匹配和用户权限计算；资源 `visible` 只影响菜单展示。
- [ ] 对明确匿名接口使用显式注解/白名单，禁止通过“未配置”获得匿名访问。
- [ ] 完成 Admin 登录、普通用户、无权限用户和缓存故障的接口验证。

### T1.4 外部系统/API/代理/日志权限闭环

**修改范围：**

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/controller/ExternalSystemController.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/controller/ExternalApiController.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/controller/ExternalProxyController.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/controller/ExternalApiLogController.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/support/ExternalPermissionGuard.java`、`ExternalQueryContractValidator.java`
- `forge-server/db/mysql/migration/` 外部资源脚本

- [ ] 为管理、调试、代理、日志清理接口定义明确权限标识和平台管理员边界。
- [ ] 外部 API 默认 `permissionCheckEnabled=true`；旧数据迁移前先盘点并提供兼容告警。
- [ ] 补齐 `/external/api/**`、`/external/proxy/**`、`/external/api/log/**` 的资源、角色和租户策略，使用 `NOT EXISTS` 防重复。
- [ ] 测试普通用户、跨租户用户、平台管理员、匿名请求和权限配置关闭时的结果。

### T1.5 开放网关和流程动作默认关闭

**修改范围：**

- `forge-server/forge-admin-server/src/main/resources/application.yml`
- capability open-gateway/identity/flow-actions 配置校验与自动装配
- capability 默认 client/grant 初始化脚本和启动检查

- [ ] 新增配置绑定测试，确认未设置环境变量时 open-gateway、flow-actions、identity 均为关闭。
- [ ] 启动检查拒绝空 pepper、弱密钥、默认 client/grant 和未绑定租户/组织的 SERVICE 身份。
- [ ] 保留 OAuth/HMAC、防重放、scope、RBAC、限流、幂等和高风险确认，测试关闭/开启两种模式。
- [ ] 生产开启必须有环境配置、审计快照和人工审批；回滚只切回关闭状态。

### T1.6 临时 JDBC 和 preview-sql 隔离

**修改范围：**

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/controller/DataConnectionController.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/support/JdbcDataSourceProvider.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/controller/DataDatasetController.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataDatasetAccessService.java`、`impl/DataDatasetAccessServiceImpl.java`、DTO 校验、出站地址策略

- [ ] 新增私网、环回、云元数据、非法驱动、非法协议、超时、并发和超大响应测试。
- [ ] 临时连接仅允许平台管理员，驱动/主机/端口白名单，固定 `SELECT 1`，强制连接和查询超时，使用后关闭数据源。
- [ ] `preview-sql` 必须引用已保存数据集或显式管理权限，执行行权限、列权限、脱敏和只读账号。
- [ ] 普通查询、临时连接、预览 SQL 的审计日志只记录摘要，不记录密码和完整 SQL 参数。

## Phase 2：认证、幂等和文件

### T2.1 会话吊销和登录配置隔离

**修改范围：**

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemAuthServiceImpl.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/ISysOnlineUserService.java`、在线会话存储、Sa-Token 登录模型
- 登录/改密/重置密码单测

- [ ] 新增改密、找回密码、管理员重置后旧 Token 访问必须失败的测试。
- [ ] 使用 `SaLoginModel` 设置单次 client 配置，不再调用 `SaManager.setConfig` 修改全局配置。
- [ ] 增加密码版本号或等价缓存校验，覆盖多实例和缓存失效场景。
- [ ] 保留当前会话是否继续有效的产品选择，但必须显式配置并审计。

### T2.2 幂等 Token 原子消费

**修改范围：**

- `forge-server/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/service/RedisTokenService.java`
- `forge-server/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/strategy/TokenRequiredStrategyHandler.java`
- Redis Lua 脚本/序列化、并发测试

- [ ] 先写并发测试，两个请求共用 token 时最多一个进入业务方法。
- [ ] 使用 Lua 原子完成存在性、状态、过期和消费；消费失败返回统一异常。
- [ ] 移除完整 Token 日志，改为不可逆摘要和 prefix。
- [ ] 测试 Redis 重试、超时、节点切换和已消费 Token 的行为。

### T2.3 验证码原子消费和发送风控

**修改范围：**

- `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/service/impl/CaptchaServiceImpl.java`
- `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/service/ICaptchaService.java`、短信/邮件发送器、认证 Controller
- Redis Lua/`SET NX`、风控配置和字典

- [ ] 图形、短信、邮件验证码并发验证最多成功一次，错误答案按策略消费或锁定。
- [ ] 发送间隔使用原子锁，按 IP、设备、账号、租户和日配额限流。
- [ ] 增加失败次数和短时锁定，避免账号枚举和短信轰炸。
- [ ] 测试 Redis 异常时 fail-closed，不得无限发送或无限尝试。

### T2.4 分片上传与文件权限

**修改范围：**

- `forge-server/forge-framework/forge-starter-parent/forge-starter-file/src/main/java/com/mdframe/forge/starter/file/controller/FileController.java`
- `forge-server/forge-framework/forge-starter-parent/forge-starter-file/src/main/java/com/mdframe/forge/starter/file/core/FileManager.java`
- `forge-server/forge-framework/forge-starter-parent/forge-starter-file/src/main/java/com/mdframe/forge/starter/file/storage/impl/LocalFileStorage.java`、Rustfs/Tencent COS 实现
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysFileMetadataServiceImpl.java`、`SystemFileMetadataPersistence.java`
- 上传会话 DTO、Redis/对象存储 session、Flyway（如需表）

- [ ] 增加上传初始化合同：用户、租户、业务主体、总大小、分片数、TTL、私有属性和存储类型签名。
- [ ] 每片和合并阶段校验大小、连续分片、ETag、MIME、扩展名和配额；超时/异常清理临时数据。
- [ ] 集群节点之间可继续上传；uploadId 不能脱离绑定主体使用。
- [ ] 分片完成继承 `isPrivate`，默认私有；所有下载、URL、Base64、字节读取和内部消费统一调用授权函数。
- [ ] 修复 `removeBatch` 按字符串 `fileId` 查询；补充租户、私有文件、状态和过期过滤。
- [ ] 明确 `status` 逻辑删除语义，实体、Mapper、查询和迁移保持一致。

## Phase 3：数据正确性和输入校验

### T3.1 数据集分页和总数

**修改范围：**

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataQueryExecutor.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/support/DbDialect.java`、`MySqlDialect.java`、其他 dialect
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/test/java/com/mdframe/forge/plugin/data/service/DataQueryExecutorTest.java`

- [ ] 先写 page 1/page 2 和 pageSize 上限测试。
- [ ] 为 dialect 增加 offset + limit API；按 `pageNum` 计算 offset，拒绝溢出和负数。
- [ ] 增加独立 count 查询或明确响应字段为 `pageTotal`；不能把当前页数量伪装成总数。
- [ ] 验证缓存键包含 pageNum/pageSize，避免不同页命中同一缓存。

### T3.2 SQL AST、元数据参数化和标识符校验

**修改范围：**

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/support/MySqlDialect.java`、`DbDialect.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/support/SqlSafetyValidator.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/support/TableQueryBuilder.java`、`DataQueryExecutor.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/controller/DataConnectionController.java` 元数据查询

- [ ] 新增注释、大小写、换行、多语句、锁、延时函数、文件函数、系统表、用户变量和 UNION 变体测试。
- [ ] 使用 SQL parser/AST 或受限数据库账号；只允许单条 SELECT。
- [ ] 所有 schema/keyword/table/column 参数改为 PreparedStatement 或严格标识符白名单。
- [ ] `quoteIdentifier` 正确拒绝反引号、控制字符、分号和注释，不依赖简单包裹。

### T3.3 注册、密码策略和找回密码边界

**修改范围：**

- `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/controller/AuthController.java`、`RegisterRequest.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemAuthServiceImpl.java`
- `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/config/SecurityConfig.java` 的 `PasswordPolicyConfig`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml`、密码历史/版本模型

- [ ] 校验确认密码、用户名长度/格式、手机号/邮箱格式与唯一性、租户状态和注册开关。
- [ ] 引入统一 `PasswordPolicyService`，覆盖注册、改密、找回、管理员重置、第三方建用户。
- [ ] 注册租户使用可信上下文、默认租户或邀请码，不接受任意客户端 tenantId。
- [ ] 找回密码查询过滤 `user_status=ENABLED`、`del_flag=0` 和目标租户；更新条件带状态/租户。
- [ ] 测试禁用用户、跨租户账号、弱密码、历史密码、过期密码和重放验证码。

## Phase 4：依赖、测试和前端结构

### T4.1 依赖和构建质量门禁

**修改范围：**

- `forge-server/pom.xml`、`forge-dependencies/pom.xml`
- JustAuth 版本和 exclusions
- CI workflow、Maven Toolchain、依赖扫描配置

- [ ] 运行 `mvn dependency:tree`，确认 `fastjson:1.2.83` 是否进入运行时。
- [ ] 升级兼容版本或排除旧依赖，执行 API/社会化登录回归测试。
- [ ] CI 固定 JDK 17，执行 compile/test、SCA、Secret scan、SAST、SBOM 和镜像扫描。
- [ ] `pnpm audit` 工具异常需记录并替换可用审计工具，不能以命令异常作为无漏洞结论。

### T4.2 巨型前端组件拆分

**修改范围：**

- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `forge-admin-ui/src/views/data/dataset.vue`
- `forge-admin-ui/src/components/AiCrudPage.vue`
- `forge-admin-ui/src/views/flow/design.vue`
- `forge-report-ui` 中超过 800 行的设计器/图表组件
- 对应 Pinia stores、composables 和组件测试

- [ ] 先建立行为快照和权限矩阵，确保拆分不改变路由、API、字典和保存协议。
- [ ] 按面板/业务域拆分 SFC，跨层共享状态迁移到 Pinia，单个 SFC 控制在 800 行以内。
- [ ] 为每个拆分面板增加状态初始化、取消、保存失败回滚和租户切换测试。
- [ ] 执行 ESLint、Vitest、生产构建和窄屏/明暗主题浏览器检查。

### T4.3 后端巨型类拆分与复杂度门禁

**重点范围：**

- `BusinessFlowService`（约 7771 行）、`DynamicCrudService`（约 5995 行）、`BusinessObjectDesignerService`（约 4256 行）、`LowcodeRuntimeConfigBuilder`（约 3132 行）
- `DynamicCrudRepository`（约 2195 行）、`LowcodePublishService`/`LowcodeDdlService`（约 999/938 行）、`BusinessProcessOrchestrator`/`BusinessProcessService`（约 1073/974 行）、`BusinessActionExecutionService`/`BusinessTriggerExecutor`（约 999/939 行）
- `FlowTaskServiceImpl`（约 3516 行）、`FlowModelServiceImpl`（约 1243 行）、`FlowMonitorServiceImpl`（约 1102 行）、`FlowTaskEventListener`（约 1069 行）、`FlowTaskNotifyListener`（约 1101 行）

- [ ] 先统计目标类行数、方法数、依赖数、圈复杂度、事务方法、远程调用和事件副作用，建立权限/租户/状态/幂等行为快照。
- [ ] 按流程启动/回调/状态同步、动态查询/写入/主子表/字段安全、Schema 构建、任务镜像/通知/监控等子域拆分；每个类目标不超过 800 行。
- [ ] 拆分期间保持 REST 协议、Flowable 状态码、租户条件、字段权限、逻辑删除、审计、缓存键和幂等协议不变；禁止通过无意义包装类规避门禁。
- [ ] 为子表跨租户、跨主记录、逻辑删除行和并发修改增加回归测试；确认 `DynamicCrudRepository` 的隐式租户/逻辑删除条件后，再决定是否需要显式 Mapper 条件。
- [ ] 增加 ArchUnit/PMD/CPD 或等价复杂度门禁，运行后端编译、目标模块测试、权限矩阵和租户隔离测试。

### T4.4 低代码事件和运行态状态机

- [ ] 去除 `BusinessProcessOrchestrator`、`BusinessEventPublisher`、`BusinessTriggerExecutor` 在缺失可信租户时默认 `1L` 的行为；事件进入隔离失败队列或返回明确错误。
- [ ] 将未知事件条件操作符从 fail-open 改为 fail-closed；发布前校验操作符、字段类型、版本和表达式长度。
- [ ] 事件增加来源、版本、签名/可信上下文和幂等键；伪造其他 tenantId、重复事件、乱序事件均有测试。
- [ ] 为 `BusinessProcessOrchestrator` 增加 run lease/heartbeat/fencing token；节点 attempt claim 和 complete 均按 attemptId 原子更新并检查 claim 结果。
- [ ] 为远程 FlowClient 启动、审批、回调和状态同步增加 Outbox、重试、补偿、超时接管和人工恢复记录。
- [ ] 测试并发启动、并发执行、重复/乱序回调、不同租户回调、超时恢复和远程成功本地失败场景。

### T4.5 低代码发布任务、DDL 与后置同步

- [ ] 将在线 DDL 从本地业务事务中拆出，增加发布任务/Outbox，记录 requestId、schemaHash、数据源、租户、版本、操作者和结果。
- [ ] 发布状态至少覆盖预检、DDL 待执行/执行中/成功/失败、配置待同步/成功/失败和人工重试；不以 `@Transactional` 声明提供跨数据源原子性。
- [ ] 菜单、应用入口和运行配置同步使用幂等键；增加指数退避、死信、对账和人工重放。
- [ ] 测试 DDL 成功后配置失败、菜单成功后入口失败、重复 post processor 事件、重试耗尽和补偿/回滚脚本。

### T4.6 流程监控、事件镜像与 BPMN 解析安全

- [ ] 所有 `FlowMonitorServiceImpl` 方法统一绑定 tenant context 和资源权限，删除/废弃未绑定租户的旧接口；流程变量按白名单和敏感级别脱敏并记录读取审计。
- [ ] 将 `FlowModelServiceImpl` 的 BPMN process key、sequenceFlow、结构和替换逻辑迁移到安全 DOM/StAX 解析器，复用关闭 DTD/外部实体的 `BpmnXmlUtils`。
- [ ] 将 `validateNoProcessData()` 从 Service 层 `LambdaQueryWrapper` 迁移到 Mapper XML，显式加入租户和逻辑删除条件。
- [ ] 为 Flowable 镜像、候选人、业务状态和通知事件增加唯一 event id、顺序/版本、幂等写入和补偿任务；避免 `fallbackExecution=true` 在无事务上下文直接发送不可回收通知。
- [ ] 禁止日志输出原始 BPMN、流程变量和完整通知 URL；Webhook 继续使用出站场景 allowlist/private-network policy。
- [ ] 增加 XXE/DOCTYPE、CDATA、单引号、命名空间、属性重排、多 process、嵌套节点、非法引用和事件乱序测试。

## Phase 5：收尾和上线门禁

### T5.1 安全回归与权限矩阵

- [ ] 运行 P0/P1 目标测试、JDK 17 后端 package、前端 build 和 Playwright 恶意输入测试。
- [ ] 使用普通用户、租户管理员、平台管理员、匿名请求、跨租户用户和 SERVICE 身份完成权限矩阵验证。
- [ ] 记录 MySQL/Redis 可用性、网关环境变量、Flyway history、默认 client/grant 和外部权限资源结果。

### T5.2 灰度、回滚和证据归档

- [ ] 为脚本隔离、权限 fail-closed、文件 session、SQL parser 和网关关闭分别写灰度开关和回滚步骤。
- [ ] 生产凭据、密码策略、MFA、权限资源、数据迁移和高风险网关动作完成负责人签字。
- [ ] 更新 `test-spec.md`、`execution-log.md` 和必要的 memory/pitfalls；失败项保留根因和阻断状态。
