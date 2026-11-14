# Domain Knowledge

<!-- 业务域：系统做什么，而不是代码怎么分层。架构见根 ARCHITECTURE.md。
     术语表未单独沉淀前不创建 CONTEXT.md；勿在此臆造 glossary。 -->

## Core Workflows

### 管理员登录与授权访问
- **Trigger:** 用户在管理端提交账号密码（或社交/扫码等已接入方式）登录。
- **What happens:** 认证签发 Token；加载用户、角色与资源（菜单/按钮/API）；后续请求经 Sa-Token 与权限注解校验；列表类查询可再经数据权限改写。
- **Outcome:** 用户进入有权菜单与接口范围，无权资源不可见或不可调用。
- **Key models:** `SysUser`、`SysRole`、`SysResource`、会话/Token（Sa-Token）；相关表如 `sys_user`、`sys_role`、`sys_resource`、`sys_role_resource`。

### 多租户数据隔离
- **Trigger:** 任意租户上下文下的业务读写。
- **What happens:** 租户拦截器为业务 SQL 追加当前 `tenant_id`；内置/种子业务数据落在默认租户 `1`；部分系统资源表（如菜单资源）按既有约定不受租户行拦截。
- **Outcome:** 租户之间业务数据隔离；错误的 `tenant_id=0` 会导致数据对租户不可见。
- **Key models:** 租户实体/配置（`SysTenant` 等）、带 `tenant_id` 的业务表。

### 组织与行政区划数据范围
- **Trigger:** 用户按组织或区划筛选业务列表。
- **What happens:** 组织树与 `region_code` 参与查询；虚拟区划节点 code 以 `ALL` 结尾表示「本级 + 下级」聚合（见 `docs/references/region-query.md`）。
- **Outcome:** 用户只看到授权范围内的业务行。
- **Key models:** `SysOrg`、`sys_region_code`、业务表上的 `region_code` / 组织关联字段。

### 字典驱动的可配置枚举
- **Trigger:** 页面下拉、状态标签、可配置枚举展示或录入。
- **What happens:** 类型与数据维护在 `sys_dict_type` / `sys_dict_data`；前端经 `useDict` / `DictSelect` / `DictTag` 消费；后端业务状态仍用 Java 枚举码，与 `dict_value` 对齐。
- **Outcome:** 展示文案可配置，存储码与后端枚举一致，避免页面硬编码 options。
- **Key models:** `SysDictType`、`SysDictData`。

### 低代码 / AI 业务应用与 CRUD
- **Trigger:** 在生成器/低代码设计器中建模、发布或运行业务应用/页面。
- **What happens:** 设计态维护模型与页面 schema；生成或运行时走动态 CRUD/编排（`forge-plugin-generator` 等）；前端 lowcode-builder / AiForm / AiCrudPage 与后端生成器、动态持久化协作。
- **Outcome:** 租户可获得可配置的业务页面与接口，而不必每次手写全套 CRUD。
- **Key models:** 业务应用/模型/页面相关实体（如 `AiBusinessApp` 及生成器域实体）；运行时动态表/记录（以插件实现为准）。
- **Review:** 具体产品名词以运行中的菜单与 `forge-plugin-generator` 域名为准，未单独建 CONTEXT 前勿把营销名当作规范术语。

### 业务流程（审批）
- **Trigger:** 业务单据提交审批，或在流程设计器发布/绑定流程。
- **What happens:** Flowable 模型设计与部署；业务侧通过流程绑定/启动/回调注解或 API 驱动流转；待办/已办在流程 UI 中处理。
- **Outcome:** 单据进入审批状态机，通过/驳回等结果回写业务。
- **Key models:** 流程定义/实例/任务（Flowable `ACT_*` + Forge 流程插件封装）；业务侧绑定配置。

### 消息通知
- **Trigger:** 系统事件、审批、任务或运营配置触发通知。
- **What happens:** 消息中心按渠道（站内信/邮件/短信等）投递；与权限/用户定位相关。
- **Outcome:** 目标用户收到可读通知或外部投递结果落库。
- **Key models:** `forge-plugin-message` 域内消息/模板/投递记录实体。

### 定时任务与作业配置
- **Trigger:** 调度到期或管理员在作业配置中触发。
- **What happens:** Job 插件/starter 执行已配置任务；日志留存；部分清理类任务可按策略物理清理历史日志（主数据删除仍走逻辑删除约定）。
- **Outcome:** 批处理完成并留下可审计日志。
- **Key models:** `SysJobConfig`、`SysJobLog` 等。

### 文件上传与受控访问
- **Trigger:** 表单上传图片/附件或列表回显。
- **What happens:** 存储返回 fileId；展示走鉴权组件（如 `AuthImage`）与受控 URL，而非裸公网直链。
- **Outcome:** 文件可按权限下载/预览。
- **Key models:** 文件元数据/存储策略（`forge-starter-file`）；字典如 `sys_file_storage_type`。

## Domain Relationships

- 一个**用户**可拥有多个**角色**；角色绑定多个**资源**（菜单/按钮/API）。
- **租户**隔离绝大多数业务表行；用户/组织通常隶属租户上下文。
- **组织**与**行政区划**共同约束数据可见范围；虚拟区划 `*ALL` 表示聚合范围。
- **字典类型**包含多条**字典数据**；前端展示依赖字典，后端状态机依赖枚举码，二者通过 `dict_value` 对齐。
- **业务应用/模型**（低代码）可绑定**页面 schema** 与可选**流程**；运行时记录属于租户。
- **流程实例**关联业务单据；任务办理人来自用户/角色/候选人策略（以流程插件配置为准）。
- <!-- review: 协作、打印、外部 API、MCP 等扩展域关系待产品确认后补全，勿臆造 -->

## Regulatory / Compliance Context

- 禁止硬编码密钥、AK/SK、数据库密码；禁止日志输出手机号、身份证、银行卡等个人信息。
- API Key/Secret 返回前端必须脱敏；涉及资金、状态流转、权限变更的改动须在 Spec 中标注并经人工审查。
- 外部调用必须超时与降级；开源模板不得混入企业版源码或 `edition=ee` 标记。
- 逻辑删除与审计：用户可见主数据/配置/单据默认逻辑删除，以满足恢复与审计预期；例外场景须在 Spec 说明。
