# 踩坑：后端框架 / Spring / Maven

> 从 `code-copilot/memory/pitfalls.md` 按主题拆出。新条目追加到本文件。共 33 条。

## Redisson 接口存在不代表社区版可以运行


**发现日期**：2026-08-23

Redisson 3.50 社区版的 `RedissonClient` 接口虽然公开了 `getLocalCachedMapCache`，实际 `Redisson` 实现会直接抛出“Redisson PRO version”异常。编译成功和接口单测无法证明该能力可用于社区版，使用它实现 MULTI 缓存会导致每次业务读取都失败穿透。

处理原则：Forge MULTI 受管缓存使用社区版可用的 Caffeine L1、`RMapCache` L2 和类型化 Topic 失效通知组合；Topic 初次订阅或重连订阅时清空 L1。引入 Redisson 高阶数据结构前必须检查当前依赖 JAR 的实际实现或做真实运行测试，不能只依据 API 接口是否存在。

## 受管 JSON 缓存不能只恢复容器外层类型


**发现日期**：2026-08-18

`ManagedCacheValue.value` 即使通过 `@JsonTypeInfo` 保留了外层具体类型，`List<BusinessEntity>` 经过 Redis JSON 往返后仍可能变为 `List<LinkedHashMap>`。Java 泛型擦除使方法返回赋值阶段不报错，直到增强 `for`、`list.get()` 或下游字段访问发生隐式强转才抛 `ClassCastException`。

处理原则：受管缓存命中边界必须使用被缓存方法的 `getGenericReturnType()` 和应用 `ObjectMapper` 恢复集合、Map、数组的元素类型；类型恢复失败应删除异常 entry 并穿透业务方法。关键消费边界可兼容历史 Map 缓存，但不能把“清 Redis”作为永久修复，也不能只依赖外层 `@JsonTypeInfo` 推断泛型内容。

## 消息“批量接口”仍需检查数据库是否逐条更新


**发现日期**：2026-08-05

前端一次调用批量或“全部处理”接口，并不代表后端已经批量落库。若 Service 先查询目标记录，再循环调用 `updateById`，实际仍会产生 1 次查询 + N 次更新；数据量增加后，数据库往返和事务时间会线性增长。

处理原则：无逐条业务差异的状态更新应直接在 Mapper XML 中使用单条 `UPDATE`。SQL 必须显式限定可信 `tenant_id`、当前 `user_id` 和原状态；指定 ID 批量操作使用参数化 `IN`。Service 在空集合时直接返回，Mapper 仍增加空集合失败关闭条件，避免后续绕过 Service 调用时扩大更新范围。Service 单测应验证每个非空操作只调用一次批量 Mapper，空集合不访问 Mapper。

---

## 6. 本地文件存储返回相对访问地址导致图片渲染失败


**发现日期**: 2026-05-14

**问题描述**:
`/api/file/url/{fileId}` 在本地存储场景下可能返回 `/api/file/download/{fileId}` 这种相对路径。
如果前端直接把这个值塞给 `img src` 或头像组件，浏览器会去当前站点根路径取资源，导致图片不显示或加载失败。

**解决方案**:
前端统一通过 `resolveFileAccessUrl()` 归一化访问地址，必要时补上 `VITE_REQUEST_PREFIX`。
图片加载失败时，再调用 `removeCachedFileAccessUrl()` 清理旧缓存后重试。

**影响范围**:
- 所有使用文件访问地址渲染图片的前端组件
- 头像、favicon、素材预览、图片上传回显等场景

## N. 问题标题


**发现日期**: YYYY-MM-DD

**问题描述**:
简述遇到的问题和错误现象。

**错误示例**:
展示错误的代码或配置。

**正确用法**:
展示正确的代码或配置。

**根本原因**:
解释为什么会出错。

**解决方案**:
说明如何避免和修复。

**影响范围**:
说明哪些场景会受影响。
```

## 13. sys_file_metadata 不是标准业务审计表


**发现日期**: 2026-05-18

**问题描述**:
通用文件表 `sys_file_metadata` 的建表脚本只包含 `create_time`、`update_time`、`uploader_id`、`upload_time` 等文件元数据字段，没有 `create_by`、`create_dept`、`update_by` 这些标准业务表审计字段。

**解决方案**:
从 `sys_file_metadata` 做迁移脚本时，不要直接引用 `create_by` / `create_dept` / `update_by`。需要创建业务表审计字段时，优先用 `uploader_id` 映射创建人/更新人，用 `upload_time` 映射创建时间，`create_dept` 置空或按业务上下文单独补齐。

**影响范围**:
- 文件表迁移到业务表的 SQL
- 报表素材、通知附件等以文件 ID 关联业务数据的场景

## 16. 动态查询 SQL 注入检测误判字段名


**发现日期**: 2026-05-19

**问题描述**:
`DynamicQueryGenerator.containsSqlInjection()` 原正则直接匹配 `and` / `or` 等关键字子串，导致 `sort_order`、`order_no` 这类合法字段被误判为 SQL 注入字段，自定义查询和排序会静默跳过这些字段。

**解决方案**:
SQL 关键字匹配必须使用单词边界，只拦截独立关键字或危险字符：

```java
".*(?:\\b(?:insert|update|delete|drop|truncate|exec|execute|union|select|into|from|where|and|or)\\b|--|;|'|\"|\\\\).*"
```

**影响范围**:
- 动态 CRUD 搜索和排序
- 自定义查询字段选择、条件构造和排序

## 25. 后端 Maven 编译必须使用 JDK 17


**发现日期**: 2026-05-28

**问题描述**:
执行后端编译时，如果当前 shell 使用的不是 JDK 17，会在编译阶段失败：
```text
Fatal error compiling: 无效的目标发行版: 17
```

**解决方案**:
本机可显式指定 OpenJDK 17 后再执行 Maven：
```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-admin-server -am compile -DskipTests
```

**影响范围**:
- `forge-admin-server` 及其 Maven reactor 编译
- 所有 target/source 配置为 Java 17 的后端模块

## 57. 运行时异常返回给前端前必须去掉异常类名前缀，前端错误提示不能只剩一句 message


**发现日期**: 2026-06-10

**问题描述**:
登录或业务接口报错时，前端有时会直接收到 `com.mdframe.forge.starter.core.exception.BusinessException: 验证码错误或已过期` 这类字符串，既暴露了后端异常类名，也影响用户阅读。与此同时，前端全局错误提示只有一条 `message.error`，缺少请求路径、错误码、原始响应等排查信息，线上定位成本高。

**根本原因**:
部分业务代码会用 `RuntimeException` 包装 `BusinessException` 或其他异常，进入全局运行时异常处理后如果只拿 `getMessage()` 返回，就可能把 `xxxException:` 前缀一并透出。前端响应拦截器又只消费摘要 message，没有统一的“查看详情”入口，导致开发排查时只能靠控制台或后端日志。

**解决方案**:
后端 `GlobalExceptionHandler` 处理 `RuntimeException` 时，先递归解包 `BusinessException` cause，并对返回给前端的 message 做统一清洗，去掉 `com.xxx.BusinessException:`、`java.lang.RuntimeException:` 这类异常类名前缀。前端全局 HTTP 错误处理不要只弹一句 message，应该统一弹错误对话框，默认展示摘要文案，并提供“查看详情”展开区，至少包含错误码、请求方法、请求 URL、traceId 和服务端原始响应。

**影响范围**:
- `forge-server/.../GlobalExceptionHandler`
- `forge-admin-ui/src/utils/http/interceptors.js`
- `forge-admin-ui/src/utils/http/helpers.js`
- 所有依赖统一 axios 拦截器的页面，包括登录页

## 59. forge-create minimal-admin 保留 generator 时必须提供 AI 降级适配器并补齐依赖


**发现日期**: 2026-06-17

**问题描述**:
使用 `forge-create --preset minimal-admin` 生成最小后端工程后，启动可能报 `AiCrudConfigGenerateService required a bean of type AiClientAdapter that could not be found`。补上适配器后还可能在 `lingxi-plugin-generator` 编译阶段报缺少 `plugin-message`、`starter-job`、`starter-file`、`starter-excel`、`starter-id` 或 `flow-client` 相关类型。

**根本原因**:
`minimal-admin` 默认保留 `plugin-generator` 但不包含 `plugin-ai`。脚手架裁剪后删除了 admin-server 中原本依赖 `plugin-ai` 的 `AiClientAdapterImpl`，而 `AiCrudConfigGenerateService` 又强制构造注入 `AiClientAdapter`。同时 `scripts/forge-create/module-catalog.json` 中 `plugin-generator.dependencies` 没有覆盖其 POM 和源码直接引用的全部编译依赖，导致生成工程的 POM 被裁剪过头。

**解决方案**:
当选择了 `plugin-generator` 但未选择 `plugin-ai` 时，脚手架应生成一个不依赖 `plugin-ai` 的 `AiClientAdapterImpl` 降级实现：同步 AI 调用返回 fallback，流式调用返回 `Flux.empty()`。同时 `plugin-generator` 的模块目录依赖必须包含 `starter-datascope`、`starter-excel`、`starter-file`、`starter-job`、`starter-id`、`plugin-message` 和 `flow-client`，避免生成工程缺编译依赖。

**验证建议**:
修复 forge-create 裁剪逻辑后，必须重新生成临时 `minimal-admin` 工程，检查生成后的 `admin-server` 存在降级 `AiClientAdapterImpl` 且没有引用 `plugin-ai`，再执行 `mvn -pl <project>-admin-server -am compile -DskipTests` 或 `package -DskipTests`。

## 74. Spring Boot 3.5 与 Redisson 3.34.1 会触发登录 Redis 适配死循环


**发现日期**: 2026-06-24

**问题描述**:
`/auth/login` 登录链路使用 Redis/Redisson 时可能报 `StackOverflowError`，表现为请求进入 Redis 过期时间相关命令后死循环。

**根本原因**:
Forge 后端升级到 Spring Boot 3.5.x 后，Spring Data Redis 3.5 的 `RedisKeyCommands` 新增了 `pExpire(byte[], Expiration, ExpirationOptions)` 签名。Redisson 3.34.1 的 `redisson-spring-boot-starter` 仍解析到 `redisson-spring-data-33`，该适配层只覆盖旧接口签名，新接口 default 方法会与 Redisson 旧实现互相转发，最终栈溢出。

**解决方案**:
- Spring Boot 3.5.x / Spring Data Redis 3.5.x 必须使用解析到 `redisson-spring-data-35` 的 Redisson 版本。
- 本项目已将 `redisson.version` 从 `3.34.1` 升级到 `3.50.0`，依赖树应显示 `org.redisson:redisson-spring-data-35:3.50.0`。
- 排查类似 Redis 命令栈溢出时，优先执行 `mvn -pl forge-framework/forge-starter-parent/forge-starter-cache -am dependency:tree -Dincludes=org.redisson -DskipTests` 查看适配层是否匹配 Spring Data Redis 小版本。

**影响范围**:
- `/auth/login`
- `forge-starter-cache`
- 所有通过 Redisson/Spring Data Redis 执行 Redis 过期时间命令的链路

## 92. 根 POM 固定 skip 会让定向测试看起来通过但实际未执行


**发现日期**: 2026-07-03

**问题描述**:
执行 `mvn -Dtest=SomeTest test -DskipTests=false -Dmaven.test.skip=false` 时，如果根 POM 的 compiler/surefire 插件配置固定读取项目属性并默认 skip，Maven 日志可能显示构建成功，但实际出现 `Not compiling test sources`、`Tests are skipped`，新增测试没有运行。

**解决方案**:
- 先看 Maven 日志里是否有 `T E S T S` 和具体 `Tests run` 汇总，不能只看 `BUILD SUCCESS`。
- 为项目提供显式启用测试的 profile，例如 `-Penable-tests` 同时打开 testCompile 和 surefire。
- 如果启用测试后被历史坏测试阻断，先在 profile 中临时 exclude 无关旧测试，并在执行日志中说明原因，避免本变更的定向测试继续被跳过。

**影响范围**:
- `forge-server` 后端 Maven 定向单测。
- SDD `/test`、阶段收尾验证、Review 修复验证和归档前验收。

## 102. 超级管理员全量组织兜底不能当成真实绑定组织


**发现日期**: 2026-07-06

**问题描述**:
超级管理员只在 `sys_user_org` 显式绑定了一个组织，例如“内蒙古分公司”，但顶部当前组织下拉仍显示当前数据中心全部组织。

**根本原因**:
为兼容历史无组织绑定的超级管理员，登录态加载可能把当前数据中心全量组织写入 `LoginUser.orgIds` 作为兜底上下文。如果 `/system/org/current/options` 或 `/system/org/switch` 直接信任这个 `orgIds`，就无法区分“用户真实绑定了全量组织”和“历史超级管理员兜底填充了全量组织”。

**解决方案**:
组织切换选项和切换校验必须重新查询 `sys_user_org` 判断真实绑定关系：

- 有显式绑定时，只返回和允许切换绑定组织。
- 无任何显式绑定且当前用户是超级管理员时，才保留当前数据中心全量组织兜底。
- 相关查询写入 Mapper XML，避免在 Service 层新增复杂查询。

**影响范围**:
- `SysOrgServiceImpl#selectCurrentUserOrgOptions`
- `SysOrgServiceImpl#switchCurrentOrg`
- 当前组织切换器、登录态组织上下文和超级管理员历史账号兼容

## 109. Mapper XML 跨模块迁移后必须 clean install 清除旧资源


**发现日期**: 2026-07-12

**问题描述**:
Mapper XML 从一个 Maven 模块迁移到另一个模块后，即使旧模块源码已经删除，非 clean 构建仍可能保留旧模块 `target/classes/mapper` 中的 XML，并再次打包安装到 `~/.m2`。当项目使用 `classpath*:mapper/**/*Mapper.xml` 扫描时，新旧 JAR 会重复注册相同 namespace 的 `<sql id>`，启动报 `XML fragments parsed from previous mappers already contains value`。

**解决方案**:
- 先确认旧模块 `src/main/resources` 已不存在该 Mapper，避免用修改 namespace 掩盖重复资源；
- Mapper 跨模块移动后执行 Admin reactor 的 `mvn clean install -pl forge-admin-server -am -DskipTests`，同时清理各模块 `target` 并覆盖本地 Maven 仓库；
- 分别检查源码、`target/classes`、模块 JAR 和 `~/.m2` JAR，确保 Mapper 只有一个权威副本；
- 仅执行 `package` 不会覆盖 `~/.m2`，仅执行 `install` 而不 `clean` 可能继续把旧资源打包，两者都不足以闭环。

**影响范围**:
- 所有使用 `classpath*` 扫描 Mapper 的多模块应用；
- Mapper、Spring AutoConfiguration imports、配置文件等跨模块迁移场景；
- IDE 从单模块启动并读取本地 Maven 仓库依赖的开发环境。

## 110. 新增跨模块 API 后单模块测试可能解析到本地仓库旧构件


**发现日期**: 2026-07-12

**问题描述**:
在 A 模块新增公开类型或方法、B 模块立即依赖该 API 时，直接进入 B 模块执行 `mvn test` 可能从 `~/.m2` 读取旧版 A 构件，表现为源码中类型明明存在，但 B 的 `testCompile` 仍提示“找不到符号”。直接使用 `-am test` 又可能先被无关上游模块的全量测试基线失败截断，目标模块尚未执行。

**解决方案**:
- 先从 reactor 根执行 `mvn -pl <目标模块> -am install -DskipTests`，把当前工作区依赖安装到本地仓库；
- 再进入目标模块执行 `mvn -Penable-tests test`，隔离验证本轮测试；
- 上游全量测试若有既有失败，要记录失败类、数量和目标模块是否实际执行，不能把“上游截断”写成目标模块失败或通过；
- 最终仍从 reactor 根执行 Admin `package -DskipTests`，验证当前源码聚合装配。

**影响范围**:
- Forge 多模块中新增跨模块 DTO、record、SPI、AutoConfiguration 等 API；
- 本地仓库仍保留同版本旧 SNAPSHOT/扁平化构件的开发环境；
- 需要把“本轮专项测试”与“全量历史基线”分开取证的变更。

## 123. 远程异步启动的 5xx 和响应解析失败不是确定性失败


**发现日期**: 2026-07-21

**问题描述**:
远程流程启动已经到达服务端后，客户端可能收到 HTTP 5xx、传输中断或无法解析的响应。直接按失败重试启动会重复创建流程；把所有非 2xx 都视为同一确定性失败，也会漏掉服务端已成功但响应丢失的实例。

**解决方案**:
- 启动请求使用调用方生成的稳定 businessKey；
- HTTP 5xx、传输错误和响应解析错误统一视为状态未知，先按 businessKey 查询原实例；
- HTTP 4xx 与 HTTP 2xx 中的明确业务失败视为确定性失败，不发起恢复查询；
- 恢复查询失败时保留原始异常并失败关闭，不能再次发送启动请求。

**影响范围**:
- 所有具有服务端幂等键的远程流程、支付、消息投递和异步作业启动协议。

## 124. Spring Bean 有多个构造器时必须标明生产注入入口


**发现日期**: 2026-07-21

**问题描述**:
服务类同时保留生产构造器和包级测试构造器时，如果 `@Autowired` 误标在测试构造器，Spring 会把测试专用参数也当成必需 Bean。单元测试直接 `new` 服务不会覆盖容器选择过程，编译和业务测试都可能通过，但应用启动时才报缺 Bean。

**解决方案**:
- 多构造器 Spring Bean 显式把 `@Autowired` 标在生产构造器上，测试构造器保持包级且不加注解；
- 模块自有线程池由拥有其生命周期的服务创建和关闭，不为消除装配错误而注册无限定符的通用线程池 Bean；
- 增加最小 `AnnotationConfigApplicationContext` 装配测试，且刻意不注册测试专用依赖。

**影响范围**:
- 所有为测试注入 Clock、Executor、随机源或外部适配器而增加重载构造器的 Spring Service/Component。

## 129. 直接执行 Maven surefire:test 不会重新编译测试源码


**发现日期**: 2026-07-27

**问题描述**:
修改 JUnit 测试夹具后直接运行 `mvn surefire:test`，Surefire 可能继续加载 `target/test-classes` 中的旧 class，导致报错栈与修复前完全相同，容易误判为代码修复无效。

**解决方案**:
- 测试源码有改动时，先运行目标模块及依赖的 `test-compile`，确认测试类确实重新编译。
- 再用 `surefire:test -Dtest=...` 做快速定向执行，并同时核对 `Tests run` 非 0。
- 执行记录同时保留编译和测试两条证据，不把仅调用 Surefire 的旧产物结果当成最终结论。

**影响范围**:
- 所有使用 Maven Surefire 定向运行测试、且希望跳过完整生命周期以加速验证的模块。

## 139. 已验签手机号不等于可以用 LIMIT 1 任意映射用户


**发现日期**: 2026-08-01

**问题描述**:
外部 OIDC/JWT 首次身份映射复用了手机号登录查询，而该查询按优先级排序后 `LIMIT 1`。当租户内存在多个符合条件的同手机号用户且数据库没有唯一约束时，会把外部身份不确定地固化到其中一个账号。

**解决方案**:
- 外部身份使用专用 Mapper XML 查询，最多读取两条启用且未强制改密的候选。
- 只有候选数恰好为一时才建立 `issuer + sub` 映射；零条或多条均返回 `invalid_grant`。
- 普通目录校验失败属于认证失败；只有数据库/JWK 等基础设施异常返回 503，禁止把“用户不存在”误报为目录不可用。

**影响范围**:
- 所有使用手机号、邮箱等非数据库唯一字段建立外部稳定身份映射的认证链路。

## 143. Spring Service 增加测试便利构造器后必须显式选择注入构造器


**发现日期**: 2026-08-02

**问题描述**:
Spring 对只有一个构造器的组件可以隐式执行构造器注入；一旦为了单测增加第二个便利构造器，如果生产构造器未标记 `@Autowired`，容器可能回退寻找无参构造器并在启动时报 `No default constructor found`。直接 `new` Service 的普通单测无法发现此问题。

**解决方案**:
- 优先保持组件只有一个生产构造器，测试显式传入全部参数。
- 确需保留多个构造器时，在唯一生产构造器上显式添加 `@Autowired`，禁止为绕过异常添加会产生空依赖的无参构造器。
- 对带多个构造器的 Spring 组件补充 `ApplicationContextRunner`、`@Import` 或等价容器装配测试，断言 Context 无启动失败且目标 Bean 唯一存在。

**影响范围**:
- 所有使用构造器注入且为测试、兼容配置增加重载构造器的 Spring `@Service`、`@Component`、`@Configuration` Bean。

## 147. 带 `@Transactional` 的无接口 Bean 不能声明为 final


**发现日期**: 2026-08-02

**问题描述**:
通过 `@Bean` 注册的管理 Service 如果没有接口、类又声明为 `final`，Spring 无法使用 CGLIB 创建事务代理。方法上的 `@Transactional` 可能在上下文创建时失败，或无法建立预期事务边界；直接 `new` 的单元测试通常发现不了。

**解决方案**:
- 无接口且依赖类代理的 `@Transactional` Service 保持可继承，不声明 `final`。
- 只有明确使用接口/JDK 动态代理时才考虑 final 实现类，并确认事务注解位于代理可见方法上。
- 对自动配置注册的事务 Service 补充容器装配验证，不能只验证普通方法调用。

**影响范围**:
- 所有通过自动配置 `@Bean` 创建、没有业务接口且使用 `@Transactional`、`@Async`、缓存或其它 Spring AOP 的类。

## 148. MyBatis InnerInterceptor 创建期不能直接注入依赖 Mapper 的 Service


**发现日期**: 2026-08-03

**问题描述**:
MyBatis-Plus 创建全局 `InnerInterceptor` 集合时，如果某个拦截器直接构造注入依赖 Mapper 的业务 Service，会形成：

```text
MybatisPlusAutoConfiguration
-> InnerInterceptor
-> Service
-> Mapper
-> SqlSessionFactory
-> MybatisPlusAutoConfiguration
```

Spring 最终报 `Requested bean is currently in creation`。异常外层可能显示为定时任务注册器或任意最先触发 Mapper 的 Bean 创建失败，容易误判根因。

**解决方案**:
- 自动配置只向拦截器注入 `ObjectProvider<Service>` 或等价 `Supplier<Service>`，将依赖 Mapper 的 Service 延迟到 SQL 拦截运行期解析。
- 拦截器处理自身配置 Mapper 时，必须在解析延迟 Service 前直接跳过，防止运行时递归。
- 延迟依赖缺失或未就绪时明确失败关闭，不通过 `spring.main.allow-circular-references=true` 掩盖架构循环。
- 保留直接 Service 构造器时仅用于兼容现有显式构造场景，生产自动配置必须使用延迟构造器。

**影响范围**:
- MyBatis-Plus `InnerInterceptor`、MyBatis Plugin 和依赖 Mapper 的权限、审计、多租户等拦截器。
- Admin 启动阶段所有可能率先触发 Mapper 初始化的组件。

## 156. 幂等模板不能把受保护 action 的业务异常归类为基础设施故障


**发现日期**: 2026-08-03

**问题描述**:
幂等组件在 Redis 加锁后，如果继续用同一个宽泛 `catch (RuntimeException)` 包围快照读取、`action.get()` 和快照写入，Schema 校验、权限校验或业务拒绝也会被改写成“幂等服务暂不可用”。日志只剩二次包装异常，失败阶段与响应语义互相矛盾，调用方无法知道具体哪个字段错误。

**解决方案**:
- Redis 锁、快照读取和快照写入分别建立基础设施异常边界，记录安全 phase 和原始异常链；
- `action.get()` 不进入基础设施异常包装，业务异常原样向上交给协议层映射；
- 真实幂等基础设施异常仍失败关闭为 503，并保留 cause，禁止为了可用性绕过幂等保护；
- Schema 错误响应返回不包含业务值的具体原因和字段路径，日志和响应都不得记录 Idempotency-Key、Token 或完整请求 Body。

**影响范围**:
- 所有使用通用幂等模板保护业务 action 的开放 API、流程提交、业务写动作和后续系统服务写能力。

## 160. 画布 JSON 能解析不等于流程样例合法


**发现日期**: 2026-08-03

**问题描述**:
业务流程样例只做 `JSON.parse` 可以确认语法，却无法发现孤立节点、悬空边、环或无结束路径。例如定时提醒样例曾声明一个没有任何入边的失败结束节点，JSON 完全合法，但按发布门禁必须拒绝。若此类样例继续被标记为“合法基线”，后端校验器、前端画布和迁移器会产生互相冲突的预期。

**解决方案**:
- 冻结协议样例时同时执行真实强类型解析、节点注册表、DAG、从开始可达和到结束可达校验，不能只做 JSON 语法检查；
- 将手动、事件、定时等基线样例保存为测试资源，由真实发布校验器持续回归；
- 新增结果节点时必须同步增加合法来源出口；如果当前节点合同没有失败出口，不要预先放置不可达失败节点；
- 修正样例时同步更新测试规格和测试资源，执行日志明确记录原因，避免误判为协议兼容变更。

**影响范围**:
- 所有业务流程、自动化画布、迁移预览和可视化编排协议样例。

## 164. JSqlParser 4.9 会重排 ORDER BY ... FOR UPDATE 导致 SQL 语法错误


**发现日期**: 2026-08-05

**问题描述**:
Mapper XML 里写 `SELECT ... WHERE ... ORDER BY id ASC FOR UPDATE`（MySQL 语法正确），但执行时被重写成 `... WHERE ... FOR UPDATE ORDER BY id ASC`（非法），MySQL 报 `SQLSyntaxErrorException ... near 'ORDER BY id ASC'`。

**根因**:
项目实际引入 `jsqlparser:4.9`（经 `mybatis-plus-core:3.5.7` 传递）。`TenantLineInnerInterceptor` 等 MyBatis-Plus 拦截器会用 JSqlParser 解析 SQL 并重新 `toString()` 序列化。JSqlParser 4.9 序列化 `ORDER BY ... FOR UPDATE` 组合时存在 bug：把 `FOR UPDATE` 从句错误地移动到 `ORDER BY` 之前，生成 MySQL 无法解析的语句。改动 XML 的 `FOR UPDATE` 位置无效——运行时 SQL 始终来自 JSqlParser 的重排结果，重启也无法解决。

**解决方案**:
- 查询锁定语句避免 `ORDER BY ... FOR UPDATE` 组合。删除 `ORDER BY` 子句，使 SQL 以 `FOR UPDATE` 结尾（`SELECT ... WHERE ... FOR UPDATE`），JSqlParser 序列化结果保持不变；
- 锁行顺序对 `FOR UPDATE` 全行锁定场景无影响，删 `ORDER BY` 不会改变并发语义；
- 新增任何含 `FOR UPDATE` 的 Mapper 时，用与项目一致的 `jsqlparser:4.9` 验证序列化结果，而不是源码字面顺序。

**影响范围**:
- 所有带 `FOR UPDATE` + `ORDER BY` 的 Mapper SQL（本项目唯一受影响的是 `AiProviderMapper.selectIdsForDefaultSwitch`）。

## Pitfall: BaseMapper 子类无 saveBatch 能力


**记录日期**：2026-08-05

`DataBusinessDefinitionServiceImpl` 继承 `ServiceImpl<DataBusinessDefinitionMapper, DataBusinessDefinition>`，有 IService.saveBatch 能力，但只能用于 `DataBusinessDefinition` 实体。`DataBusinessDatasetMapper` 只 extends `BaseMapper<DataBusinessDataset>`（非 IService），无法调用 `saveBatch`。

**规避方式**：批量插入需要为 Mapper 对应实体创建 Service extends IService，或在 Mapper XML 中写批量 INSERT。循环 insert 保留属 P2 范围。

## Pitfall: LEFT JOIN 直接 GROUP BY 导致结果集膨胀


**记录日期**：2026-08-05

分页查询中每行执行相关子查询（COUNT/SUM）时，改为 `LEFT JOIN r ON r.session_id = s.id GROUP BY s.id` 虽然能消除子查询，但如果 JOIN 的表有多行匹配，结果集会膨胀，GROUP BY 需要处理更多行。

**推荐写法**：用派生表先聚合再 JOIN：
```sql
LEFT JOIN (
    SELECT session_id, COUNT(*) AS cnt, SUM(token_usage) AS total
    FROM ai_chat_record
    GROUP BY session_id
) r ON r.session_id = s.id
```
派生表先按 session_id 聚合为每 session 一行，JOIN 后结果集不膨胀，性能更优。

## Pitfall: FIND_IN_SET 替代 LIKE 匹配逗号分隔列表


**记录日期**：2026-08-05

候选人列表 `candidate_users` 存储逗号分隔的 userId（如 "1,23,456"），原来用 4 路 LIKE 匹配：
```sql
column = #{userId}
OR column LIKE CONCAT(#{userId}, ',%')
OR column LIKE CONCAT('%,', #{userId})
OR column LIKE CONCAT('%,', #{userId}, ',%')
```
4 路 LIKE 无法使用索引，全表扫描。

**替代方案**：`FIND_IN_SET(#{userId}, t.candidate_users)` 单条件替代，语义等价且更简洁。

## 168. 刷新树数据后必须重绑选中对象


**发现日期**：2026-08-08

树接口刷新通常会整体替换节点数组。只保留选中 ID、继续使用刷新前的节点对象，会让 `children` 等字段停留在旧快照，删除子节点后仍可能误判父节点含有下级。刷新完成后应按稳定 ID 在新树中查找节点并重绑选中对象；节点已不存在时清空选择。叶节点上下文还应显式返回空数组，不能用 `children || allRows` 将缺失子集回退为全量数据。

## 174. MySQL 派生表外层引用的每个计算列都必须显式起别名


**发现日期**：2026-08-12

`INSERT ... SELECT seed.xxx FROM (SELECT JSON_OBJECT(...)) seed` 中，派生表列名由第一个 SELECT 决定。若 JSON、字符串常量或表达式未声明 `xxx` 别名，目标表即使真实存在同名列，MySQL 仍会报 `Unknown column 'seed.xxx' in 'field list'`。修完第一个漏别名后，后续同类表达式还可能继续报错。

处理原则：所有派生表首个 SELECT 的计算列、常量列都显式声明稳定别名；UNION 后续分支沿用首个 SELECT 的列名。合同测试应断言外层使用的关键 `seed.xxx` 在首个 SELECT 中有对应别名，并对迁移所有 INSERT 目标列和当前全量表结构做静态对比。

## 178. Generator 新增强制适配器时必须检查所有聚合服务


**发现日期**：2026-08-16

`forge-app-server` 和独立 `forge-flow-server` 都会扫描 generator 插件。若 generator Service 新增构造器强制依赖，而只在 Admin 服务实现适配器，App 和 Flow 会在启动期同时报缺少 Bean；只验证 Admin 编译或启动发现不了这个问题。

处理原则：新增 generator 跨模块适配器时，必须检查 Admin、App、Flow 三个聚合服务。真实管理能力由 Admin 实现；App/Flow 只需完成运行态装配时，应提供服务内失败关闭桥接，误调用时明确拒绝，不能用空结果或无操作保存伪装成功。典型接口包括 `MenuRegisterAdapter`、`AiClientAdapter` 和 `ApplicationPermissionAdapter`。

## 181. Mockito 匹配重载方法时必须指定参数类型


**发现日期**：2026-08-20

同一依赖存在 `resolve(LowcodeModelSchema)`、`resolve(AiCrudConfig)` 等重载时，`when(resolver.resolve(any()))` 会在 Java 测试编译阶段报“引用不明确”，即使测试运行逻辑只会传其中一种类型。

处理原则：对重载方法使用带类型的匹配器，例如 `any(LowcodeModelSchema.class)`，或通过显式类型变量消除重载歧义。新增重载后应至少触发一次 `testCompile`，避免仅运行主代码 `compile` 漏掉测试源码问题。

## 分页复杂查询的 COUNT(*) 可能触发 JSqlParser 解析失败

**发现日期**: 2026-08-30

**问题描述**:
待办列表增加候选组关联和多层 `EXISTS` 后，分页请求报
`net.sf.jsqlparser.parser.ParseException: Encountered unexpected token: "("`，位置通常是
`SELECT COUNT(` 的左括号。原始列表 SQL 可以解析，但 MyBatis-Plus 分页自动生成的
`SELECT COUNT(*) ...` 在租户/数据权限拦截器再次解析时失败。

**解决方案**:
统一使用 `PaginationInnerInterceptor` 的兼容子类，将自动 count SQL 的首个
`COUNT(*)` 替换为语义等价的 `COUNT(1)`；保留原有 count 优化、租户隔离和数据权限改写，
并用包含嵌套候选组条件的 SQL 解析测试回归。新增复杂分页 SQL 时应同时验证原始查询和
分页 count 查询经过所有拦截器后的可解析性。

**影响范围**:
所有使用 MyBatis-Plus 分页且 SQL 含深层嵌套条件、函数或子查询的列表接口。

## Maven 父 POM 的 Surefire 分组过滤必须确保测试引擎存在

**发现日期**：2026-09-06

**问题描述**：
父 POM 对所有 reactor 模块配置 Surefire `groups` 或 `excludedGroups` 时，依赖-only 模块即使没有测试源码，也会在 provider 探测阶段报 `groups/excludedGroups require TestNG, JUnit48+ or JUnit 5`。

**根本原因**：
Surefire 的分组参数需要从测试 classpath 选择 JUnit 4、JUnit 5 或 TestNG provider；模块没有对应引擎时无法初始化，`skipTests` 或 `failIfNoTests` 不能绕过 provider 探测。

**解决方案**：
若仓库需要保留统一的 JUnit 5 标签过滤，在根 POM 以 test scope 继承 `junit-jupiter-engine`，并由 Spring Boot BOM 管理版本。定向测试使用 `-Dsurefire.failIfNoSpecifiedTests=false`，不要把无效的 `-DfailIfNoTests=false` 当作同一配置。

## Maven 编译器通用 skip 会覆盖 maven.test.skip

当根 POM 在 `maven-compiler-plugin` 的共享 `<configuration>` 中写入 `<skip>${forge.compiler.skip}</skip>` 时，`testCompile` 也会继承该值，导致即使设置 `maven.test.skip=true`，测试源码仍被编译并可能阻断发布打包。

发布构建需要主源码和测试源码分别配置：主编译使用 `skipMain=${forge.compiler.skip}`，测试编译使用 `skip=${maven.test.skip}`；默认将 `maven.test.skip` 设为 `true`，测试 profile 再显式设为 `false`。这样 `package/install` 不复制测试资源、不编译测试、不运行 Surefire，同时保留显式测试入口。
