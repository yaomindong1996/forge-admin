# 流程通知矩阵增量验证

## P0

- Flow 插件使用 JDK 17 编译通过，覆盖 `FlowTaskNotifyListener` 的事件分发签名和消息依赖。
- Flow Server 使用 JDK 17 编译通过，覆盖通知渠道查询 Controller 的依赖装配。
- 前端 `forge-admin-ui` 使用 Node 20.19.0 完成 build，覆盖模型矩阵 Vue/API 代码。
- V1.0.128/V1.0.129 迁移通过 `git diff --check`、Flyway placeholder 和重复版本静态检查。

## P1

- `FlowNotifyConfig` 解析空值、非法 JSON、事件渠道和模板覆盖。
- `FlowNotifyConfig` 对新待办补齐 WEB、过滤抄送 SMS，并忽略未知渠道。
- 新待办未配置时保持站内信 + 连接开关控制的协同卡片；显式渠道按 WEB/EMAIL/SMS/COLLABORATION 独立幂等键分发。
- 审批结果和通过抄送按模型配置发送，流程变量进入消息模板参数。
- 前端只序列化用户明确配置的事件，避免编辑 todo 时意外开启 result/cc。

## 本轮增量验证

本轮仅针对 Spec 中后端未编译、前端未实现和结果/抄送模板迁移三个缺口执行；不启动真实数据库、Flow 服务或外部协同连接。

## 最终审查增量

- 增加异步租户上下文审查：模板查询、待办接收人解析、抄送角色解析必须携带业务租户。
- 增加渠道接口协议审查：通知渠道查询接口必须与流程 API 使用同一加解密协议。
- 复跑定向单测、Flow 插件/Flow Server 编译、前端 ESLint 和生产构建。
- 保留真实 Flyway、服务启动、外部消息渠道和浏览器 E2E 为集成环境验收，不在本地伪造通过结论。

## 2026-08-23 挂载客户端菜单增量验证

- 应用门户导航按 `mountTarget` 投影：管理端仅返回 `ADMIN/BOTH`，H5 仅返回 `MOBILE/BOTH`；移动端页面嵌套在旧管理端默认分组下时保留父级分组。
- 系统菜单发布的 H5 根资源固定挂到顶级 `parent_id = 0`，不再挂到管理端 `/ai` 目录，避免 H5 客户端过滤父级后整棵菜单树消失。
- 新增门户导航投影 Vitest，覆盖客户端隔离、跨端页面组父级保留和空分组移除。
- 未执行生产构建、真实数据库、服务启动和浏览器 E2E；按用户要求保留现有工作区运行态由环境验收。

## 2026-08-23 页面设计编辑态增量验证

- 页面发布面板的父级目录请求按客户端隔离：管理端传 `clientCode=pc`，移动端传 `clientCode=h5`。
- 两端同时挂载时分别保存 `menuParentId` 与 `mobileMenuParentId`，发布服务按对应客户端读取。
- 页面 schema 与门户导航定向 Vitest 共 19 个测试通过；相关新增/修改组件定向 ESLint 通过。
- 完整运行页文件仍有历史 lint 错误，未在本轮扩大修复范围；未执行生产构建和真实端到端验证。

## 2026-08-23 菜单正式门户入口增量验证

- `BusinessApplicationPageMenuPublishServiceTest` 定向 Maven 测试通过：应用根/自定义内容页使用门户地址；绑定业务对象的页面写入 `/ai/crud-page/{configKey}` 独立运行地址，页面组件为 `ai/crud-page`；未配置对象运行配置时才回退 `/app/{portalSlug}?pageId=...`。
- 管理端定向 ESLint：`src/composables/useMenu.js`、门户访问地址组件和配置工具通过；菜单导航不再对页面发布目标做前端二次改写，目标完全由发布资源的 `path/component` 决定。
- 管理端定向 Vitest：门户地址、门户导航与应用构建 schema 共 23 个测试通过，新增对象表单页独立运行地址断言。
- V1.0.132 迁移完成 Flyway 占位符静态扫描与 `git diff --check`；未执行真实数据库迁移。
- 未执行生产构建、服务启动和浏览器 E2E；按用户要求保留真实菜单刷新/数据库验收到运行环境。
- 追加 H5 菜单回归：移动端页面资源使用 `/pages/lowcode-runtime?configKey=...` 作为 path/component，避免门户组件被 H5 客户端误解析。
- 追加管理端单页回归：绑定业务对象的页面资源使用 `/ai/crud-page/{configKey}` 独立运行地址和页面参数；只有自定义内容页没有独立 CRUD 配置时才回退到 `/app/{portalSlug}?pageId=...`。

## 2026-08-23 菜单资源唯一性增量验证

- `MenuRegisterAdapterImplTest` 覆盖页面菜单已存在时只更新、不新增；历史 `resource_type` 不一致时仍复用；pc/h5 同权限码在客户端隔离索引下可分别创建。
- `V1.0.133__align_resource_unique_key_with_client_code.sql` 静态检查索引防重复、历史空客户端归一化和 Flyway 无占位符。
- `git diff --check` 通过；未执行 Maven 构建、真实数据库迁移和运行态 E2E。

## 2026-08-23 页面发布路径增量验证

- 页面发布路径变更覆盖：父级选择自动打开 `systemMenuVisible`、导航草稿自动保存、进入发布面板前保存草稿，以及工作台编辑权限下保留移动端页面。
- 已执行门户导航投影定向 Vitest：4 个测试通过。
- 已执行 `git diff --check`。
- 页面运行文件的完整 ESLint 仍受历史 17 个错误阻断；未执行生产构建（遵循用户要求），真实发布菜单、角色权限和数据库快照需在集成环境验收。

## 2026-08-23 保存后流程触发与发布状态增量验证

- 动态 CRUD 的业务事件字段读取同时覆盖平铺记录和 `{main, children}` 聚合记录，并兼容驼峰/下划线字段；增强条件、变量映射、标题模板和业务流程开始条件保持一致。
- 异步增强触发器按事件自带 `tenantId` 恢复租户上下文，避免线程池中触发器查询和动作执行串租户或查不到配置。
- 已发布 `START_EVENT/RECORD_CREATED` 流程使用主记录条件成功匹配并创建业务流程运行记录。
- 应用不可变版本幂等提交时重新调用 `markPublished`，确保应用表的 `design_status` 与 `last_publish_version` 收敛到发布结果。
- JDK 17 定向 JUnit 共 9 个测试通过，0 失败；相关主代码静态编译通过，`git diff --check` 通过。
- 未执行全项目构建、服务重启、真实数据新增/Flowable 启动和数据库 E2E；按用户要求保留给运行环境验收。

## 2026-08-23 保存后流程触发与发布状态真实回归

- 业务事件必须按 `configKey` 从 `ai_business_object` 解析标准 `objectCode/suiteCode`；仅在旧配置没有业务对象时回退 `ai_crud_config`，确保事件对象编码能命中已发布流程的 `subject_object_code`。
- 正式发布应用后检查 `designStatus=PUBLISHED`；随后调用 `designPreview=true` 物化运行草稿，再次检查仍为 `PUBLISHED`，防止预览派生保存重新传播 `CHANGED`。
- 在最新应用发布版本下通过动态 CRUD 新增记录，检查 `ai_business_process_run` 对应接口返回 `triggerType=EVENT`、`status=WAITING`、标准 `businessKey` 和审批节点。
- 收尾取消本轮流程运行并删除测试业务记录；不删除运行审计记录，保留 `CANCELED` 状态用于追溯。
- 遵循用户要求，不执行全项目 Maven/Vite 构建；复用此前定向 JUnit 结果，并补做相关文件 `git diff --check`。
