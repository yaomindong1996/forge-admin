# 执行记录

## 2026-08-22

- 开始本轮增量实现；已读取 `spec.md`、项目记忆、Forge 流程开发参考、编码规范和自动化测试标准。
- 初始后端 compile 暴露 `FlowTaskNotifyListener` 调用三参数方法时遗漏 `event.getVariables()`；已修复，待复跑。
- 修正前端通知矩阵兼容语义：NULL 配置仍保持 NULL；缺失 result/cc 事件不再默认写入 WEB；todo WEB 必选，result/cc 可不启用；显式 COLLABORATION 配置会被保留，用于绕过连接级 `todoPushEnabled`。
- 通知监听器的流程模型查询改为 `FlowModelMapper.selectByModelKey` XML SQL，并在业务租户上下文中执行；XML 显式过滤 `del_flag = 0`。流程模型复制同时复制通知/Webhook/深链配置。
- 渠道接口增加协同 Provider MESSAGE 能力校验，只有已启用连接且存在消息适配器的平台才返回；补齐实际平台编码 `WECHAT_ENTERPRISE` 的企业微信展示名。
- 定向单测：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -Penable-tests -Dtest=com.mdframe.forge.starter.flow.support.FlowNotifyConfigTest -Dsurefire.failIfNoSpecifiedTests=false -Dsurefire.useFile=false test`，3 个测试通过，`BUILD SUCCESS`。非法 JSON 用例按预期产生一条回退 warning。
- Flow 插件编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests`，JDK 17，`BUILD SUCCESS`；仅有项目既有过时 API、unchecked、Lombok Builder warning。
- Flow Server 编译：`mvn -pl forge-flow/forge-flow-server -am compile -DskipTests`，JDK 17，37 模块全部 `SUCCESS`；仅有项目既有编译 warning。
- 前端定向 ESLint：`./node_modules/.bin/eslint src/views/flow/model.vue src/api/flow.js`，0 error、4 个 Vue 单行 HTML 格式 warning。
- 前端构建：`./node_modules/.bin/vite build`，成功（`built in 33.26s`）；存在项目既有 Vite native config、CSS `//` 注释、dynamic import 与 chunk/plugin timing warning。
- 迁移静态检查：V1.0.128/V1.0.129 版本唯一；新增模板均 `tenant_id = 1` 且使用 `NOT EXISTS`；无未转义 Flyway `${...}`；`git diff --check` 通过。
- 复核后端/前端最终改动：FlowNotifyConfig 对 todo 始终补齐 WEB、cc 过滤 SMS；实体允许显式写回 `notify_config = NULL`；渠道接口只暴露已注册 MESSAGE 能力的平台；模板平台编码兼容 `WECHAT_ENTERPRISE` → `WECOM`。最终 Flow Server compile 与前端 build 仍为成功。
- 前端最终 build 用时约 56.06s，warning 类型与上一轮一致；迁移静态扫描按 `del_flag = 0` 排除逻辑删除条件后通过（未发现 tenant_id=0、`${...}` 或重复版本）。
- 未执行真实数据库 Flyway、真实消息投递、真实协同连接与浏览器端到端验证；这些验证需要可用数据库/服务/外部平台配置。
- 本轮未启动服务、未连接数据库、未修改或清理其他任务进程。

## 2026-08-22（最终审查增量）

- 审查修正：企业协同卡片模板编码解析改在业务租户上下文内执行，避免异步线程查询 `sys_message_template` 时缺少租户条件。
- 审查修正：抄送落库服务未装配时不再提前终止配置化渠道通知；仍会记录告警并继续执行矩阵渠道分发。
- 审查修正：抄送角色解析、待办接收人解析显式恢复业务租户上下文，覆盖异步线程读取系统组织表的路径。
- 审查修正：通知渠道接口补齐 `@ApiDecrypt` / `@ApiEncrypt`，与流程模型接口保持一致的报文协议。
- 复跑定向单测：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -Penable-tests -Dtest=com.mdframe.forge.starter.flow.support.FlowNotifyConfigTest -Dsurefire.failIfNoSpecifiedTests=false -Dsurefire.useFile=false test -q`，3 个测试通过。
- 复跑 Flow 插件编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests -q`，成功。
- 复跑 Flow Server 编译：`mvn -pl forge-flow/forge-flow-server -am compile -DskipTests -q`，成功。
- 前端定向 ESLint：`./node_modules/.bin/eslint src/views/flow/model.vue src/api/flow.js`，成功，无错误。
- 前端生产构建：`NODE_OPTIONS=--max-old-space-size=8192 ./node_modules/.bin/vite build`，成功（约 41.79s）；仅保留既有 native config、动态导入和插件耗时警告。
- 迁移静态检查：V1.0.128/V1.0.129 版本无重复；新增 SQL 无 `tenant_id = 0`、无 Flyway `${...}` 占位符；相关文件 `git diff --check` 通过。
- 本轮仍未启动服务、连接数据库或调用真实邮件/SMS/企业协同渠道；真实 Flyway 与消息投递 E2E 继续作为环境验收项。

## 2026-08-22（内嵌流程设计器焦点修复）

- 修复应用中心内嵌业务流程设计器的输入框无法点击问题：Naive UI `n-modal` 焦点陷阱要求默认内容根节点为 `div`，原 `section` 根节点会导致焦点被重定向到 `aria-hidden` 哨兵。
- 应用运行时业务流程弹层和业务流程页内嵌 Flowable 设计器弹层统一改为 `div` 根容器，保留原有尺寸和弹层行为。
- 前端定向 Vitest：`page-design-tabs.spec.js`、`business-process-designer-workbench.spec.js`，27 个测试通过；定向 ESLint 通过；Playwright 实际输入“按钮名称”成功且无 `aria-hidden` 焦点警告。
- 按用户要求未继续生产构建；未启动服务或连接数据库。

## 2026-08-22（流程模型创建与姓名展示优化）

- 新建流程模型时由服务端生成唯一 `modelKey`，前端仅展示只读值；保存成功后回填服务端返回的真实 Key。
- 新建流程页面移除流程类型、表单类型和事件通知方式选择；新模型后端固定 `notify_type = redis`，并新增 `V1.0.130__default_flow_event_notify_redis.sql` 调整数据库默认值。
- 流程设计页继续保留表单类型配置，但仅在模型保存后（编辑状态）展示，避免创建阶段选择。
- 流程发起、任务创建/分配、待办/已办/我发起列表、流程历史和任务表单统一按用户 ID 反查真实姓名，兼容历史账号值和低代码发起记录。
- 最终验证：Flow 插件及 Generator 插件使用 JDK 17 编译成功；前端 `vite build` 成功（约 46 秒）；相关 ESLint 和 `git diff --check` 通过。
- 未执行真实数据库 Flyway、服务启动、真实消息投递和浏览器 E2E；需在集成环境验收。

## 2026-08-22（通知配置入口与模板预览优化）

- 将流程模型新增/编辑弹窗中的通知矩阵移除，统一放入流程设计页「更多设置 → 通知与推送」。
- 通知设置按新待办、审批结果、流程通过抄送分组展示，可选择通知渠道，并显示当前选中的模板。
- 新增模板下拉选择、消息模板详情加载和示例变量预览，支持预览标题与内容；默认模板分别为 `FLOW_TODO_CARD`、`FLOW_RESULT_CARD`、`FLOW_CC_CARD`。
- 模板数据通过消息模板接口读取，渠道仍通过 `/api/flow/notify-channels` 动态读取；通知配置仍保存到 `sys_flow_model.notify_config`。
- 前端定向 ESLint 通过；Vite 生产构建通过（约 52.53 秒，保留项目既有配置/动态导入警告）。

## 2026-08-22（流程运行态与应用发布回归修复）

- 页面发布节点标准化保留 `mountTarget`、`menuName`、`menuParentId`、`menuSort` 及系统菜单可见性；切换管理端/移动端挂载时自动打开菜单可见性，避免发布服务过滤掉页面。
- 应用页面菜单发布支持 `ADMIN → pc`、`MOBILE → h5`、`BOTH → pc+h5`，并按权限码+客户端幂等 upsert，避免 pc/h5 同权限码互相覆盖；H5 不复用管理端外部父级资源 ID。
- 动态 CRUD 成功写入事件接入已发布 `START_EVENT` 业务流程，事件运行记录使用版本+事件+对象+记录 ID 幂等键；新增/更新同时发布 `FORM_SUBMITTED` 事件。
- 审批动作节点遇到历史通用对象码 `business_object` 时回退到已发布流程主对象快照，并允许更新当前主记录；仍只查询发布态 CRUD 运行配置。
- H5 低代码字段适配补齐日期/时间/数值区间双端点控件与数组序列化；普通单表保存发送可写字段顶层协议，避免服务端收到仅含 `main/children` 包装而报“没有可写入的字段”。
- 验证：JDK 17 下 Generator/System 定向 compile 成功；`BusinessApplicationPageMenuPublishServiceTest`、`BusinessProcessActionExecutorTest`、`BusinessProcessOrchestratorTest` 定向 Maven 测试通过；管理端 in-app-builder Vitest 15 个通过；相关管理端 ESLint 通过。
- H5 Vitest 因项目当前 Vite 无法解析 `vue/compiler-sfc` 未执行成功；未运行生产构建（按用户要求），未启动服务、连接数据库或执行真实菜单/流程/移动端 E2E。
- 追加修复菜单同步编译错误：在遍历菜单 DTO 时补充 `clientCode` 局部变量，供客户端隔离父级解析、资源键和过期菜单判断使用；JDK 17 下 `mvn -pl forge-admin-server -am -DskipTests compile` 通过，`git diff --check` 通过。

## 2026-08-23（挂载客户端菜单隔离修复）

- 修复应用门户导航未按客户端筛选的问题：新增 `portal-navigation-runtime.js`，管理端只保留 `pc` 页面，H5 只保留 `h5` 页面；页面组按当前端的可见子页面递归保留，兼容移动页面挂在旧管理端默认分组下的历史数据。
- 修复系统菜单发布的移动端根目录：`h5` 菜单无外部父级时挂到顶级 `parent_id = 0`，不再复用管理端 `/ai` 父级；菜单发布服务同时按客户端递归保留祖先分组，并支持旧式分组类型。
- 新增并通过管理端定向 Vitest：`./node_modules/.bin/vitest run src/views/app-center/__tests__/portal-navigation-runtime.spec.js`，4 个测试通过。
- 定向 ESLint：`./node_modules/.bin/eslint src/views/app-center/application-portal.vue src/views/app-center/components/portal/portal-navigation-runtime.js src/views/app-center/__tests__/portal-navigation-runtime.spec.js`，通过。
- 后端定向测试：JDK 17 下 `mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests=false -Dtest=BusinessApplicationPageMenuPublishServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`，通过；新增移动端根目录及父分组断言。
- `mvn -q -pl forge-admin-server -am -DskipTests compile` 与 `git diff --check` 通过；未执行生产构建、未启动服务、未连接数据库或外部消息渠道。
- 追加边界修正：发布服务仅为确有当前客户端页面的 `pc/h5` 投影生成应用根菜单，避免“仅移动端页面”在管理端留下空根菜单。
- 复跑 `BusinessApplicationPageMenuPublishServiceTest` 与 `git diff --check`，均通过。

## 2026-08-23（页面发布编辑态的菜单目录作用域）

- 修复用户实际使用的页面设计地址 `/app-center/application/:applicationCode/runtime?...&edit=1`：页面发布面板的管理端父级目录通过 `clientCode=pc` 查询，移动端父级目录通过 `clientCode=h5` 查询，不再把两端菜单混在同一个选择树中。
- 移动端新增独立的 `mobileMenuParentId` 配置并持久化到页面节点及 `settings`；两端同时挂载时可分别选择管理端目录和移动端目录。
- 发布服务读取 `menuParentId`（pc）与 `mobileMenuParentId`（h5），移动端不再误用管理端资源 ID。
- 定向 Vitest：应用构建 schema 与门户菜单投影共 19 个测试通过；`MenuParentSelect`、页面发布面板和相关 schema 通过定向 ESLint。完整 `application-runtime.[applicationCode].vue` ESLint 仍有该文件既有的 18 个 `no-use-before-define/no-unused-vars` 错误，本轮未扩大范围修复。
- 后端 `BusinessApplicationPageMenuPublishServiceTest` 定向测试通过；`git diff --check` 通过。未执行生产构建、服务启动、数据库和浏览器 E2E。
- 追加边界断言：页面显式选择外部移动端目录后，不再同时生成旧应用内分组下的空 H5 目录。

## 2026-08-23（菜单入口切换到正式应用门户）

- 根因：`BusinessApplicationPageMenuPublishService` 将应用根菜单和页面菜单硬编码为 `/app-center/application/{applicationCode}/runtime`，点击菜单会进入页面管理运行页，而不是正式发布门户。
- 初步修复：发布菜单按应用 `portalSlug`（缺省回退 `applicationCode`）生成门户地址；该方案随后根据用户反馈收敛为对象页面独立运行地址。
- 新增 `forge-server/db/migration/V1.0.132__migrate_application_page_menus_to_portal.sql`，现已扩展为按页面对象配置迁移存量资源：对象页面改为独立 CRUD 地址，自定义内容页保留正式门户路径，保留客户端、父级和权限关系。
- 菜单点击保持资源自身的 `path/component`，不在前端把旧 runtime 地址猜测改写成门户地址；对象页面的存量修复统一由 V1.0.132 根据发布快照解析后端生成。
- 验证：JDK 17 下 `BusinessApplicationPageMenuPublishServiceTest` 定向 Maven 测试通过；Node 20.19.0 下菜单导航、门户访问地址组件和配置工具定向 ESLint 通过；门户地址/导航/schema Vitest 23 个通过；V1.0.132 Flyway 占位符扫描和 `git diff --check` 通过。
- 未执行真实数据库 Flyway、服务启动或浏览器 E2E；用户明确要求不做生产构建。
- 追加 H5 回归保护：新发布的 `h5` 页面菜单按页面 `objectRef.configKey` 生成 `/pages/lowcode-runtime?configKey=...&appId=...`，只有 `pc` 资源切换到 `app-center/application-portal`；存量 V1.0.132 迁移限定为 `client_code=pc`。
- 根据用户反馈修正管理端目标：绑定业务对象的页面菜单不再挂到应用门户 `/app/{slug}?pageId=...`，而是发布为单页低代码地址 `/ai/crud-page/{configKey}?pageKey=...&appId=...`；表单页附带 `runtimeOpenMode=CREATE_FORM&mode=create`，自定义内容页才使用门户页面参数兜底。
- V1.0.132 同步增加 JSON_TABLE 存量修复：从已发布应用快照的 `inAppBuilder.nodes[].objectRef` 解析 `configKey/pageKey/pageMode/formKey`，把历史对象页面菜单迁移为独立 CRUD 地址。

## 2026-08-23（菜单资源唯一性与客户端索引修复）

- 修复 `MenuRegisterAdapterImpl` 菜单注册幂等性：注册菜单、应用入口和应用页面菜单均优先按权限码与客户端复用已有资源；历史资源 `resource_type` 变化时使用不限制类型的兼容查询，存在则更新，不再直接插入。
- 新增 `SysResourceMapper.selectOneByPermsAndClientCodeAnyType`、`selectOneByPathAndClientCode`，并兼容历史 `client_code` 为空的管理端资源。
- 新增 `V1.0.133__align_resource_unique_key_with_client_code.sql`：将 `uk_tenant_resource_active` 调整为 `(tenant_id, resource_type, perms, client_code, del_flag)`，使同一页面权限码可分别投影到 pc/h5；迁移具备索引存在性和重复执行保护。
- 新增 `MenuRegisterAdapterImplTest` 回归覆盖：已有资源更新不插入、历史资源类型不一致时更新、pc/h5 同权限码分别创建。
- 静态验证：相关文件 `git diff --check` 通过；未执行 Maven 构建、真实数据库 Flyway 或服务启动（按用户要求不构建）。

## 2026-08-23（页面发布路径导致工作台菜单消失修复）

- 页面发布面板选择管理端/移动端父级时同步写入 `systemMenuVisible=true`，并通过导航草稿自动保存，避免应用发布继续使用旧的未挂载状态。
- 进入应用发布页前若导航草稿仍有改动，先保存服务端草稿再展示发布页，避免发布快照与页面发布路径配置不一致。
- 应用工作台编辑者/有应用编辑权限的用户保留完整页面树，移动端页面不会因管理端 `pc` 客户端过滤而从工作台左侧消失；正式应用门户仍按 `pc/h5` 客户端隔离。
- 定向 Vitest：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && ./node_modules/.bin/vitest run src/views/app-center/__tests__/portal-navigation-runtime.spec.js`，4 个测试通过。
- `git diff --check` 通过。
- 定向 ESLint 对页面运行文件仍报告该文件已有的 17 个 `no-use-before-define/no-unused-vars` 错误；本轮新增代码未引入新的错误类型，未扩大范围修复历史问题。
- 未执行生产构建、后端构建、服务启动、数据库迁移和浏览器 E2E；遵循用户“不用构建”的要求。

## 2026-08-23（保存后流程触发与发布状态修复）

- 根因修复一：动态 CRUD 主子表创建返回 `{main, children}`，原增强触发条件和业务流程开始条件只读取顶层字段；新增 `BusinessEvent.readRecordValue/readPreviousValue`，统一兼容主记录、平铺记录、驼峰和下划线字段。
- 根因修复二：增强触发器在 `@Async` 线程中没有显式恢复事件租户；现用 `TenantContextHolder.executeWithTenant(event.tenantId, ...)` 包住触发器匹配和动作执行。
- 业务流程事件入口继续由 CRUD 创建成功后发布 `RECORD_CREATED` 与 `FORM_SUBMITTED`，只匹配当前对象的已发布 `START_EVENT` 版本，并用事件幂等键避免重复启动。
- 发布状态修复：`BusinessApplicationVersionService.commitImmutable` 在目标不可变版本已存在且快照一致的幂等分支中也执行 `markPublished`，防止恢复/重试发布成功后应用仍停留在 `CHANGED`。
- JDK 17 主代码定向编译成功；默认 JDK 8 首次执行曾报“无效的目标发行版: 17”，切换 `/opt/homebrew/Cellar/openjdk@17/17.0.13` 后通过。
- Generator 模块全测试源码编译受存量问题阻断：`FormulaExecutionEngineTest` 历史文件编码异常，以及 `DynamicCrudRepositoryTest`、`DbAggregateDataProviderTest` 使用旧构造器参数。本轮未修改无关测试。
- 使用测试依赖 classpath 单独编译本轮 3 个测试文件，再执行 `mvn -Dforge.tests.skip=false -Dforge.test.groups= -Dtest=BusinessTriggerExecutorEventTest,BusinessProcessOrchestratorTest,BusinessApplicationVersionServiceTest -DfailIfNoTests=false surefire:test`；9 个测试通过，0 失败、0 错误、0 跳过。
- `git diff --check` 通过。未执行全项目 Maven/Vite 构建、服务重启、数据库迁移或真实流程 E2E，遵循用户“不用构建”的要求。

## 2026-08-23（保存后流程触发与发布状态真实闭环）

- 继续排查真实数据发现 CRUD 配置编码为 `presale_registration_business_object`，而发布流程绑定的标准对象编码为 `business_object`；`BusinessEventPublisher` 原来读取 `ai_crud_config.object_code`，事件因此无法命中流程版本。修复为优先通过 `configKey` 查询 `ai_business_object`，同时补齐 `suiteCode`，仅为旧配置保留 CRUD 回退。
- 发布状态的第二个根因是 `/ai/crud-config/render/{configKey}?designPreview=true` 调用 `prepareRuntimeDraft` 后，派生运行配置保存仍执行 `markObjectChanged`。现将“设计保存”和“预览/发布物化”区分：前者继续传播应用变更，后者保持对象当前设计状态且不把应用重新标成 `CHANGED`。
- 定向测试复用：`BusinessEventPublisherTest` 与 `BusinessApplicationDraftPreviewContractTest` 均通过；相关文件 `git diff --check` 通过。未执行完整 Maven/Vite 构建。
- 启动 8584 隔离服务验证。首次正式发布暴露隔离类路径混用新 `MenuRegisterAdapterImpl` 和旧 system 插件包，抛出 `SysResourceMapper.selectOneByPermsAndClientCode` 的 `NoSuchMethodError`；只为隔离进程制作临时 system 运行 Jar 覆盖对应 Mapper 类/XML，未修改业务源码、未影响 8580 服务。
- 正式发布接口结果：应用 `presale_registration_apply` 从 `designStatus=CHANGED,lastPublishVersion=18` 发布为 `runStatus=SUCCESS,targetVersionNo=20,currentStep=COMMIT`，应用详情变为 `designStatus=PUBLISHED,lastPublishVersion=20`。
- 随后调用 `/ai/crud-config/render/presale_registration_business_object?designPreview=true` 返回成功，再查应用仍为 `PUBLISHED / v20`，确认列表不再出现“有未发布变更”。
- 在 v20 发布态新增动态记录 `id=20`，运行接口返回 `businessKey=business_object:20`、`triggerType=EVENT`、`status=WAITING`、`currentNodeId=approval_mt2533e6_1`，确认“记录新增后自动发起流程”真实生效。
- 清理验证数据：取消本轮及前一轮运行记录 `2091411013778010113`、`2091408295868891138`，状态均为 `CANCELED`；删除动态测试记录 20、19。运行审计记录按平台语义保留。
- 本轮 8584 隔离服务 PID 79219 已正常停止，原有 8580 服务未停止；临时运行类覆盖和本轮认证/健康检查临时文件已清理。
- 最后从原有 8580 服务调用应用分页接口，列表记录 `presale_registration_apply` 返回 `designStatus=PUBLISHED`、`lastPublishVersion=20`、`pageCount=4`，与应用中心外部列表使用的字段一致。
