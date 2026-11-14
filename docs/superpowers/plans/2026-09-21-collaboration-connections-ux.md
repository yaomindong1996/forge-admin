# 企业协同连接配置体验优化 Implementation Plan

> **For agentic workers:** This plan is being executed inline in the current session.

**Goal:** 将 `/collaboration/connections` 从“多个技术弹窗拼接的配置页”改成以连接配置状态和用户任务为中心的可理解编辑流程。

**Architecture:** 保留现有连接、应用和能力接口，前端拆出连接配置向导/抽屉与状态展示组件。列表只保留配置和测试两个高频动作，配置视图用基本信息、应用凭据、启用能力三个阶段组织现有字段；依赖字段按能力开关显示，避免改变后端协议。

**Tech Stack:** Vue 3 `<script setup>`、Naive UI、AiCrudPage、现有 collaboration API、Vitest。

---

### Task 1: 拆分连接配置视图并定义用户可读的状态模型

**Files:**
- Create: `forge-admin-ui/src/views/system/collaboration/components/ConnectionSetupPanel.vue`
- Create: `forge-admin-ui/src/views/system/collaboration/components/ConnectionStatusBadge.vue`
- Modify: `forge-admin-ui/src/views/system/collaboration/connections.vue`

- [x] 提取配置弹窗/应用凭据/能力绑定的局部视图，主页面保留页签、列表和 API 编排。
- [x] 增加“待配置、配置不完整、基础信息完整、已停用”等前端展示状态，保持后端 `status` 字段兼容。
- [x] 让应用凭据、能力绑定和连接基础信息共用当前连接详情数据，避免用户在多个弹窗之间来回记忆上下文。

### Task 2: 优化连接列表信息层级和操作入口

**Files:**
- Modify: `forge-admin-ui/src/views/system/collaboration/connections.vue`

- [x] 连接主体列合并平台、连接名称和编码；减少首屏技术字段。
- [x] 增加主要用途和配置状态展示。
- [x] 高频操作调整为“配置、测试”，同步、消息测试、停用和删除收入更多操作。
- [x] 连接编码和企业标识只作为辅助信息，不作为主要编辑对象。

### Task 3: 优化编辑依赖关系与字段文案

**Files:**
- Modify: `forge-admin-ui/src/views/system/collaboration/components/ConnectionSetupPanel.vue`
- Modify: `forge-admin-ui/src/views/system/collaboration/connections.vue`

- [x] 调整平台显示名称文案，编辑时锁定连接编码。
- [x] 待办 H5、同步周期仅在对应能力或开关开启后显示。
- [x] 将默认挂载组织 ID 改为组织选择器，并以字符串提交雪花 ID，避免精度丢失。
- [x] 将“物理应用”改为“平台应用/应用凭据”，将身份策略、目录权威来源改为业务可读文案。
- [x] 保持 Secret 留空保留现值，增加配置缺失提示和保存后的下一步引导。

### Task 4: 增量验证

**Files:**
- Create or modify: `forge-admin-ui/src/views/system/collaboration/__tests__/connections-ux.spec.js`
- Create: `code-copilot/changes/collaboration-connections-ux/test-spec.md`
- Create: `code-copilot/changes/collaboration-connections-ux/execution-log.md`

- [x] 验证连接状态判定、能力依赖字段和编辑只读规则。
- [x] 执行目标 Vitest、目标 ESLint、`pnpm --ignore-workspace build`。
- [x] 执行 `git diff --check` 和新增文件空白检查；不启动真实服务、不修改数据库。
