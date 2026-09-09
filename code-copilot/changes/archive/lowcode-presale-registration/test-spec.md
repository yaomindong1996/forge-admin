# 门店预售登记增量测试计划

## 验证基线

- 低代码扫码与动态字段基础能力最近一次基线：前端 7 个文件、36 项 Vitest 通过，generator 7 个测试类、30 项通过，前端生产构建通过。
- 本轮差异：字段间数值比较、预售 V1.0.105 Flyway、提交/提货/退货发布快照、应用聚合和外部查询源占位。
- 本轮追加差异：V1.0.106 快照修复、独立 H5 通用运行页、app-server 低代码运行依赖和移动入口深链。

## P0

- 对象设计版本与 CRUD 发布版本独立：已有对象设计快照 v1 且首次 CRUD 发布为 v1 时，新增对象设计快照必须为 v2，关联 `publishVersion` 仍为 v1。
- app-server Spring 装配：引入 generator 后必须存在 `MenuRegisterAdapter` Bean，且运行服务的适配器不能写入后台菜单资源。
- app-server Spring 装配：引入 generator 后必须存在 `AiClientAdapter` Bean，运行服务误触发 AI 时必须失败关闭，不得产生模型调用。
- `ASSERT_RECORD.numericConstraints`：允许受控表单数值，拒绝非法操作符、保护字段和非数字常量。
- 预售迁移合同：三张表、三对象、两关系、扫码、动态规则、字段事件、当前子表选项、三类事务动作和不可变发布快照完整。
- 安全：迁移不包含 URL、Header、凭据、任意 SQL/脚本配置、Flyway `${...}`、租户 0 或 `logic_delete_active`。
- 前端回归：扫码、字段联动、子表事件、动作协议和业务表单编译基线继续通过。
- MONEY 协议：显式 `MONEY + 整数存储列` 按元输入、分存储、元回显；历史 `decimal` MONEY 字段保持兼容；金额超过配置小数位时拒绝静默舍入。

## P1

- generator 聚合编译使用 Java 17。
- H5 使用 Node 20 执行 `pnpm build:h5`。
- 前端相关文件 ESLint 和生产构建通过。
- SQL 进行括号/引号粗校验和关键元数据数量检查。

## 命令

```bash
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
  PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
  mvn -Penable-tests \
  -Dtest=BusinessActionCommandPolicyTest,AssertRecordActionStepExecutorTest,PresaleRegistrationLowcodeMigrationContractTest test
```

```bash
cd forge-server
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
  PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
  mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm vitest run <既有七个定向测试文件>
pnpm exec eslint <本轮相关前端文件>
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

```bash
rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.105__seed_presale_registration_lowcode_app.sql
git diff --check
```

## 跳过项

- 不执行真实 MySQL/Flyway、Admin/Flow 服务启动和接口 E2E。
- 不执行真实企业微信、手机摄像头和外部会员/商品/收款系统联调。

## 2026-08-12 移动运行时收尾验证

- V1.0.106 必须把业务域默认类型和主模型修正为 `MASTER_DETAIL`，两个明细模型修正为 `SINGLE`，并覆盖 CRUD/业务对象已发布快照。
- `BusinessAppOpenServiceTest` 验证 `MOBILE + RUNTIME` 返回 `H5` 打开类型，目标地址为 `/forge-h5/#/pages/lowcode-runtime` 且携带 `configKey`。
- H5 Node 合同测试覆盖字段动态显隐、字段事件参数/结果映射和企业微信扫码回调归一化。
- app-server 依赖树必须直接包含 `forge-flow-client`，防止扫描 generator 流程服务时发生运行时类缺失。

## 2026-08-12 金额协议增量验证

- generator 定向回归新增 `DynamicCrudMoneyValueTest`：5 项通过；合计 10 个测试类、49 项通过。
- 前端 `field-type-utils` 回归：`money`/`integer` 识别为数字字段，5 个测试文件、30 项通过。
- 预售模型合同：`cashAmount` 标记为 `businessFieldType=MONEY`、`precision=2`，页面输入为两位小数，底层 `cash_amount` 继续按 bigint 分存储。

## 2026-08-12 Flyway 派生表别名修复验证

- 复现根因：`ai_lowcode_model` 种子的首个派生表 SELECT 未给模型 JSON 声明 `model_schema` 别名，外层读取 `seed.model_schema` 报 1054。
- 同步修复业务对象种子缺失的 `description` 别名，避免迁移继续执行后出现第二个 1054。
- `PresaleRegistrationLowcodeMigrationContractTest` 5 项通过；全量初始化表结构对比确认 V1.0.105 的所有 INSERT 目标列均存在。

## 2026-08-12 验证结果

- generator 定向回归：9 个测试类、44 项通过。
- generator Java 17 聚合编译：32 个 Reactor 模块全部成功。
- 前端定向回归：8 个测试文件、43 项通过。
- 前端 ESLint：0 error；保留 `AiForm.vue` 既有 1 条 `vue/no-required-prop-with-default` warning。
- 前端生产构建：成功，保留既有动态/静态导入、组件同名和 CSS 注释警告。
- V1.0.105 静态扫描：无 Flyway 占位符、租户 0、URL、认证配置、任意 SQL/脚本键或 `logic_delete_active`；SQL 词法括号平衡，30 条语句。
- `git diff --check` 与新增文件 whitespace 检查无错误。

## 2026-08-12 H5 字段事件与明细校验增量验证

- H5 Node 合同测试：`node --test src/utils/__tests__/lowcode-runtime.test.js`，5 项通过，覆盖动态显隐、查询参数/结果映射、空值跳过与目标清理、扫码上下文白名单和企微扫码结果归一化。
- H5 ESLint：尝试执行 `pnpm exec eslint ...`，当前 H5 工程未安装 eslint binary，命令返回 `ERR_PNPM_RECURSIVE_EXEC_FIRST_FAIL Command "eslint" not found`；不阻断，已由生产构建覆盖 Vue/JS 编译。
- H5 生产构建：Node 20.19.0 下执行 `pnpm build:h5`，`DONE Build complete`。
- 后端修复迁移合同：Java 17 执行 `mvn -Penable-tests -Dtest=PresaleRegistrationLowcodeMigrationContractTest,BusinessAppOpenServiceTest -DfailIfNoTests=false test`，7 项通过。
- app-server 聚合编译：Java 17 执行 `mvn -pl forge-app-server -am compile -DskipTests`，退出码 0。
- Flyway 静态检查：V1.0.105/V1.0.106 无 `${...}` 占位符、租户 0、URL、认证凭据或逻辑删除生成列；`git diff --check` 通过。

## 2026-08-12 对象发布版本号冲突修复验证

- `BusinessObjectDesignVersionServiceTest` 模拟预售对象已存在设计快照 v1、首次 CRUD 发布为 v1 的状态，断言新对象设计快照按历史序列写入 v2，`publishVersion` 仍保留 v1。
- `PresaleRegistrationLowcodeMigrationContractTest` 同时回归预售种子和修复迁移，避免本次通用发布修复破坏既有发布快照契约。

## 2026-08-12 app-server 装配修复验证

- 新增 `AppMenuRegisterAdapter` 无操作适配器，供 app-server 扫描 generator Service 时完成依赖注入；Admin 服务的 `MenuRegisterAdapterImpl` 不受影响。
- 使用 Java 17 执行 app-server 聚合编译，确认新增适配器及 generator 依赖可编译；未启动服务或连接真实数据库。

## 2026-08-12 app-server AI 适配器装配修复验证

- 新增 `AppAiClientAdapter`，普通调用返回 fallback，流式调用返回不支持错误；该服务不引入 Admin AI 插件，也不会访问外部模型。

## 2026-08-12 外围接口 Mock 查询源增量验证

- `V1.0.107` 必须以幂等方式增加 `sys_external_api.execution_mode`、`mock_response_json` 和索引；不能出现 Flyway `${...}` 占位符或租户 0。
- 外围接口管理必须支持 `HTTP` / `MOCK`，Mock 模式必填合法响应 JSON；低代码查询源启用时必填合法输入/输出 Schema。
- Mock 调用必须保留接口状态、权限、调用日志和响应提取/转换语义，但不得读取外部系统运行配置或调用统一出站客户端。
- 四个预售 sourceKey 必须作为启用的低代码查询源预置：`wecom/user-store`、`member/member-by-mobile`、`product/product-by-barcode`、`payment/static-code`。
- 必跑：external 插件 9 项定向测试、external/generator 依赖聚合编译、管理端生产构建、SQL 静态扫描和 `git diff --check`。
- 跳过：真实 MySQL/Flyway、真实 HTTP/数据库/脚本查询、企业微信与 H5 的接口 E2E。

## 2026-08-12 预售 H5 与表单设计态展示增量验证

- 查询源契约：`string` 参数允许运行上下文传入的 `String/Number/Boolean` 标量并统一转字符串，拒绝对象/数组，避免企微 userid 因 Long 类型被误判。
- V1.0.108 修复预售主表展示快照：`salesUserId=fields[3]` 和 `status=fields[16]` 填写态隐藏；`staticPaymentNo=fields[12]`、`staticPaymentInfo=fields[13]`、`cashAmount=fields[14]` 模型态保持可见，运行态只由 `payMethod` 的 `runtimeRules` 控制。
- 收款方式字段保留 `props.__events` 兼容摘要，目标字段保留 `runtimeRules` 作为真实运行协议；设计器点开“收款方式”能看到它影响静态码和现金字段。
- H5 列表筛选默认折叠，移动端不直接展开查询条件；主表填写态不渲染导购 userid 和状态，明细标题优先使用中文 `tabTitle/relationName`。
- 表单设计器画布预览不套用运行态显隐规则，避免“静态码单号/现金金额”在设计时被隐藏；真实 H5/运行页仍按 `runtimeRules` 生效。
- V1.0.107 Mock 种子不保存真实 URL、认证、脚本或 SQL；Mock 系统 `base_url` 仅用非 URL 占位值，避免误认为已配置真实外围接口。
- 必跑：H5 纯函数测试、H5 生产构建、external 查询源契约测试、V1.0.107 合同测试、预售 V1.0.108 合同测试、管理端设计器 ESLint、管理端生产构建、SQL 静态扫描和 `git diff --check`。
- 跳过：真实 MySQL/Flyway、真实企业微信 JS-SDK、手机摄像头、Admin/App 服务启动和四个外围接口 E2E。
