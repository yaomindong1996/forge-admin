# 执行记录

## 2026-08-12 对象发布版本号唯一键冲突修复

- 用户现场错误：发布对象 `1910000000000001111` 时，`ai_business_object_design_version.uk_ai_business_design_version_no` 报重复键 `1-1910000000000001111-1`。
- 根因：预售种子已写入对象设计快照 `version_no=1`；首次人工发布生成 CRUD 发布 v1 后，发布服务又错误将 CRUD `publishedVersion` 写入对象设计 `versionNo`，造成第二次写入设计 v1。
- 修复：`BusinessObjectDesignVersionService` 始终按该对象 `MAX(version_no)+1` 生成设计历史版本；`BusinessObjectPublishService` 仅写关联 `publishVersion`；移除内部 DTO 的可误用 `versionNo` 入参。
- 定向测试：Java 17 在 generator 模块执行 `mvn -Penable-tests -Dtest=BusinessObjectDesignVersionServiceTest,PresaleRegistrationLowcodeMigrationContractTest -DfailIfNoTests=false test`，7 项通过，`BUILD SUCCESS`。
- 聚合编译：Java 17 在 `forge-server` 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`，32 个 Reactor 模块全部成功；仅保留既有 deprecated/unchecked/Lombok warning。
- 静态检查：`git diff --check` 通过。未启动服务或修改数据库，无待清理进程；真实发布验证需部署新后端后重新点击发布。

## 2026-08-12 app-server 缺少菜单适配器修复

- 用户启动 `forge-app-server` 失败：`BusinessSuiteService` 无法注入 `MenuRegisterAdapter`。
- 根因：真实 `MenuRegisterAdapterImpl` 位于 Admin 服务；app-server 扫描 generator 后也创建依赖该接口的 Service，但没有运行态菜单实现。
- 修复：新增 `forge-app-server/src/main/java/com/mdframe/forge/app/server/bridge/AppMenuRegisterAdapter.java`，提供无操作 Bean；App 服务不注册、更新或删除后台菜单，菜单管理仍由 Admin 服务完成。
- 验证：Java 17 执行 `mvn -pl forge-app-server -am compile -DskipTests`，应确认 app-server 及其依赖聚合编译成功；未在本轮启动真实服务。

## 2026-08-12 app-server AI 适配器装配修复

- 追加启动错误：`AiCrudConfigGenerateService` 无法注入 `AiClientAdapter`。
- 根因：AI 适配器真实实现位于 Admin 服务；app-server 扫描 generator 后创建 AI 相关 Service，但没有 AI 插件实现。
- 修复：新增 `forge-app-server/src/main/java/com/mdframe/forge/app/server/bridge/AppAiClientAdapter.java`，普通调用返回 fallback，流式调用失败关闭；不引入 AI 插件或外部模型访问。

## 2026-08-12 初始化与首轮定向验证

- 变更范围：预售低代码 Flyway 收口、字段间数值门禁、提交/提货/退货发布快照和应用交付元数据。
- 首次聚合编译未显式指定 Java 17，默认 JDK 不支持目标发行版 17，在 `forge-starter-job` 阶段失败；已确认机器存在 `/opt/homebrew/Cellar/openjdk@17/17.0.13`，后续命令固定使用该 JDK。
- generator 定向测试：
  - 命令：`env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Penable-tests -Dtest=BusinessActionCommandPolicyTest,AssertRecordActionStepExecutorTest,PresaleRegistrationLowcodeMigrationContractTest test`
  - 结果：3 个测试类、23 项通过，`BUILD SUCCESS`。
- 服务与环境：未启动服务、未连接数据库或 Redis，无需清理进程。

## 2026-08-12 收尾验证

- generator 定向回归：
  - 命令：`env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Penable-tests -Dtest=BusinessActionCommandPolicyTest,LowcodeRuntimeConfigBuilderTest,BusinessObjectPublishServiceCommandTest,BusinessObjectPublishServiceFieldEventTest,AssertRecordActionStepExecutorTest,AdjustNumberActionStepExecutorTest,TransitionStatusActionStepExecutorTest,DynamicCrudCommandRepositoryTest,PresaleRegistrationLowcodeMigrationContractTest test`
  - 结果：9 个测试类、44 项通过，`BUILD SUCCESS`。
- generator 聚合编译：
  - 命令：在 `forge-server` 执行 Java 17 的 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`。
  - 结果：32 个 Reactor 模块全部成功；只有既有 deprecated/unchecked/Lombok Builder warning。
- 前端定向回归：
  - 命令：Node 20.19.0 下执行扫码、`AiFormItem`、动态规则、表单协议、字段事件、动作协议和表单编译 8 个 Vitest 文件。
  - 结果：8 个测试文件、43 项通过。
- 前端 ESLint：相关低代码运行时、设计器和测试文件 0 error；保留 `AiForm.vue:162` 既有 1 条 prop warning。
- 前端生产构建：Node 20.19.0 下执行 `pnpm build`，结果 `✓ built in 1m 32s`；保留既有动态/静态导入、组件同名和 CSS 注释警告。
- Flyway/SQL 静态检查：
  - V1.0.105 为当前最高且唯一版本。
  - `${...}`、租户 0、URL、认证/凭据、任意 SQL/脚本配置键和 `logic_delete_active` 扫描均无命中。
  - 自定义只读词法检查确认引号/注释闭合、圆括号平衡，共 30 条 SQL 语句。
  - Flyway 合同测试覆盖运行表、模型/对象/关系、扫码、动态规则、查询事件、事务动作、发布快照、应用聚合和安全边界。
- 静态质量：`git diff --check` 通过；V1.0.105 和本变更新增文档 whitespace 检查无输出。
- 跳过项：按既有分工未执行真实 MySQL/Flyway、服务启动、真实企业微信/手机摄像头和四个外部查询源 E2E。
- 服务与环境：本轮未启动服务，无需清理 PID。

## 2026-08-12 MONEY 协议增量收口

- 变更范围：补齐通用 MONEY 运行时元/分转换；`money`/`integer` 直接 CRUD 表单数字控件识别；预售现金字段补充 MONEY 协议元数据。
- generator 定向回归：执行 Java 17 `mvn -Penable-tests -Dtest=BusinessActionCommandPolicyTest,LowcodeRuntimeConfigBuilderTest,BusinessObjectPublishServiceCommandTest,BusinessObjectPublishServiceFieldEventTest,AssertRecordActionStepExecutorTest,AdjustNumberActionStepExecutorTest,TransitionStatusActionStepExecutorTest,DynamicCrudCommandRepositoryTest,DynamicCrudMoneyValueTest,PresaleRegistrationLowcodeMigrationContractTest test`，10 个测试类、49 项通过，`BUILD SUCCESS`。
- 前端增量回归：Node 20.19.0 下执行 `pnpm vitest run src/components/ai-form/__tests__/field-type-utils.spec.js src/components/ai-form/__tests__/AiFormItem.spec.js src/components/lowcode-builder/shared/__tests__/runtime-rules.spec.js src/views/app-center/components/designer/form-first/__tests__/formDesignerSchema.spec.js src/views/app-center/components/designer/__tests__/business-form-runtime-compile.spec.js`，5 个测试文件、30 项通过。
- 前端执行 `pnpm exec eslint src/components/ai-form/field-type-utils.js src/components/ai-form/__tests__/field-type-utils.spec.js` 无错误；执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，结果 `✓ built in 1m 35s`，仅保留既有动态导入、组件同名和 CSS 注释告警。
- Java 17 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`，32 个 Reactor 模块全部成功。
- 最终静态检查：`git diff --check`、新增文档/SQL/测试文件行尾空白扫描均无输出；V1.0.105 无 Flyway 占位符、租户 0、URL、认证凭据或 `logic_delete_active`。
- 未启动服务、未连接真实数据库、未执行企业微信设备和外部查询源 E2E；无待清理 PID。

## 2026-08-12 Flyway 1054 修复

- 用户真实迁移在 V1.0.105 第 159 行失败：`Unknown column 'seed.model_schema' in 'field list'`。
- 根因：派生表首个 SELECT 的模型 JSON 和业务对象描述缺少外层引用所需别名；已补充 `model_schema`、`description`。
- 验证：Java 17 执行 `mvn -Penable-tests -Dtest=PresaleRegistrationLowcodeMigrationContractTest test`，5 项通过；`git diff --check` 通过。
- 只读结构对比：V1.0.105 所有 INSERT 目标列均存在于 `forge-server/db/全量初始化SQL.sql` 对应表结构中。
- 未连接或修改用户数据库；若 `forge_schema_history` 已留下 1.0.105 失败记录，需先 Flyway repair 再重跑。

## 2026-08-12 H5 运行时与 Schema 修复增量

- 新增通用 H5 低代码运行页：`forge-h5-ui/src/pages/lowcode-runtime.vue`，支持发布配置、列表、表单、详情、主子表、动态显隐、字段查询事件、动作和扫码。
- 新增 `V1.0.106__fix_presale_lowcode_runtime_schema.sql`：修复预售模型 `appType`、主模型 `children`，并同步 `ai_crud_config`、对象发布快照和 CRUD 发布版本快照；不修改已执行的 V1.0.105。
- app-server 引入 `forge-plugin-generator`，使 H5 的 `/ai/crud-config/render`、动态 CRUD、查询源和业务动作接口由同一应用服务承载。
- 预售迁移合同测试：Java 17 `PresaleRegistrationLowcodeMigrationContractTest`，6 项通过。
- app-server 聚合编译：Java 17 `mvn -pl forge-app-server -am compile -DskipTests`，35 个 Reactor 模块全部成功。
- H5 构建：Node 20.19.0 执行 `pnpm build:h5`，构建成功。
- 未启动服务、未连接真实数据库、未执行企微 SDK/摄像头/外部查询源 E2E；部署环境需先执行 Flyway repair（若历史失败记录已写入）后再运行 V1.0.106。

## 2026-08-12 移动低代码运行时收尾

- 协议修正：V1.0.106 将预售业务域默认类型和主模型修正为 `MASTER_DETAIL`，两个明细模型修正为 `SINGLE`；主模型 `children` 修正为空模型对象数组，并覆盖 CRUD 配置、业务对象版本和 CRUD 历史发布版本。
- 协议校验补齐：两个明细模型的 `presaleOrderId` 原先错误标记为 `systemField`，V1.0.106 将其改为隐藏的只读业务字段，并同步修复配置/发布版本快照。
- 运行依赖：app-server 增加 `forge-flow-client` 直接依赖。执行 `mvn -pl forge-app-server dependency:tree -Dincludes=com.mdframe.forge:forge-flow-client -DskipTests`，确认 `forge-flow-client:1.0.0:compile` 在运行类路径中。
- 后端验证：Java 17 执行 `mvn -pl forge-app-server -am compile -DskipTests`，35 个 Reactor 模块全部成功；执行 `PresaleRegistrationLowcodeMigrationContractTest,BusinessAppOpenServiceTest`，7 项通过。
- H5 验证：Node 20.19.0 执行 `node --test src/utils/__tests__/lowcode-runtime.test.js`，3 项通过；两次增量执行 `pnpm build:h5` 均 `DONE Build complete`。
- 扫码兜底：企微、uni 和浏览器摄像头扫码保持不变；失败时不再误发 `SCAN_COMPLETE` 查询，提示手工输入，输入法确认键可触发受管商品查询。
- 静态检查：V1.0.105/V1.0.106 无 Flyway `${...}`、租户 0 和 `logic_delete_active`；`git diff --check` 通过。
- 跳过项：未启动真实服务、未连接数据库、未执行企业微信 JS-SDK、手机摄像头及外部查询源 E2E；无待清理进程。

## 2026-08-12 字段事件和明细校验增量收口

- 变更范围：H5 通用运行时补齐字段事件协议行为；保存前校验已渲染主表/明细行必填字段；扩展查询源请求支持取消信号。
- H5 Node 合同测试：Node 20.19.0 执行 `node --test src/utils/__tests__/lowcode-runtime.test.js`，5 项通过。
- H5 ESLint：执行 `pnpm exec eslint src/pages/lowcode-runtime.vue src/components/lowcode/LowcodeForm.vue src/components/lowcode/LowcodeField.vue src/utils/lowcode-runtime.js src/utils/barcode-scanner.js src/utils/__tests__/lowcode-runtime.test.js src/api/index.js src/components/AiField.vue`；当前 H5 工程未安装 eslint binary，返回 `Command "eslint" not found`，未将其误记为通过。
- H5 生产构建：执行 `pnpm build:h5`，`DONE Build complete`。
- 后端合同测试：Java 17 执行 `mvn -Penable-tests -Dtest=PresaleRegistrationLowcodeMigrationContractTest,BusinessAppOpenServiceTest -DfailIfNoTests=false test`，7 项通过。
- app-server 聚合编译：Java 17 执行 `mvn -pl forge-app-server -am compile -DskipTests`，退出码 0。
- 静态检查：V1.0.105/V1.0.106 无 Flyway 占位符、租户 0、URL、认证凭据或 `logic_delete_active`；`git diff --check` 通过。
- 未启动服务、未连接真实数据库、未执行企微 SDK/手机摄像头及四个外部查询源 E2E；工作区原有 Admin 服务 PID 未由本轮启动或停止。

## 2026-08-12 受管 Mock 查询源与外围接口管理

- 变更范围：新增 `V1.0.107__add_external_api_mock_query_sources.sql`，在外围接口管理增加 `HTTP` / `MOCK` 执行模式和 Mock 响应 JSON；预置并启用预售的 `wecom/user-store`、`member/member-by-mobile`、`product/product-by-barcode`、`payment/static-code` 四个低代码查询源。
- Mock 安全边界：Mock 仅解析管理员保存的 JSON，继续执行接口启用状态、权限校验、调用日志、受控响应提取和转换；不读取外部系统运行配置，不执行出站 HTTP、脚本或 SQL。
- 后端定向测试：Java 17 在 `forge-server` 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-external -am -Penable-tests -Dtest=ExternalProxyServiceImplTest,ExternalQuerySourceServiceImplTest,ExternalQueryContractValidatorTest,LowcodeQuerySourceMigrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test`，9 项通过，`BUILD SUCCESS`。覆盖 Mock 返回配置 JSON、未读取外部系统/未调用出站客户端、调试响应状态、低代码输入契约和 V1.0.107 内容。
- 后端聚合编译：Java 17 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-external,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`，32 个 Reactor 模块成功；只有既有 deprecated/unchecked/Lombok warning。
- 前端校验：Node 20.19.0 下执行 `pnpm exec eslint src/views/external/manage.vue src/api/external/api.ts`，ESLint 当前 flat 配置未匹配 `.ts` 文件，仅返回 “File ignored because no matching configuration was supplied” warning；执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 成功，最终复跑结果 `✓ built in 1m 57s`，保留既有动态/静态导入、组件同名和 CSS 注释 warning。
- 静态检查：`rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.107__add_external_api_mock_query_sources.sql` 无输出，`git diff --check` 通过。
- 跳过项：未执行真实 MySQL/Flyway、Admin/App 服务启动、真实 HTTP/数据库/脚本查询、企业微信或 H5 外部查询 E2E；本轮未启动服务，无需清理 PID。

## 2026-08-12 V1.0.107 seed.system_name 迁移修复

- 用户真实迁移失败：`Unknown column 'seed.system_name' in 'field list'`，位置为 `V1.0.107__add_external_api_mock_query_sources.sql` 第 89 行。
- 根因：`sys_external_system` 种子的派生表首个 `SELECT` 未给第三列声明 `system_name` 别名，外层 `SELECT seed.system_name` 在 MySQL 中无法解析。
- 修复：为首个 `SELECT` 补充显式别名：`'企业微信Mock' system_name`。
- 回归：Java 17 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-external -am -Penable-tests -Dtest=LowcodeQuerySourceMigrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test`，2 项通过，`BUILD SUCCESS`。
- 静态检查：`git diff --check` 通过；V1.0.107 无 Flyway `${...}` 占位符。
- 未连接或修改用户数据库；若 `forge_schema_history` 已记录 1.0.107 失败，需要先执行 Flyway repair 再重启。

## 2026-08-12 H5/设计态展示与查询源参数修复

- 用户反馈范围：
  - `/ai/lowcode/query-source/execute` 报 `查询参数[企微userid]类型不正确`。
  - 表单设计器点开“收款方式”看不到它控制静态码/现金金额的配置。
  - “静态码单号”“现金金额”在表单设计器中不可见。
  - H5 列表页移动样式不合理，筛选条件默认展开。
  - H5 填写态不应显示导购 userid 和状态，商品条码/操作日志需要中文标题。
- 修复：
  - `ExternalQueryContractValidator` 对 `string` 参数允许 `String/Number/Boolean` 标量并转字符串；对象/数组继续失败关闭。
  - 新增 `V1.0.108__fix_presale_mobile_form_visibility.sql`：同步修复 CRUD 当前配置、CRUD 发布版本、业务对象设计数据和对象设计发布快照；`salesUserId=fields[3]`、`status=fields[16]` 填写态隐藏，`staticPaymentNo=fields[12]`、`staticPaymentInfo=fields[13]`、`cashAmount=fields[14]` 模型态保持可见，运行态按 `payMethod.runtimeRules` 控制。
  - 管理端设计器给来源字段增加“当前字段影响”摘要，并让画布预览不执行运行态显隐规则，避免设计时目标字段被隐藏。
  - H5 运行页默认折叠筛选条件，子表标题优先使用中文 `tabTitle/relationName`，保存和详情加载按 `modelCode/relationKey/key` 同步明细别名。
  - `V1.0.107` Mock 系统 `base_url` 改为非 URL 占位 `mock-local`，避免 Mock 种子保存真实或伪真实接口地址。
- 验证：
  - H5 纯函数测试：Node 20.19.0 执行 `node --test src/utils/__tests__/lowcode-runtime.test.js`，8 项通过。
  - H5 构建：Node 20.19.0 执行 `pnpm build:h5`，`DONE Build complete`。
  - 查询源契约：Java 17 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-external -Penable-tests -Dtest=ExternalQueryContractValidatorTest -Dsurefire.failIfNoSpecifiedTests=false test`，4 项通过。
  - 外部查询源迁移合同：Java 17 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-external -Penable-tests -Dtest=LowcodeQuerySourceMigrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test`，2 项通过。
  - 预售迁移合同：Java 17 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Penable-tests -Dtest=PresaleRegistrationLowcodeMigrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test`，7 项通过。
  - 管理端设计器 ESLint：Node 20.19.0 执行 `pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`，0 error。
  - 管理端生产构建：Node 20.19.0 执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，`✓ built in 1m 54s`；保留既有动态/静态导入、组件同名和 CSS 注释 warning。
  - 静态检查：`git diff --check` 通过；V1.0.105/V1.0.106/V1.0.107/V1.0.108 无 Flyway `${...}` 占位符，无租户 0、URL、认证凭据、任意 SQL/脚本键或 `logic_delete_active` 命中。
- 已知非本轮阻断项：
  - 曾尝试执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -Penable-tests -Dtest=PresaleRegistrationLowcodeMigrationContractTest ... test`，被依赖模块 `forge-plugin-message` 的既有测试编译错误阻断：`MessageServiceImplTest` 构造器缺少 `ApplicationEventPublisher` 参数。该错误与本轮预售/H5/查询源修复无关，后续如需全依赖测试需单独修 message 模块测试。
- 跳过项：未执行真实 MySQL/Flyway、Admin/App 服务启动、真实企业微信 JS-SDK、手机摄像头和四个外围接口 E2E；本轮未启动服务，无需清理进程。
