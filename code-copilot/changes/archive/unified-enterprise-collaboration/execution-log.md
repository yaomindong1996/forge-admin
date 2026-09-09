# 执行日志 — 统一企业协同集成与企业微信全能力接入

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-07-28 | Input | 用户明确企微优先，并要求后续飞书/钉钉复用登录、人员同步、消息和待办能力 | 以通用企业协同连接为目标，不做企微硬编码 |
| 2026-07-28 | Research | 读取三份客户需求、客户实施清单、项目规则、Proposal 模板、安全规则和相关现有变更 | 未修改客户原始需求文档 |
| 2026-07-28 | Research/Social | 复核 `sys_social_config`、`sys_user_social`、Social Controller/Factory/Service 和前端回调 | 确认单平台单租户登录模型、连接维度缺失，以及前端自报第三方身份风险 |
| 2026-07-28 | Research/Directory | 复核 `sys_org/sys_user/sys_user_org/sys_post/sys_user_post` 和现有 DDL | 确认可承载同步结果，但无外部映射、快照、冲突和来源所有权模型 |
| 2026-07-28 | Research/Message | 复核 `MessageChannel/MessageClient/MessageServiceImpl` 和消息三张表 | 确认渠道可插拔，但只支持 WEB/SMS/EMAIL，发送结果不能表达逐人部分失败 |
| 2026-07-28 | Research/Flow | 复核 `FlowTaskEventListener`、`FlowTask`、Flow 权限/幂等和 Job Handler | 确认任务事件可作为投影源；外部调用应异步补偿，不进入 Flowable 事务 |
| 2026-07-28 | Research/Security | 复核 `PersistentCryptoService`、AES 实现和出站安全能力 | 确认存在版本化密文基础，但企微 Secret 上线前仍需认证加密/外部引用门禁 |
| 2026-07-28 | Proposal | 创建 `spec.md`、`tasks.md`、`test-spec.md` 和本日志 | 状态为 propose；未修改生产代码、SQL 或前端 |
| 2026-07-28 | Proposal/Self Review | 对需求、任务、接口、数据和测试执行一致性复核 | 消除重复 App Mapper；拆出 4C 兼容迁移；统一能力绑定事实源；回调改为连接码 + 应用码定位凭据 |
| 2026-07-28 | Customer Plan | 更新客户版完整任务清单 | 增加 COL-01–10 通用协同底座任务，企微作为首个完整适配器，飞书/钉钉后续复用 |
| 2026-07-28 | Reader Test | 使用只读陌生读者复核客户版与技术 Proposal | 发现估算无法对账、一期/二期缺少独立门禁、P0/阶段混用、P2 资料阻塞 M1、P3 和卡片直批边界冲突、COL/WX 重复计价风险 |
| 2026-07-28 | Proposal/Refine | 修复 Reader Test 问题 | 建立 Gate A/B/C 和 M1/M2 独立验收；P2 资料不阻塞 M1；合同测试归 P0/P1；直批改为关闭扩展点；增加 COL/WX/Task/工作量唯一对照 |
| 2026-08-24 | Fix/Social OAuth | 修复授权地址未读取 LOGIN 应用加密 Secret | 授权跳转与授权码换身份统一通过 `decryptAppSecret` 构造非缓存请求，使用后清零 Secret 字符数组；旧连接 Secret 保持兼容回退 |

## 技术决策

| 决策 | 选择 | 放弃方案 | 原因 |
|------|------|----------|------|
| 首个 Provider | 企业微信全能力 | 同时实现企微/飞书/钉钉 | 先形成一个真实闭环，避免三个半成品适配器 |
| 扩展方式 | Provider Registry + 分能力 Connector | 业务服务按 platform switch | 后续平台只新增适配器和合同测试 |
| 模块 | `forge-starter-collaboration` + `forge-plugin-collaboration` | 全塞进 Social/System/Flow | 保持依赖方向，避免业务插件反向污染 Starter |
| 配置根 | 兼容升级 `sys_social_config` | 新建企微专表 | 保留现有登录配置、菜单和用户认知 |
| 企业登录 | 通讯录映射优先、服务端一次性票据 | 登录时自动注册、前端自报 uuid | 避免重复账号和身份伪造 |
| 目录写入 | 只管理连接拥有的数据 | 镜像覆盖全部 Forge 用户/组织/权限 | 降低误删和权限扩大风险 |
| 待办 | Forge 权威任务 + 本地可靠投影 | 企微作为任务主库、监听器同步调外网 | 外网故障不阻断流程，状态可补偿 |
| 调度 | Forge Job Handler | 新建同步调度表 | 复用任务日志、重试、告警和集群调度 |
| 回调路由 | `connectionCode + appCode` | 只按连接选择一套回调凭据 | 支持同连接多物理应用，避免 Callback Token/EncodingAESKey 混用 |

## 验证记录

| 时间 | 范围 | 命令 | 结果 | 警告/跳过 |
|------|------|------|------|-----------|
| 2026-07-28 | Proposal 文档 | `git diff --no-index --check /dev/null <file>`；`rg` 占位符/旧命名/术语扫描；`awk` 代码块计数；`git status --short` | 通过：5 个 文档无空白错误，4 份含代码块文档围栏闭合，旧回调路径和重复 Mapper 无残留 | 本轮 不构建、不启动服务、不连接真实企微/MySQL/Redis；`--no-index` 内容差异退出码 1 为预期 |
| 2026-07-28 | M1 编码（Task 1-13/16-18 一期） | `JAVA_HOME=17 mvn -pl forge-admin-server -am compile -DskipTests`；`npx eslint src/api/collaboration.js src/views/system/collaboration/ --fix` | 后端 BUILD SUCCESS（含 forge-plugin-collaboration 全链路装配）；前端 6 页面 + API 层 ESLint 零错误 | 未执行单元测试与真实企微 UAT（Task 19A 待客户资料）；Flyway V1.0.57-59/V1.0.62 未在真实库执行验证 |
| 2026-08-24 | Social OAuth 应用 Secret 回归 | 先运行 `SocialOAuthLoginServiceTest` 验证 Red；修复后运行该测试及 `SocialAppCredentialServiceTest`；执行 Social Reactor package | Red 在旧实现按预期失败；Green 1/1 通过；扩展定向测试 13/13 通过；模块及依赖 BUILD SUCCESS | 显式启用项目默认跳过的编译/测试；存在原有 deprecation/unchecked/Builder 编译警告；篡改密文失败用例按预期输出一条解密失败日志；未启动服务或调用真实企业微信 |

## 客户输入与外部阻塞

| 项目 | 状态 | 所需内容 | 安全要求 |
|------|------|----------|----------|
| 测试企业 | pending | corpId、测试部门/成员/标签 | 文档只记录脱敏引用 |
| 登录应用 | pending | AgentId/Secret、回调 URL 和授权范围 | Secret 只进入安全配置渠道 |
| 通讯录应用 | pending | Secret、部门/成员/标签读取权限 | 不通过聊天或 Git 提交 |
| 消息/待办应用 | pending | AgentId/Secret、消息/卡片权限 | 测试只向明确测试用户发送 |
| 事件回调 | pending | 公网 HTTPS 域名、可信 IP、Token、EncodingAESKey | Token/Key 不进操作日志 |
| M1 业务规则 | pending | 人员唯一键、权威字段、离职、根部门、岗位、一期 SLA/留存 | 回填 Spec 9.1 后进入 Gate B |
| M2 待办规则 | pending | 待办应用、回调、测试用户、SLA、简单直批可选项 | 回填 Spec 9.2 后进入 Gate C；不阻塞 M1 |
| 容量基线 | pending | 部门/员工/标签/日消息/待办/回调量和 SLA | 仅保存汇总规模，不保存人员明细 |

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| Task 16 页面结构 | index.vue + ConnectionEditor/ApplicationEditor 组件拆分 | 按 V1.0.59 菜单 component 值实现为 connections.vue 单页（详情弹窗内含应用/能力绑定编辑） | 已在 tasks.md Task 16 记录实施偏差；功能验收标准不变 |
| Task 18 能力绑定端点 | 服务层含 bind/unbind/deleteApp | 初版 Controller 未暴露，前端无法闭环 | 已补 3 个端点 + V1.0.62 API 资源与 admin 授权 |
| socialConfig.vue 兼容改造 | 兼容跳转或只读提示 | 已完成：只读列表+详情（Secret 仅显示已配置/掩码）+迁移提示与跳转连接管理；移除新增/编辑/删除写操作，保留刷新缓存 | 旧后端写接口暂保留（有掩码回写保护、无调用方），待 Task 4C 迁移完成后下线 |

## 安全备忘

- 禁止输出、复制或记录真实 Secret、Access Token、Refresh Token、Callback Token、EncodingAESKey、回调解密正文。
- `/social/callback` 当前记录完整 `AuthUser` 并返回前端，且 `/auth/login` 信任客户端第三方身份；该项是 P0 Critical，企微企业登录不得在修复前上线。
- 旧 `sys_social_config.client_secret` 为普通字符串列；迁移必须先盘点和 dry-run，再进行认证加密和比较更新清理。
- 全量同步只有在完整快照成功后才能停用未出现人员/部门；拉取中断不得执行离职/删除逻辑。
- 外部待办回调不得直接更新 `sys_flow_task`，必须调用 Forge 现有权限、状态和幂等受控动作。
