# 执行日志 — 低代码应用门户产品化改造

## 2026-08-17 前置审计

- 范围：读取 `AGENTS.md`、项目记忆、Spec/Tasks、自动化测试标准；确认第 11 章技术决策，并采用第 13 章建议值。
- 现状：`forge-server/db/migration` 最新版本为 `V1.0.123`，Task 1 预定使用 `V1.0.124`。
- 现状：`BusinessApplicationRuntimeService` 已从不可变快照读取 `options.inAppBuilder` 并按权限过滤节点；`GridBlockRenderer` 已支持 AiCrudPage/AiForm/内容区块运行态复用。
- 现状：`ai_business_application.options` 为 JSON 扩展配置；本变更新增门户配置列并保留旧 options 兼容读取。
- 警告：真实 MySQL/Flyway、Admin/Flow 启动和 E2E 未执行，遵循用户环境偏好；后续必须由用户回填运行证据。

## 2026-08-17 至 2026-08-18 顺序实施

### P0 门户底座

- 新增 `V1.0.127__add_business_application_portal_config.sql`，扩展 `portal_slug`、`portal_config`、`ai_assistant_config`，初始化存量 slug/配置、逻辑删除唯一索引及隐藏路由权限资源。
- 后端支持 code/slug 解析、slug 合法性与唯一性校验、门户配置持久化、发布快照回放和当前用户页面权限过滤。
- 前端新增 `/app/:applicationCodeOrSlug`、`app-portal` 独立布局、门户导航/水印/空态/页面渲染；保留原设计态运行入口。

### P1 设置、发布和快照

- 新增应用设置页：基础、访问、导航、权限、全球化、高级配置。
- 新增应用发布页：就绪度、发布状态、访问链接/二维码、历史版本、回滚、AI 助理与分发配置。
- 发布与回滚快照包含门户和 AI 助理配置；发布前校验 slug、页面与助理绑定合法性。

### P2 创建和市场

- 新增智能、模板、Excel、空白四种创建方式；失败初始化保留应用草稿以便重试。
- Excel 只解析首 Sheet，读取表头和最多 50 行样例，生成对象、列表页、表单页草稿；不导入业务行、不自动建表。
- 应用中心新增“我创建的/我有权限的/最近使用”和“应用市场”；`CREATED` 使用可信会话用户 ID，`RECENT` 仍经租户/权限查询。
- 官方/推荐模板可立即启用或生成源码；组织私有模板因无持久化协议展示明确空态。

### P3 AI、分发和移动端

- 门户 AI 对话重新加载不可变发布快照，校验当前用户可见页面、已发布 `pageIds` 和能力；只传页面结构，不读取/写入真实业务行。
- `AiClientImpl` 移除请求正文、响应正文、系统提示词和上下文值日志，只保留长度、键数量及路由元数据。
- Forge/钉钉分发只保存配置；钉钉只接收受管连接器标识并写入 `PENDING_EXTERNAL_SYNC`，不接收 AppKey/AppSecret。
- 门户新增移动导航、流式页面区块和宽表横向操作；H5 复用同一发布快照，通过 `?display=h5` 链接和二维码访问。
- 最终审查补充应用中心 query 状态回填：组件复用或外部导航改变 `view/scope/filter/page` 时同步界面，避免重复请求。

## 2026-08-18 自动化验证

### 静态检查

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

- `git diff --check`：通过，无空白错误。
- `${...}` 扫描：本变更 `V1.0.124` 无命中；仅命中既有 `V1.0.72` 的消息模板变量，非 Flyway 属性占位符。
- `.DS_Store`：保留用户原有根目录修改和 `forge/.DS_Store` 删除，本变更未处理这两项。

### 后端编译

环境：Java 17，`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home`。

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
mvn -pl forge-admin-server -am compile -DskipTests
```

- Generator reactor：通过，最终复跑 32/32 模块成功。
- Admin reactor：通过，45/45 模块成功，覆盖本变更同时修改的 `forge-plugin-ai`。
- 编译警告为既有 deprecated/unchecked/Lombok builder 提示，本轮无新增编译错误。

### 后端测试

标准 reactor `test-compile -am` 未通过，阻断点是仓库既有测试与生产构造器漂移：

- `forge-plugin-message/MessageServiceImplTest`
- `BusinessObjectPublishServiceFieldEventTest`
- `BusinessObjectPublishServiceCommandTest`

该问题与本变更生产源码无关，但不能将全量测试编译记为通过。为验证新增测试，使用 Maven 生成的测试 classpath 单独编译：

- `BusinessApplicationExcelImportServiceTest`
- `BusinessApplicationAiAssistantServiceTest`

随后执行：

```bash
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator
mvn surefire:test -Penable-tests -Dtest='*BusinessApplication*Test'
```

- 结果：92 tests，0 failures，0 errors。
- 新增 Excel 测试发现并修复 `safeFileName`：无 `/` 的普通文件名此前会变为空串，导致合法 `.xlsx` 被拒绝。

### 前端 Lint、单测和构建

环境：Node.js 20.19.0。仓库 `pnpm-workspace.yaml` 缺少有效 `packages`，pnpm 8 在执行命令前报 `packages field missing or empty`；因此使用现有 `node_modules` 的直接入口。

```bash
cd forge-admin-ui
./node_modules/.bin/eslint --fix <本变更前端文件与目录>
./node_modules/.bin/eslint <本变更前端文件与目录>
./node_modules/.bin/vitest run \
  src/views/app-center/__tests__/app-template-catalog.spec.js \
  src/views/app-center/__tests__/application-create-result.spec.js
node --max_old_space_size=8192 ./node_modules/vite/bin/vite.js build
```

- ESLint：初次 0 errors / 119 个可修复换行 warnings；格式化后 0 errors / 0 warnings。
- Vitest：2 files、6 tests 全部通过。
- Vite：Node 20.19.0 下构建通过，9062 modules transformed，`built in 38.82s`。
- 构建保留的既有警告：Vite native config-loader 兼容提示、`UserSelectModal` 组件重名、CSS `//` 注释、两个 ineffective dynamic import、plugin timing 信息。

## 未执行与后续环境验收

- 未启动 Admin/Flow/Vite 开发服务；无本轮服务需要清理。
- 未连接或修改真实 MySQL/Redis，未执行 Flyway；需在隔离数据库验证新库、存量库、重复执行和索引结果。
- 未执行登录 Token、curl 新增接口、发布/回滚和权限用户的真实 API 链路。
- 未执行 Playwright、浏览器和移动真机验收。
- 未执行 Forge 首页实际投放和钉钉 Connector 同步：仓库当前没有对应持久化/调用协议，不能宣称外部效果完成。
- AI 助理没有执行真实数据查询或写入；当前能力是发布页面结构范围内的安全指导。

## 2026-08-18 Review 修复增量

### 修复范围

- 统一应用中心与正式门户的应用可见性判定：支持全员、指定角色、指定部门/组织、指定用户和应用管理员；用户、角色、组织范围均以可信会话上下文为准。
- 正式门户基于已发布版本快照中的 `portalConfig.permission` 二次校验；应用管理员绕过页面级 RBAC，`systemMenuVisible=false` 不再自动放行无页面权限的页面。
- 发布态 slug 只从已发布版本快照解析，设计态修改不会在重新发布前影响正式访问；修复 Mapper 重复包含基础列片段的问题。
- 新增 Forge 工作台投放查询接口，首页改为读取真实投放列表；当前用户投放保存可信 `targetUserId`，角色投放保存 `roleIds`，禁用/取消投放后不再展示；发布页增加取消工作台分发操作。
- 钉钉分发继续仅保存受管连接器标识和 `PENDING_EXTERNAL_SYNC`，未伪造外部同步成功。
- 将门户 slug 逻辑删除唯一索引调整为 `(tenant_id, portal_slug, del_flag)`，与既有 BIGINT 主键墓碑逻辑删除语义一致，允许删除后同 slug 重建。
- 门户根节点、水印时间消费配置中的语言、时区和日期格式；高级缓存/版本留存配置明确标注当前仅随发布快照保存，未伪造缓存清理任务。

### Review 修复验证

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
mvn -pl forge-admin-server -am compile -DskipTests

cd forge-framework/forge-plugin-parent/forge-plugin-generator
mvn -Penable-tests -Dtest='*BusinessApplication*Test' -DfailIfNoTests=false surefire:test
```

- Generator reactor：32/32 模块编译通过。
- Admin reactor：45/45 模块编译通过。
- BusinessApplication 相关测试：96 tests，0 failures，0 errors。
- 全量 `test-compile` 仍被仓库既有测试构造器漂移阻断（`BusinessObjectPublishServiceFieldEventTest`、`BusinessObjectPublishServiceCommandTest`），与本轮生产代码无关。

```bash
cd forge-admin-ui
./node_modules/.bin/eslint <本轮修改的前端文件与目录>
./node_modules/.bin/vitest run \
  src/views/app-center/__tests__/home-workbench-apps.spec.js \
  src/views/app-center/__tests__/portal-config.spec.js \
  src/views/app-center/__tests__/app-template-catalog.spec.js \
  src/views/app-center/__tests__/application-create-result.spec.js
node --max_old_space_size=8192 ./node_modules/vite/bin/vite.js build
```

- ESLint：0 errors、0 warnings。
- Vitest：4 files、8 tests 全部通过。
- Vite production build：9062 modules transformed，构建通过（约 40.84s）。
- 保留既有构建警告：native config-loader、`UserSelectModal` 重名、CSS `//` 注释、无效 dynamic import 和 plugin timing 信息。
- `git diff --check`、Mapper XML `xmllint --noout` 和 Flyway `${...}` 占位符扫描通过。

### 本轮未执行项

- 未执行真实 MySQL/Flyway、Admin/Flow/Vite 服务启动、登录 Token/curl 链路、Playwright/浏览器及移动真机验收；本轮未启动服务，无需清理。
- 未执行钉钉 Connector 外部同步，仓库当前无对应协议；未执行真实 AI 业务数据查询或写入，保持失败关闭。
- 组织私有模板持久化仍因仓库没有持久化协议保持空态，不宣称产品化完成。

## 2026-08-18 Final Fix 验证

### 最终修复内容

- 工作台入口只把设计态分发开关作为候选，名称、slug、应用可见范围、页面权限和可达首页统一从当前不可变发布快照计算。
- `/by-slug/{portalSlug}` 兼容接口改为发布态运行时投影；发布快照缺少历史 slug 时只回退稳定应用编码。
- 前端角色 ID 全程使用字符串，后端校验角色租户、逻辑删除、启用状态、门户基础权限及当前用户可管理范围。
- Flyway 的存量 slug 去重维度与唯一索引统一为 `(tenant_id, portal_slug, del_flag)`，不会因已删除历史记录改写当前有效应用 slug。
- AI 应用方案新增流程建议；用户确认初始化后只调用应用级 `BusinessProcessService.create()` 创建最小设计草稿，不部署 Flowable、不写 BPMN/节点表单绑定。
- 移除尚无自动赋权消费协议的默认角色输入项；缓存策略和版本保留继续明确为仅随发布快照保存。
- 组织私有模板、真实 AI 数据读写、钉钉/企微 Connector 明确保持延期，不伪造完成状态。

### 后端验证

环境：Java 17，`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home`。

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
mvn -pl forge-admin-server -am compile -DskipTests
```

- Generator reactor：32/32 模块 `BUILD SUCCESS`。
- Admin reactor：45/45 模块 `BUILD SUCCESS`。
- 保留的警告均为仓库既有 deprecated、unchecked 和 Lombok builder 提示。

标准测试源码编译：

```bash
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator
mvn -Penable-tests -DskipTests test-compile
```

- 结果：失败；仍由既有 `BusinessObjectPublishServiceFieldEventTest`、`BusinessObjectPublishServiceCommandTest` 构造器参数漂移阻断。
- 为避免复用旧 class，使用 Maven 生成的 test classpath 重新 `javac` 编译本轮新增/修改的三个测试类，编译成功。

```bash
mvn -Penable-tests -Dtest='*BusinessApplication*Test' \
  -DfailIfNoTests=false surefire:test
```

- 结果：100 tests，0 failures，0 errors，0 skipped。
- 新增 `BusinessApplicationAiInitializeServiceTest` 证明流程建议绑定本次生成对象并创建应用级流程设计草稿。

### 前端与静态验证

```bash
cd forge-admin-ui
./node_modules/.bin/eslint \
  src/views/app-center/components/create/AppCreateAi.vue \
  src/views/app-center/components/portal/portal-config.js \
  src/views/app-center/components/settings/AppSettingsPermission.vue \
  src/views/app-center/components/publish/AppPublishDistribute.vue \
  src/views/home/index.vue src/api/business-application.js

./node_modules/.bin/vitest run \
  src/views/app-center/__tests__/home-workbench-apps.spec.js \
  src/views/app-center/__tests__/portal-config.spec.js \
  src/views/app-center/__tests__/app-template-catalog.spec.js \
  src/views/app-center/__tests__/application-create-result.spec.js

node --max_old_space_size=8192 ./node_modules/vite/bin/vite.js build
```

- ESLint：0 errors、0 warnings。
- Vitest：4 files、11 tests 全部通过。
- Vite：9062 modules transformed，生产构建通过（约 32.19s）。
- 构建仅保留既有 config-loader、组件重名、CSS `//` 注释、ineffective dynamic import 和 plugin timing 警告。

```bash
git diff --check
xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessApplicationMapper.xml
rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.127__add_business_application_portal_config.sql
```

- 三项通过；Flyway 脚本无属性占位符。
- 未启动任何服务，无需清理端口或进程。

### 仍需环境/平台验收

- 隔离 MySQL 中执行 Flyway 并核验索引、存量回填和重复保护。
- 启动 Admin 后验证登录、发布/回滚、不同角色/组织/用户的门户与工作台 API 链路。
- Playwright/移动真机验证设置、发布、四种创建方式、门户导航和 H5。
- 平台先提供组织私有模板、受管 Connector、AI 业务操作、缓存执行器和版本清理协议，再继续相应验收。

## 2026-08-22 应用门户与应用中心体验修复增量

### 修复范围

- 应用门户页面导航统一归一化 `type/nodeType/parentId`，页面组使用明显的标题、缩进和左侧层级线，页面仍通过 `pageId` 切换。
- 应用门户右上角接入现有 `MessageNotification` 组件；个人资料改为应用上下文内的资料弹窗，不再把门户用户带回系统资料页；系统布局未传入上下文时保留原系统资料路由。
- 应用中心设置/发布入口统一打开当前 `BusinessApplicationRuntime` 工作台的 `view=settings/publish` 面板；窄屏下筛选、搜索、创建按钮分行布局；应用市场 Tab 支持横向滚动并与搜索框分行。
- 发布页状态卡只展示当前版本、最近发布和页面数；应用列表只展示页面资产，并将草稿/待发布/有变更状态统一显示为“有变更未发布”。发布成功后通过事件和 `storage` 通知应用中心刷新列表。

### 前端增量验证

```bash
cd forge-admin-ui
pnpm exec vitest run \
  src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js \
  src/views/app-center/__tests__/application-runtime-load.spec.js \
  src/views/app-center/__tests__/workspace-redirect.spec.js \
  src/views/app-center/__tests__/application-designer-navigation.spec.js \
  --reporter=dot
pnpm exec vite build
```

- Vitest：4 个文件、29 个测试全部通过。
- Vite production build：成功，9106 modules transformed；保留仓库既有 native config-loader、CSS `//` 注释、ineffective dynamic import 和 plugin timing 警告。
- 目标文件 ESLint 检查发现 `application-runtime.[applicationCode].vue` 中已有未使用函数/声明顺序问题（来自本轮之前的工作区改动）；本轮新增门户、导航、应用中心、发布状态代码已通过构建并未引入编译错误。

### 未执行项

- 未启动 Admin/Vite 服务，未执行用户登录、真实消息接口和应用发布 API 链路。
- 未执行 Playwright 375/768/1280 宽度截图验收；需在本地服务可用、具备 `admin/123456` 登录环境后补跑。
- 未修改数据库或 Flyway，本轮无新增后端迁移；未启动服务，无需清理进程。

## 2026-08-18 字典缓存运行时异常修复

- 现象：组织树字段翻译 `SysOrgTreeVO.orgType` 抛出 `LinkedHashMap cannot be cast to SysDictData`。
- 根因：受管缓存只保留了集合外层具体类型，Redis JSON 往返后的泛型元素成为 Map；`SytemDictValueProvider` 的增强 `for` 在读取元素时发生隐式强转。
- 修复：共享 `ForgeCacheAspect` 按被缓存方法声明的泛型返回类型恢复容器元素，转换失败时清理坏 entry 并穿透；字典 Provider 额外兼容历史 Map 缓存项。
- 红测：新增 AOP 用例修复前稳定复现 `Map1 cannot be cast to SamplePayload`。
- 绿测：`ForgeCacheAspectTest` 6 tests；starter 无 Mockito 的 6 个相关测试类共 17 tests；`SysDictDataServiceImplTest + SytemDictValueProviderLegacyCacheTest` 共 8 tests，均为 0 failures/errors。
- 编译：Admin reactor 45/45 模块 `BUILD SUCCESS`。
- 环境警告：完整 starter 27 tests 中 4 个既有 Mockito Manager 用例因当前 JDK 无法 self-attach Byte Buddy agent 报环境错误，未记为通过。
- 未执行：未重启用户现有 Admin/Redis，未调用组织树接口做真实缓存命中复验；本轮未启动服务，无需清理。

## 2026-08-18 表单左侧组件完整支持修复

### 修复范围

- 新增前端字段组件合同，左侧 33 个字段组件、自动建字段、属性面板和表单编译统一读取同一份默认定义。
- 新增后端组件目录，`LowcodeSchemaValidator` 支持全部字段组件；`slider`、`year` 等扩展组件保存时不再抛“不支持的控件类型”。
- 补齐 13 个扩展字段组件的模型恢复和发布编译，运行时保留原组件类型，不再回退为 `input`。
- 修正 Naive UI 日期/时间控件的格式化值绑定，年份、月份、日期/日期时间及范围值按业务字符串回显和提交，不再混用时间戳协议。
- 多选、穿梭框、日期范围、日期时间范围、时间范围统一以 JSON 数组写入文本列，读取时恢复数组并兼容历史逗号分隔值。
- 19 个高级页面组件发布为 `widget` 布局节点，并接入表单运行时 `PageWidgetRenderer`，不再被编译器静默过滤。

### 验证证据

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

- Generator reactor：32/32 模块 `BUILD SUCCESS`，耗时 18.734 秒。
- 编译警告为仓库既有 deprecated、unchecked 和 Lombok builder 提示，无新增编译错误。

```bash
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator
mvn -Penable-tests \
  -Dtest='BusinessObjectDesignerPageSchemaTest,LowcodeRuntimeConfigBuilderTest,DynamicCrudStructuredValueTest' \
  -DfailIfNoTests=false surefire:test
```

- 后端目标测试：28 tests，0 failures，0 errors，0 skipped。
- 覆盖全部字段组件校验、13 个扩展字段及 19 个 Widget 布局编译、扩展组件运行时类型保留，以及数组值 JSON/历史逗号值往返。
- 标准全模块 `test-compile` 仍受既有 `BusinessObjectPublishServiceFieldEventTest`、`BusinessObjectPublishServiceCommandTest` 构造器参数漂移阻断；本轮测试类已单独编译并由 Surefire 实际执行，不把全量测试编译记为通过。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
./node_modules/.bin/eslint <字段组件本轮文件>
./node_modules/.bin/vitest run <6 个本轮回归测试文件>
node --max_old_space_size=8192 ./node_modules/vite/bin/vite.js build
```

- ESLint：0 errors、0 warnings。
- Vitest：最终 6 files、23 tests 全部通过；组件合同测试逐项确认左侧 33 个字段组件具备运行时渲染入口，`AiFormItem` 测试确认年份和日期范围按格式化字符串往返。
- Vite production build：9063 modules transformed，`built in 41.72s`。
- 构建仅保留既有 CSS `//` 注释、ineffective dynamic import 和 plugin timing 警告。
- 未启动 Admin/Flow/Vite 开发服务，未修改数据库或 Redis，无本轮服务需要清理。

## 2026-08-18 Task 18 页面形态与表单保存最小闭环

### 实现范围

- 新增 `PageTypeSelector.vue` 与 `page-shape-design.js`：页面形态选择支持表单页、列表页、列表 + 表单、自定义页面；页面名称自动推导对象名称/编码且编码可编辑。
- 页面管理主入口增加空白应用引导，数据页创建后直接进入现有 `ForgeFormDesigner`；顶部对象栏持续展示并允许编辑对象名称/编码。
- 新增 `designBusinessApplicationPage` API 与后端 `POST /ai/business/application/{id}/design-page`；元数据事务内保存应用构建器、对象、字段、表单 Schema、默认列表/详情视图及可见对象关联，DDL 在事务提交后安全追加同步。
- 后端按实际数据记录数拒绝字段删除、字段编码变化和存储类型变化；已有数据字段写入 `fieldBinding.locked`，前端禁用字段删除、编码和类型操作，并禁止清空包含锁定字段的画布。
- 对象字段复用保存时合并运行字段目录，后端校验页面确实包含并绑定提交的表单资产；更新对象关联时保留原有扩展配置。

### 自动化验证

前端环境：Node.js 20.19.0（通过 `source ~/.nvm/nvm.sh && nvm use v20.19.0`）。

```bash
cd forge-admin-ui
./node_modules/.bin/eslint \
  'src/views/app-center/application-runtime.[applicationCode].vue' \
  src/views/app-center/components/designer/PageTypeSelector.vue \
  src/views/app-center/in-app-builder/page-shape-design.js \
  src/views/app-center/in-app-builder/page-form-object-promotion.js \
  src/views/app-center/in-app-builder/in-app-builder-schema.js \
  src/views/app-center/in-app-builder/__tests__/page-shape-design.spec.js \
  src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue \
  src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue \
  src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue \
  src/api/business-application.js
```

- 结果：通过，0 errors / 0 warnings。

```bash
./node_modules/.bin/vitest run \
  src/views/app-center/in-app-builder/__tests__/page-shape-design.spec.js \
  src/views/app-center/components/designer/form-first/__tests__/formDesignerSchema.spec.js
```

- 结果：2 files、14 tests 全部通过；覆盖四种页面形态、五种基础字段、对象字段复用和 Schema 归一化。

```bash
NODE_OPTIONS=--max-old-space-size=8192 ./node_modules/.bin/vite build
```

- 结果：生产构建通过，9066 modules transformed。保留既有 Vite native config-loader、组件重名、CSS `//` 注释、两个 ineffective dynamic import 和 plugin timing 警告。

后端环境：Java 17（`/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home`）。

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile
```

- 结果：Generator reactor 32/32 模块 `BUILD SUCCESS`。

由于根 POM 的全模块 `test-compile` 仍被无关既有测试构造器漂移阻断，本轮按测试标准复用了生成的 Maven classpath，单独编译本轮三类测试后执行：

```bash
cd forge-framework/forge-plugin-parent/forge-plugin-generator
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
javac -cp "target/classes:target/test-classes:$(<target/test-classpath.txt)" -d target/test-classes \
  src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPageDesignServiceTest.java \
  src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPageFieldGuardTest.java \
  src/test/java/com/mdframe/forge/plugin/generator/controller/BusinessApplicationControllerTest.java
JAVA_TOOL_OPTIONS='-javaagent:/Users/yaomindong/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' \
  mvn -Penable-tests \
  -Dtest='BusinessApplicationPageDesignServiceTest,BusinessApplicationPageFieldGuardTest,BusinessApplicationControllerTest' \
  -DfailIfNoTests=false surefire:test
```

- 结果：14 tests、0 failures、0 errors。显式 Byte Buddy agent 仅用于绕过当前 JVM 无法 self-attach 的环境限制，未修改 POM。

### 未执行项

- 未启动 Admin/Flow/Vite 服务，未连接真实 MySQL/Redis，未执行 Flyway、登录 Token、curl 或 Playwright；本轮未启动服务，无需清理进程。
- 列表专用设计器、真实数据新增和移动端/浏览器交互仍需隔离环境验收；当前列表形态复用同一表单资产与 `AiCrudPage` 运行协议，不宣称专用列表编辑体验已验收。
- 根 POM 全量测试编译的已知阻断仍是 `forge-plugin-message/MessageServiceImplTest` 及其它既有构造器参数漂移，与本轮生产代码无关。

## 2026-08-18 Task 18 交付前补强验证

### 补强范围

- 修复从「对象字段」拖入字段时未继承 `fieldBinding.locked` 的缺口；对象已有业务数据时，运行字段目录会向复用组件传递结构锁。
- 后端增加已持久化表单组件对比：字段列表作为保存基线时，仍按稳定组件 ID 拒绝锁定组件删除、字段编码变化和组件类型变化，防止直接 API 请求绕过前端。
- 页面形态选择弹窗和设计器对象栏改用 Naive UI 主题变量，并补充窄屏单列布局。

### 验证证据

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
./node_modules/.bin/eslint <Task 18 本轮文件>
./node_modules/.bin/vitest run \
  src/views/app-center/in-app-builder/__tests__/page-shape-design.spec.js \
  src/views/app-center/components/designer/form-first/__tests__/formDesignerSchema.spec.js
NODE_OPTIONS=--max-old-space-size=8192 ./node_modules/.bin/vite build
```

- ESLint：0 errors、0 warnings。
- Vitest：2 files、15 tests 全部通过。
- Vite：9066 modules transformed，生产构建通过（42.96 秒）。
- 保留既有 native config-loader、`UserSelectModal` 重名、CSS `//` 注释、ineffective dynamic import 和 plugin timing 警告。

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile

cd forge-framework/forge-plugin-parent/forge-plugin-generator
javac -cp "target/classes:target/test-classes:$(<target/test-classpath.txt)" -d target/test-classes \
  src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPageDesignServiceTest.java \
  src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPageFieldGuardTest.java \
  src/test/java/com/mdframe/forge/plugin/generator/controller/BusinessApplicationControllerTest.java
JAVA_TOOL_OPTIONS='-javaagent:/Users/yaomindong/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' \
  mvn -Penable-tests \
  -Dtest='BusinessApplicationPageDesignServiceTest,BusinessApplicationPageFieldGuardTest,BusinessApplicationControllerTest' \
  -DfailIfNoTests=false surefire:test
```

- Generator reactor：32/32 模块 `BUILD SUCCESS`。
- 后端定向测试：17 tests、0 failures、0 errors、0 skipped。
- `git diff --check` 通过。

### 未执行项

- 未启动 Admin/Flow/Vite 开发服务，未连接或修改 MySQL/Redis；无本轮服务需要清理。
- 真实登录/curl、MySQL DDL/Flyway、页面保存后新增数据和 Playwright 交互继续保留为隔离环境验收项。

## 2026-08-19 Review 修复验证

- 范围：工作台重定向、页面管理主入口、设置页权限归集、发布快照分发判定、门户权限 ID 校验。
- 前端定向 ESLint 通过；Vitest 6 files、19 tests 通过。
- 后端 `BusinessApplicationServiceTest` + `BusinessApplicationRuntimeServiceTest`：27 tests、0 failures。
- Generator reactor 32/32 模块 `compile -DskipTests` 通过；`git diff --check` 通过。
- 未启动 Vite/Admin，未做浏览器端到端点击验证。

## 2026-08-19 页面保存失败修复

- 现象：直接设计页面保存时 `design-page` 报「数据模型至少需要一个业务字段」。
- 原因：新建对象只有系统字段时，`hasBusinessData()` 仍调用 `previewCreateTable()`，该接口会先做完整模型校验。
- 修复：没有业务字段时直接视为无业务数据，先保存用户字段再同步数据表。
- 验证：`BusinessApplicationPageDesignServiceTest` 3 tests 通过。

## 2026-08-22 应用门户问题修复增量

- 范围：页面组与页面树层级展示、应用门户个人资料上下文、复用系统消息通知组件、应用中心和应用市场响应式布局、设置/发布路由兼容、发布成功后的列表刷新。
- 兼容修复：`UserAvatar` 仅在门户传入 `profileRoute` 时留在应用内；系统布局未传该参数时继续在当前窗口跳转 `/profile`。

```bash
cd forge-admin-ui
pnpm exec eslint \
  src/layouts/components/UserAvatar.vue \
  src/views/app-center/application-portal.vue \
  src/views/app-center/components/portal/PortalNavigation.vue \
  src/views/app-center/in-app-builder/in-app-builder-schema.js \
  src/views/app-center/index.vue \
  src/router/index.js \
  src/views/app-center/components/ApplicationTable.vue \
  src/views/app-center/components/AppMarketPanel.vue \
  src/views/app-center/components/publish/AppPublishStatusCard.vue \
  src/views/app-center/application-workspace/ApplicationPublishPanel.vue
```

- 结果：ESLint 通过。

```bash
pnpm exec vitest run \
  src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js \
  src/views/app-center/__tests__/application-runtime-load.spec.js \
  src/views/app-center/__tests__/workspace-redirect.spec.js \
  src/views/app-center/__tests__/application-designer-navigation.spec.js \
  --reporter=dot
```

- 结果：4 个文件、29 个测试全部通过。

```bash
pnpm exec vite build
```

- 结果：生产构建通过，9106 个模块转换完成。保留既有 Vite native config-loader、CSS `//` 注释、ineffective dynamic import 和 plugin timing 警告，均未阻断构建。
- 未执行：未启动 Admin/Flow/Vite 服务，未连接真实 MySQL/Redis；Playwright 登录后的窄屏布局、门户消息/资料交互、发布后真实列表刷新待可用后端环境验收。本轮未启动服务，无需清理进程。

补充：随后针对发布状态边界（新应用显示“尚未发布”，已有版本且草稿变更显示“有变更未发布”）重新执行 `pnpm exec vite build`，构建成功（9106 个模块转换）。

## 2026-08-22 顶部菜单渲染修复

- 现象：应用门户顶部导航将页面组和子页面用自定义 flex 结构并排渲染，页面组标题、子页面和内容区发生挤压重叠。
- 修复：`PortalNavigation.vue` 的 `style="top"` 分支改为直接复用系统 `TopMenuBar`，把页面组/页面转换为系统菜单项，使用系统既有悬浮下拉、多级菜单、激活态和滚动逻辑；`TopMenuBar`/`TopMenuDropdown` 增加对字符串图标的兼容，既保留系统组件图标，也支持应用页面图标。
- 侧边导航仍保留应用门户的父子缩进展示，只有顶部导航复用系统顶部菜单，不影响侧边模式。

```bash
cd forge-admin-ui
pnpm exec eslint \
  src/views/app-center/components/portal/PortalNavigation.vue \
  src/views/app-center/application-portal.vue \
  src/layouts/components/TopMenuBar.vue \
  src/layouts/components/TopMenuDropdown.vue
pnpm exec vitest run src/views/app-center/__tests__/portal-config.spec.js --reporter=dot
pnpm exec vite build
```

- 结果：ESLint 通过；Vitest 3 tests 通过；Vite 生产构建成功，保留既有 ineffective dynamic import 和 plugin timing 警告。

### 2026-08-22 page_11 真实页面复验与 prop 根因修复

- 真实访问地址：`http://localhost:3000/app/presale_registration_apply?pageId=page_11`。
- 首次复验发现 DOM class 为 `portal-navigation is-[object Object]`，说明组件的 `style` prop 被 Vue 当作原生 `style` 属性处理，顶部导航没有进入系统菜单分支。
- 修复：将 `PortalNavigation` 的 `style` prop 重命名为 `navigationStyle`，同步修改门户三个调用点和递归子节点传递。
- Playwright 复验结果：页面 DOM 出现 `forge-top-menu portal-system-top-menu`；悬浮页面组后，系统下拉菜单显示 `12`、`11`，菜单位置位于顶栏下方，不再覆盖页面主体。
- 追加点击复验：点击系统下拉项 `12` 后 URL 正确切换为 `?pageId=page_12`，返回 `page_11` 后页面组仍保持系统顶部菜单结构。
- 产物截图：`var/tmp/page11-portal-menu-hover.png`（临时验收文件）。

### 2026-08-22 顶部菜单激活项可见性修复

- 现象：点击普通页面后页面组看似隐藏，点击页面组子页面后普通页面又看似隐藏。
- 根因：复用的 `TopMenuBar` 默认激活色为白色，而应用门户顶部背景也是白色；菜单节点实际仍在 DOM 中，只是激活项白底白字不可见。
- 修复：门户顶部菜单覆盖系统颜色变量，普通项使用门户次要文字色，激活项使用门户主题色，悬浮使用门户主文字色；页面组没有配置图标时补充文件夹图标。
- 验证：Playwright 依次点击 `测试`、页面组子项 `11`，两个顶层菜单始终可见；激活项颜色为 `rgb(51, 112, 255)`，非激活项颜色为 `rgb(100, 106, 115)`；ESLint 通过。

## 2026-08-23 存量预售对象页结构恢复

- 根因：真正的预售应用是 `PRESALE_REGISTRATION_APP`，原 `PS_PRESALE_ORDER / ps_presale_order` 对象、CRUD 发布配置、入口和历史业务数据均存在；改版前对象页没有独立页面实体，新版只读取 `inAppBuilder.nodes/pages` 后得到空页面树。`presale_registration_apply` 已被另一套测试数据改为“请假申请”，未对其做覆盖恢复。
- 修复：前端设计/门户规范化和后端不可变运行快照同时兼容 `primaryObjectCode + PRIMARY 对象`，投影为新版对象页；仅在页面树为空且存在旧主对象标识时触发。迁移结构写入 `legacyObjectPageMigrated`，避免用户以后主动删除全部页面时被重复补回；设计工作台首次打开后自动写回草稿。
- 数据操作：通过应用现有更新 API，仅将 `PRESALE_REGISTRATION_APP` 的恢复页面 `page_ps_presale_order` 写入应用草稿；没有重建或覆盖对象、CRUD 配置和 `ps_presale_order` 业务数据，没有发布新应用版本。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
./node_modules/.bin/eslint \
  src/views/app-center/in-app-builder/in-app-builder-schema.js \
  src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js
./node_modules/.bin/vitest run \
  src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js
```

- 结果：目标 Schema/Test ESLint 通过；Vitest 1 file、18 tests 全部通过。整份 `application-runtime.[applicationCode].vue` 定向 ESLint 仍命中该脏工作区已有的 17 个 `no-use-before-define/no-unused-vars` 错误，本轮未扩大修复范围；真实 Vite HMR 页面加载和自动迁移调用通过。

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests

cd forge-framework/forge-plugin-parent/forge-plugin-generator
mvn -Penable-tests -DskipTests test-compile
mvn -Penable-tests -Dtest='BusinessApplicationRuntimeServiceTest' \
  -DfailIfNoTests=false surefire:test
```

- 结果：Generator reactor 32/32 模块 `BUILD SUCCESS`；测试源码重新编译成功；`BusinessApplicationRuntimeServiceTest` 9 tests、0 failures、0 errors。
- Playwright：打开 `http://localhost:3000/app/PRESALE_REGISTRATION_APP` 后 URL 回填 `pageId=page_ps_presale_order`；导航显示“预售单”，页面加载 `ps_presale_order` 的查询、工具栏、表格和 5 条历史记录，`.ai-crud-page` 数量为 1。设计工作台首次打开返回应用更新接口 200，并确认恢复节点、对象引用和迁移标记已写回当前草稿。
- 产物截图：`var/tmp/presale-restored-workspace.png`、`var/tmp/presale-restored-portal.png`（临时验收文件）。
- 跳过：按用户要求未执行前端生产构建和全量 Maven 构建；未启动或停止任何服务，复用用户已有的前后端开发服务。

## 2026-08-23 正式应用门户系统页面修复

- 范围：正式动态应用门户缺少系统自带页面。新增共享系统导航节点投影，PC 端固定显示“个人工作台、我的待办、我已办的、我发送的、抄送我的”，并复用 `PageManagementSystemView`；H5 不注入桌面工作台页面，继续按 `mountTarget` 过滤用户页面。
- 代码：`page-management.js` 提供系统页导航节点工厂；`portal-navigation-runtime.js` 提供应用门户客户端投影；`PortalNavigation.vue` 对系统页做独立置顶排序；`application-portal.vue` 对 `system:*` 页面切换到现有 Workspace 组件。

```bash
cd forge-admin-ui
./node_modules/.bin/eslint \
  src/views/app-center/application-portal.vue \
  src/views/app-center/components/portal/PortalNavigation.vue \
  src/views/app-center/components/portal/portal-navigation-runtime.js \
  src/views/app-center/in-app-builder/page-management.js \
  src/views/app-center/__tests__/portal-navigation-runtime.spec.js \
  src/views/app-center/in-app-builder/__tests__/page-management.spec.js
./node_modules/.bin/vitest run \
  src/views/app-center/__tests__/portal-navigation-runtime.spec.js \
  src/views/app-center/in-app-builder/__tests__/page-management.spec.js \
  src/views/app-center/__tests__/portal-config.spec.js --reporter=dot
cd ..
git diff --check
```

- 结果：目标 ESLint 通过；Vitest 3 个文件、11 个测试全部通过；`git diff --check` 通过。
- Playwright：使用现有开发服务登录应用门户，顶部菜单顺序为“个人工作台、我的待办、我已办的、我发送的、抄送我的、测试、页面组 1”；点击“我的待办”后 URL 为 `http://localhost:3000/app/presale_registration_apply?pageId=system:todo`，渲染 `.flow-page` 1 个且显示待办空状态。
- 跳过：未执行前端生产构建、全量 Maven 构建和新服务启动；无本轮服务需要清理。

## 2026-08-23 应用内通知公告与待办跳转修复

- 范围：系统页面菜单补齐 Ionicons 图标；新增 `system:messages` 通知公告页；门户右上角消息通知、消息列表中的审批入口、工作台统计卡片均支持应用内命名路由，不再跳到旧 `/flow/*`、`/message/*` 页面。
- 关键实现：`MessageNotification` 增加 `messageRoute`、`todoRoute`、`doneRoute`；`mergeMessageNavigationTarget` 保留应用路由参数并合并任务参数；`PageManagementSystemView` 复用现有 `MessageList`、Workspace 和 Flow 页面。

```bash
cd forge-admin-ui
./node_modules/.bin/eslint \
  src/views/app-center/application-portal.vue \
  src/views/app-center/components/portal/PageManagementSystemView.vue \
  src/views/app-center/components/portal/PortalNavigation.vue \
  src/views/app-center/components/portal/portal-navigation-runtime.js \
  src/views/app-center/in-app-builder/page-management.js \
  src/layouts/components/MessageNotification.vue \
  src/layouts/components/message-notification-utils.js \
  src/layouts/components/__tests__/message-notification-utils.spec.js \
  src/views/message/message-list.vue \
  src/views/workspace/summary.vue \
  src/views/flow/todo.vue \
  src/views/app-center/in-app-builder/__tests__/page-management.spec.js \
  src/views/app-center/__tests__/portal-navigation-runtime.spec.js
./node_modules/.bin/vitest run \
  src/views/app-center/in-app-builder/__tests__/page-management.spec.js \
  src/views/app-center/__tests__/portal-navigation-runtime.spec.js \
  src/layouts/components/__tests__/message-notification-utils.spec.js \
  src/views/app-center/__tests__/portal-config.spec.js --reporter=dot
cd ..
git diff --check
```

- 结果：目标 ESLint 通过；Vitest 4 个文件、16 个测试全部通过；`git diff --check` 通过。
- Playwright：现有开发服务实测顶部系统菜单包含图标；点击“通知公告”进入 `pageId=system:messages`，消息列表正常；点击“我的待办”进入 `pageId=system:todo`，`.flow-page` 正常；消息抽屉“查看全部消息”进入 `system:messages`。
- 备注：对整份历史工作台文件执行 ESLint 仍会报告本轮之前已存在的 `application-runtime.[applicationCode].vue` 变量顺序/未使用函数错误，本轮仅新增图标映射，没有扩大清理范围。

## 2026-08-23 应用门户滚动与表单底部操作区修复

- 范围：修复应用内待办/已办等系统页面无法下滑，以及动态页面内容和表单底部操作按钮被固定区块高度裁掉的问题。
- 根因：门户壳只有最小高度且主内容区禁止滚动；同时 `.portal-loading-host :deep(.n-spin-content)` 把 `100vh` 误应用到待办列表内部 `NSpin`。动态页面的 `pageHeight` 未计入 `style.height`，运行态表单继续使用设计态固定高度和 `overflow: hidden`。
- 修复：门户建立 `100vh -> calc(100vh - 56px) -> min-height: 0` 的确定高度链，主内容区负责纵向滚动；根加载样式收窄为直接子级；系统页内部保持受限高度；页面高度兼容 `style.height`，`AiForm` 在运行态使用最小高度并允许内容自然撑开。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
./node_modules/.bin/eslint \
  src/views/app-center/application-portal.vue \
  src/views/app-center/components/portal/PageManagementSystemView.vue \
  src/views/app-center/components/portal/PortalPageRenderer.vue
./node_modules/.bin/vitest run \
  src/views/app-center/in-app-builder/__tests__/page-management.spec.js \
  src/views/app-center/__tests__/portal-navigation-runtime.spec.js \
  src/layouts/components/__tests__/message-notification-utils.spec.js \
  src/views/app-center/__tests__/portal-config.spec.js --reporter=dot
cd ..
git diff --check -- \
  forge-admin-ui/src/views/app-center/application-portal.vue \
  forge-admin-ui/src/views/app-center/components/portal/PageManagementSystemView.vue \
  forge-admin-ui/src/views/app-center/components/portal/PortalPageRenderer.vue
```

- 结果：目标 ESLint 通过；Vitest 4 个文件、16 个测试全部通过；相关文件 `git diff --check` 通过。
- Playwright 桌面视口 1280×800：`portal-main` 高 744px；待办卡片滚动区 `clientHeight=586`、`scrollHeight=934`，设置后 `scrollTop` 从 0 变为 240。
- Playwright 1024×500 系统页循环：待办、已办、我发送的卡片滚动区设置后 `scrollTop` 均从 0 变为 120；抄送我的和通知公告当前内容未超过容器，不产生无意义滚动条。
- Playwright 小视口 1024×500：动态页内容区 `clientHeight=444`、`scrollHeight=680`；进入新增表单并滚到底后，底部操作区位于 `y=416..471`，完整落在 500px 视口内。
- Playwright 移动宽度 390×844：移动导航占高 122px，门户主内容区自动取得剩余 666px；待办卡片滚动区 `clientHeight=324`、`scrollHeight=1864`，没有再被门户根加载样式撑高。
- 未执行：按用户要求未执行前端生产构建和全量 Maven 构建；复用用户已运行的开发服务，本轮未启动或停止任何服务。
