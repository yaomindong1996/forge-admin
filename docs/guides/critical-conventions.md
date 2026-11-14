# 关键约定（Critical Conventions）

> 从根 `AGENTS.md` 渐进披露拆出。违反会导致编译失败、运行时异常或数据问题。
> 本文件是关键约定的权威正文；`AGENTS.md` 仅保留短指令并链接到此。

### 5.1 SQL 必须写在 Mapper XML 中

查询类 SQL **禁止**在 Service 层用 `LambdaQueryWrapper` 构建。必须写在 Mapper XML 中。
- **原因**：`DataScopeInterceptor` 按 `mapperMethod` 精确匹配改写 SQL；XML 更易审查和优化
- **例外**：仅单表 `selectById`、`insert`、`updateById`、`deleteById` 等 MyBatis-Plus 内置方法允许

### 5.2 租户 ID 规则

- 业务数据（字典、配置等）的 `tenant_id` **必须设为 `1`**（默认租户），**禁止设 `0`**
- `TenantLineInnerInterceptor` 自动追加 `WHERE tenant_id = 当前租户ID`，`0` 的数据对所有租户不可见
- `sys_resource`（菜单/权限）表不受租户拦截，`tenant_id` 设为 `1` 即可

### 5.3 分页参数命名

- 前端传 `pageNum` + `pageSize`
- 后端 Controller 必须用 `@RequestParam(defaultValue = "1") Integer pageNum`，不能用 `page`

### 5.4 AiCrudPage 占位符

- URL 占位符用 **冒号格式**：`:id`、`:dictId`
- **禁止**花括号格式：`{id}` — 组件只识别 `includes(':id')`

### 5.5 图片/文件渲染

- `imageUpload` 组件存储的是 **fileId**，不是 URL
- 表格列渲染图片用 **`AuthImage`** 组件（自动带 Token），禁止直接用 `NAvatar` src
- 获取下载链接用 `getFileUrl(fileId)` from `@/utils/file`

### 5.6 循环依赖禁止

- Service 之间**禁止互相注入**
- 跨 Service 协调逻辑上提到 Controller 层

### 5.7 字典禁止硬编码

- 下拉选项、状态标签 **必须使用字典组件**（`DictSelect` / `DictTag` / `useDict`）
- Schema 必须定义为 `computed`，确保字典异步加载后响应式更新
- 业务枚举和可配置枚举必须维护到 `sys_dict_type` + `sys_dict_data`，禁止在前端页面写死 `options` 或标签映射
- 新增内置字典必须通过 `forge-server/db/mysql/migration/` 的 Flyway 脚本写入，脚本需具备 `NOT EXISTS` 防重复保护，`tenant_id` 必须为 `1`
- 字典类型命名使用小写下划线，系统级字典建议使用 `sys_` 前缀；文件存储类型统一使用 `sys_file_storage_type`
- 前端读取字典时使用 `useDict('<dict_type>')`，表格回显优先使用 `DictTag`，下拉选项使用 `computed(() => dict.value.<dict_type> || [])`
- 字典数据的 `dict_value` 必须与后端枚举/存储策略/业务状态值保持一致，`dict_label` 只负责展示文案，`list_class` 负责标签样式

### 5.8 API 规范

- RESTful：`GET /page`(分页)、`GET /:id`(详情)、`POST /`(新增)、`PUT /`(修改)、`DELETE /:id`(删除)
- 统一返回：`RespInfo.success(data)` / `RespInfo.error(msg)`
- 敏感接口：`@ApiDecrypt` / `@ApiEncrypt`
- **请求体禁止 `Map`**：Controller 写接口必须用明确的 DTO/VO 接收 `@RequestBody`，禁止 `@RequestBody Map<String, Object>` 再 `params.get("xxx")`
- 正确：`public RespInfo<?> approve(@RequestBody FlowTaskApproveDTO dto)`
- 错误：`public RespInfo<?> approve(@RequestBody Map<String, Object> params)`
- 允许的例外只有：低代码动态记录体、流程变量字段 `variables`、真正动态键值的元数据；`taskId`、`comment`、`userId`、`action` 等固定字段必须是 DTO 属性，不能整份请求都用 Map
- 新增或修改写接口时，先建 DTO/VO，再写 Controller；禁止先用 Map 占位再回头补类型
- **写接口入参校验以 DTO 注解 + Controller `@Valid`/`@Validated` 为主**（Jakarta Validation：`@NotBlank`、`@NotNull`、`@Size`、`@Pattern`、分组等）
  - 正确：DTO 字段加 `@NotBlank(message = "…")`，Controller 写 `add(@Valid @RequestBody XxxDTO dto)`；由 `GlobalExceptionHandler` 统一转 400
  - 禁止：仅靠 Service 里手写 `if (StrUtil.isBlank(...)) throw new BusinessException` 做「字段非空/格式」类校验（易漏、与 HTTP 校验重复）
  - Service 只保留**业务规则**：跨字段约束、状态机、权限/租户、唯一性、敏感配置策略、内置数据不可改等
  - 非 HTTP 入口（定时任务、内部调用、消息消费）若复用同一 Service：要么走带 `@Valid` 的 Facade，要么在入口显式 `Validator.validate`，禁止假设「注解已生效」
  - 新增/编辑校验差异用 Validation Group（如 `Create`/`Update`），不要拆两套几乎相同的 DTO 只为必填不同
  - 存量模块按触及即改：本轮改到的写接口必须补齐注解与 `@Valid`，并删除与之重复的 Service 非空判断

### 5.9 业务状态必须用枚举

- Java 业务代码中的状态值**禁止魔法数字/字符串**（如 `setStatus(2)`、`getStatus() == 1`、`"terminated"`），必须使用枚举
- 通用启用/停用（`enabled`、`visible`、`userStatus`、`isEnabled` 及同类 0/1 开关）统一用 `com.mdframe.forge.starter.core.enums.EnableStatus`：写入走 `EnableStatus.ENABLED.getCode()`，比较走 `EnableStatus.ENABLED.matches(value)`
- 多状态业务字段必须建本模块枚举，提供 `getCode()` / `matches()`；正确示例：`entity.setStatus(FlowTaskStatus.APPROVED.getCode())`、`FlowTaskStatus.APPROVED.matches(entity.getStatus())`
- 实体字段可继续用 `Integer`/`String` 与数据库列对齐，禁止把实体字段改成枚举类型只为了少写 `getCode()`
- **Mapper XML / SQL 允许字面量**（`status = 0`、`status IN (2, 3, 7)`），禁止在 XML 里写 Java 类全名（`${@...Enum@XXX}`），避免改包路径后运行时才失败
- 前端展示走字典（`DictSelect` / `DictTag` / `useDict`），`dict_value` 必须与后端枚举码一致
- 不要套 `EnableStatus` 的字段：Boolean 开关、编码规则段配置、`readonly` / `isDefault` / `validationPassed` 等非启用语义；这些要么保持原类型，要么建专用枚举
- 测试可用字面量 0/1 作为存储码，生产代码不行

### 5.10 安全红线

- 禁止硬编码密钥、AK/SK、数据库密码
- 禁止提交包含用户个人信息的测试数据
- 禁止在日志中打印手机号、身份证、银行卡
- API Key/Secret 返回前端必须脱敏（保留前4后4，中间 `****`）
- 涉及资金/状态流转/权限变更，必须在 Spec 中标注并经人工审查
- 外部接口调用必须设置超时（默认 3s）并做降级；状态变更必须走状态机

### 5.11 数据库规范

- 所有业务表必须包含：`id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`
- 字符集 `utf8mb4`，引擎 `InnoDB`
- 金额字段用 `long`，单位**分**
- 时间字段用 `LocalDateTime`

### 5.12 逻辑删除规范

> Forge 项目默认优先逻辑删除，只有明确属于运行时中间表、关系重建表、框架表或留存清理任务的场景才允许物理删除。

#### 适用范围

- 用户可见的主数据、配置数据、设计态元数据、业务单据、可恢复/可审计数据，删除必须走逻辑删除。
- 新增业务主表、配置表、设计态元数据表默认增加 `del_flag` 字段；`0` 表示未删除，非 `0` 表示已删除。没有“删除后允许同业务键重建”约束的普通表可以继续使用 `0/1`。
- 历史表已经使用 `deleted`、`status=0` 等字段表达软删除时，可以保持现有语义，但必须在实体、Mapper XML 和查询条件中保持一致。
- Flowable 引擎原生表（如 `ACT_*`）和定时任务框架自身运行表不纳入 Forge 逻辑删除改造。
- `sys_job_config`、`sys_job_log` 属于 Forge 内部表，不因“定时任务”命名而排除；行级删除按逻辑删除规范处理。日志留存清理类专用任务可以按策略物理清理历史数据，但必须在代码和 Spec 中说明。

#### 实体与 SQL 规则

- 表有逻辑删除字段时，实体必须显式声明对应字段并加 `@TableLogic`；禁止只依赖 MyBatis-Plus 全局配置兜底。
- `@TableLogic` 字段类型必须匹配数据库字段类型：`tinyint` 用 `Integer`，`bigint` 用 `Long`，`char(1)`/`varchar` 用 `String`。
- 使用删除标记唯一索引的数值主键表，实体必须声明 `@TableLogic(value = "0", delval = "主键数据库列名")`，删除时由 MyBatis-Plus 原子写入当前行主键；自定义删除 SQL 也必须写主键，禁止固定写 `1`。
- Mapper XML 中的查询必须显式过滤未删除数据，例如 `AND del_flag = 0` 或 `AND deleted = 0`；不要假设自定义 XML 会被 MP 自动补全。
- 删除接口协议可以保持不变（例如 `DELETE /:id` 或既有 `POST /remove`），但 Service/Mapper 底层必须从物理 `DELETE` 改为逻辑删除。
- 批量删除必须批量更新逻辑删除字段，避免循环逐条物理删除。

#### 唯一键与迁移规则

- 逻辑删除会改变唯一键语义；存在业务唯一键的表必须评估“已删除记录阻塞重新创建”的问题。
- 不是所有逻辑删除表都需要删除标记唯一索引：无业务唯一键，或业务键要求跨历史永久唯一时，不要机械附加删除标记。
- 业务键只要求“未删除记录唯一”且删除后允许同值重建时，数值主键表使用 `del_flag BIGINT NOT NULL DEFAULT 0`，并建立普通唯一索引 `UNIQUE (tenant_id, 业务键..., del_flag)`；有效行使用 `0`，删除后写当前行主键。
- 禁止使用固定 `0/1` 配合上述唯一索引，否则同一业务键最多只能保留一条已删除历史；禁止新增可见 `logic_delete_active` 生成列或依赖函数/部分索引表达该语义。
- 字符串主键表如需相同语义，`del_flag` 使用可容纳主键的字符串类型，并通过专用 Mapper 执行 `SET del_flag = 主键列`；MyBatis-Plus 通用字符串逻辑删除会把列名当字面量，禁止调用。
- Flyway 迁移必须先扩展删除字段、回填存量删除行为主键墓碑，再替换唯一索引并改代码；脚本必须具备 `information_schema` 防重复保护。
- 只给实体加 `@TableLogic` 但数据库缺字段会导致运行时 SQL 报错，禁止这样提交。

#### 允许物理删除的场景

- Flowable/Quartz/SnailJob 等第三方框架自带运行表，按框架语义处理。
- 纯关联关系重建表、临时表、缓存表、短期会话表、导入暂存表，可以物理删除，但必须确认无恢复/审计要求。
- 日志归档和留存清理任务可以物理清理超期历史数据；普通行级删除仍按该表的逻辑删除规则执行。
- 任何新增物理删除点必须在 Spec 或任务说明中写明原因、影响范围和回滚方式。

### 5.13 数据库脚本维护规范

所有数据库结构和内置数据变更必须走统一脚本，不允许只改实体、Mapper 或本地数据库。

#### 目录约定

| 目录 | 用途 |
|------|------|
| `forge-server/db/mysql/migration/` | MySQL Flyway 增量（表结构、索引、系统资源等） |
| `forge-server/db/mysql/全量初始化SQL.sql` | MySQL 空库全量 |
| `forge-server/db/postgresql/migration/` | PostgreSQL Flyway 增量（与 MySQL 平行版本号） |
| `forge-server/db/postgresql/全量初始化SQL.sql` | PostgreSQL 空库全量 |

#### Flyway 命名规范

- 版本脚本统一命名：`V<版本号>__<lower_snake_case_description>.sql`
- 示例：`V1.0.2__add_dashboard_version_table.sql`
- `V1.0.0__baseline.sql` 是历史基线，新变更版本必须大于 `1.0.0`
- 版本号必须单调递增；同一版本号只能有一个脚本
- 已经执行到数据库并进入 `forge_schema_history` 的脚本禁止修改；需要修正时新增下一个版本脚本

#### 分支合并 / 社区同步后必须查重版本号

并行分支或 `forge:upstream` 同步后，两边可能各自新增同号迁移（例如两个 `V1.0.218__*.sql`），Flyway 启动会直接失败。

合并或同步**提交前**执行：

```bash
# 社区模板
ls forge-server/db/mysql/migration/V*.sql | sed 's/__.*//' | sort | uniq -d
# 改名工程（如 ForgePro）把路径换成 <prefix>-server/db/mysql/migration/
```

有输出即冲突，按下列顺序处理（细则见 `code-copilot/memory/pitfalls/db-flyway.md`）：

1. 查 `forge_schema_history`：已落地的版本与 description **保留文件名不动**
2. 冲突的另一方改名为当前目录**最大版本号 + 1**（如 `V1.0.224__…`），禁止改已执行脚本内容
3. 同步更新契约测试 / 文档里写死的旧文件名
4. 再跑一遍查重为空后才允许提交；同步 ForgePro 时同样检查产品侧 `db/mysql/migration`

#### SQL 编写规则

- 脚本必须可重复执行或具备防重复保护：`CREATE TABLE IF NOT EXISTS`、`INSERT ... SELECT ... WHERE NOT EXISTS`、新增列/索引前查 `information_schema`
- `INSERT` 必须显式写列名，禁止依赖表字段顺序
- 业务内置数据 `tenant_id` 必须为 `1`，禁止写 `0`
- `sys_resource`、`sys_role_resource` 等权限资源脚本必须做 `NOT EXISTS` 防重复
- 生产敏感数据、真实密码、Token、AK/SK、API Key 禁止提交到 SQL
- 涉及数据修复、状态流转、资金、权限放开的 SQL，必须在 Spec 中说明影响范围和回滚方式

#### 启动与验证

- `forge-admin-server` 启动时由 Flyway 执行 `forge-server/db/mysql/migration` 脚本；`forge-report-server` 单独启动不会执行这些迁移
- 默认读取 `classpath:db/mysql/migration`；Admin POM 用 `maven-resources-plugin` 的 `copy-resources`（勿写进默认 `<resources>`，否则 3.3+ 会拒绝对 `../db/mysql/migration` 的跨模块路径）把源 SQL 原样拷到 classes/JAR，不依赖启动目录
- `fail-on-missing-locations=true`；IDE 启动前刷新 Maven 并构建资源，禁止回退扫描服务器残留 SQL
- 若设置了 `FORGE_FLYWAY_LOCATIONS` 或 `FORGE_FLYWAY_ENABLED`，会覆盖默认配置；迁移未执行时优先检查这两个环境变量
- 验证迁移结果：

```sql
SELECT installed_rank, version, description, success
FROM forge_schema_history
ORDER BY installed_rank DESC;
```

### 5.14 前端 Vue 页面规范（规模 / 拆分 / Store / 注释）

> 本节为全局硬性约束，适用于所有 AI 生成的与修改的前端代码，违反即打回。细则见 `code-copilot/rules/coding-style.md` §7.4–7.6。

#### 单文件行数上限

| 档位 | 行数（template + script + style 合计） | 要求 |
|------|----------------------------------------|------|
| 常规 | **不超过 1000** | 新建 / 日常改动的目标上限；接近时先拆再加功能 |
| 特例 | **大于 1000 且不超过 2000** | 仅允许有充分理由（如编排壳、历史巨页渐进拆分中）；须在 PR/Spec 说明原因与后续拆分计划 |
| 禁止 | **超过 2000** | **禁止提交**，必须先重构拆分 |

- 新增功能禁止继续往已超限（或即将超限）的 SFC 里堆逻辑；发现所在文件已超限时，**先拆分再实现**。
- 编排入口（薄壳）可保留：真正逻辑进 composable / 子组件 / store，壳文件本身仍应尽量不超过 1000。

#### Pinia 强制使用（避免深度传参）

- **能用 Pinia 的场景必须用 Pinia**：跨组件、跨面板、跨层级的共享状态或通信（设计器 schema、选中 ID、面板 UI 状态、多组件联动的业务状态等），必须沉淀到 Pinia store（`src/stores/` 下按领域建子目录，如 `stores/designer/`）。
- **禁止纯 props/emit 层层透传**：同一个状态经 props + emit 转手超过 **2 层**即视为违规，必须改用 store；子组件直接读写 store，父组件不再充当数据中转站。
- 仅「展示用一次性 props」或「单层父子事件」可保留 props/emit；跨右侧属性栏、画布、工具栏等多面板联动一律走 store。
- 存量巨型组件改造遵循渐进式：入口接收 props 后 `syncFromProps` 同步进 store，并 watch store 对外 emit（兼容存量父组件）；内部子面板全部读写 store，逐步消灭中间 props 链。
- store 命名与文件：`useXxxStore` → `stores/<domain>/xxxStore.js`；一个 store 聚焦一个领域，禁止大杂烩 store。

#### 组件拆分与同类方法拆分（强制）

- **组件必须拆分**：复杂页面按 UI 区块拆独立子组件（工具栏 / 筛选区 / 表格区 / 抽屉或弹窗 / 属性面板的每个 collapse 或 tab-pane 一个文件）。
- **同类方法必须拆分**：按业务域抽 composable（如 `useXxxList.js`、`useXxxForm.js`、`useXxxPreview.js`），禁止在一个 setup 里堆互不相关的大段方法；纯函数下沉模块级 `utils.js` / `xxxUtils.js`。
- 拆分后目录：同域组件放同目录或子文件夹（如 `panels/FooPanel.vue`、`composables/useFoo.js`），禁止散落到无关目录。
- 禁止在一个 SFC 里同时堆积：多种组件类型的属性配置、多个业务域的状态、超长内联模板。发现即拆。

#### 注释与 template 布局清晰度（强制）

- **关键处注释必须明确**：非显而易见的业务规则、状态机、兼容历史数据、与后端协议约定、易踩坑分支，须用简短中文注释说明「为什么」；禁止无意义的废话注释，也禁止关键逻辑零注释。
- **template 布局分区注释**：顶层模板按区块用 HTML 注释标明结构，例如 `<!-- 工具栏 -->`、`<!-- 筛选区 -->`、`<!-- 表格/列表 -->`、`<!-- 抽屉/弹窗 -->`，让阅读者能一眼扫清页面骨架。
- 复杂 `v-if` / 权限 / 模式切换处，在模板旁补一行注释说明触发条件。
- script 内 composable / 大段逻辑按域用分节注释（如 `// —— 列表查询 ——`），与方法拆分一致。

### 5.15 Java 单类规模与职责边界

- Java 生产代码单个类（含内部类）原则上控制在 **1000 行以内**；新增类不得超过 1000 行。达到约 800 行时应先检查职责并规划拆分，不得继续把新功能堆进巨型类。
- 存量超过 1000 行的类按 Spec 分阶段收敛；本轮无法完成全量拆分时，必须在 `tasks.md` 标记剩余边界，禁止把“局部提取”宣称为整类达标。修改巨型类时不得使其继续增长，除非是保持兼容所需且在 Spec 中说明。
- 按稳定业务边界拆成 Facade/Orchestrator、策略、编译器、领域服务、仓储等组件；入口保留公开 API 和事务边界，不得仅为凑行数制造无职责的转发类或循环依赖。
- 每个拆分阶段必须有编译及相关契约/回归测试，特别核对租户、权限、状态机、回调幂等、运行时 JSON 与 SQL 协议。

### 5.16 代码形态上限与 AI 编码红线

> 短清单，细则见 `code-copilot/rules/coding-style.md` §9–§12。新增代码必须达标；修改存量代码时不得让指标继续恶化。

- **形态上限**：单方法 ≤ **80 行**（前后端一致），单行 ≤ **120** 字符，认知复杂度 ≤ **15**，参数 ≤ **5** 个（超过封装 DTO），嵌套 ≤ **3** 层。类与 SFC 行数仍以 5.14、5.15 为准。
- **注入与事务**：构造器注入（`@RequiredArgsConstructor` + `final`），新代码禁止字段 `@Autowired`；写事务用 `@Transactional(rollbackFor = Exception.class)`，禁止自调用期望事务生效，事务内禁止远程调用。
- **线程池**：新代码禁止 `Executors.newXxx`，用 Spring `ThreadPoolTaskExecutor`（有界队列、命名线程、传递租户上下文）。
- **注入防护**：Mapper XML 只用 `#{}`；`${}` 仅限经白名单校验的表名/列名/排序字段。
- **鉴权**：非公开接口必须有 `@SaCheckPermission` 等鉴权注解；禁止用前端传入的 `tenantId`/`userId` 做权限判断；禁止为跑通代码关闭鉴权、租户拦截或放开 CORS `*`。
- **依赖**：禁止引入未确认真实存在的依赖，优先复用 `forge-dependencies` BOM；新增依赖须在 Spec 说明。
- **AI 行为**：先搜索确认类/方法/组件存在再调用，禁止臆造 API；只改当前 Task 相关代码；禁止删测试、放宽断言、加 `eslint-disable`/`@SuppressWarnings` 来通过检查。

### 5.17 Git 提交说明必须用中文

- **标题和正文必须使用中文**（可保留模块名、类名、路径等必要英文专有名词）。
- 推荐格式：`[<变更名>] <中文简述>`，无变更名时用 `feat:` / `fix:` 等类型前缀加中文，例如 `fix: 登录页不再错误套用业务工作台布局`。
- **仅特殊情况可用英文**：上游 merge/revert 的原信息、第三方工具自动生成的提交、必须与外部仓库英文历史对齐时。人工编写的提交默认不允许纯英文。
- AI 助手创建 commit 时同样遵守本条，不得改用英文凑格式。

### 5.18 开源模板仓库禁止企业版代码

- 本条只约束 Forge 开源模板仓库；`forge:create` 生成的客户工程可以提交合法授权的插件源码。
- 企业版源码在独立私有仓库交付；开源模板不得包含企业包前缀（`com.mdframe.forge` + `.ee`，目录形式同理）
  或 `edition=ee` 的插件描述。规范与负例测试用分段写法，不以忽略文档/测试目录规避检查。
- 本地安装的后端 `forge-server/plugins/`、前端 `forge-admin-ui/src/views/plugins/` 不得进入模板 Git 索引；
  联调后的两个宿主 POM 插件标记必须恢复为空，不能用强制添加忽略文件绕过。
- 提交模板前执行 `pnpm check:edition`，检查工作区可提交文件及暂存原始内容；社区样例允许独立提交。
  不自动修改 Git 索引，不取代代码审查。生成器去除模板专用忽略区块，也不复制此检查工具和命令。

### 5.19 系统版本与发版规范（AI 必须执行）

- **每次代码交付前明确本轮是否正式发版**；涉及版本调整、打包交付或发布时，必须先完整读取
  [系统版本与发布维护](../../forge-docs/guide/release-version.md)，复用当前变更的 Spec、Tasks 和验证记录。
- **唯一发行版本源**是 `forge-server/pom.xml` 的 `revision`，从 1.2.0 建立记录基线。
  以后必须读取当前值，不得将本文示例当作永远的当前版本；禁止在页面硬编码版本或用数据库迁移号代替。
- **日常开发不自动升版**：用户可感知功能、修复和升级注意事项写入 `CHANGELOG.md` 的 `Unreleased`。
  只有明确进入发版流程且目标版本已确认时才升版；“继续优化”“提交代码”“push”不等于同意发布新版本。
- 正式版本按兼容性选择：兼容修复升补丁号、兼容新增能力升次版本、不兼容变更升主版本。
  用户未指定目标版本时先给建议并确认；不得重复使用已发布版本，禁止改写历史标签和发布产物。
- 升版后执行 `pnpm version:sync`、`pnpm version:check`，维护精确版本的 CHANGELOG 段落；
  生成工程额外核对 `forge.config.json`。插件、原生 App 和 Flyway 版本独立维护，禁止全仓库替换版本串。
- **发布前必须验证**：按变更风险跑增量测试，发布构建默认跳过测试但不替代测试证据；
  从同一干净发布提交构建交付范围内的各端，核对后端 build-info、前端 version.json 和“关于系统”。
  发布提交、Git 标识、构建时间必须真实；未知信息明确缺省，不得伪造、拿前端信息冒充后端或隐瞒跳过项。
- **分别确认操作授权**：实现、提交、推送、创建/推送标签、发布制品、部署及执行生产迁移不是同一个动作。
  只执行用户授权范围内的操作，不因“发版规范”自动操作生产；禁止用 repair、关闭校验或改旧 SQL 掩盖迁移问题。
- 收尾在 `execution-log.md` 记录当前/目标版本、CHANGELOG 归属、验证结果、跳过项与发布状态。
  没有真实证据只能报告“待提交 / 待发布 / 待部署”，不能把代码完成或构建通过称为上线成功。

---

