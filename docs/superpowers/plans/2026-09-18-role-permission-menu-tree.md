# 角色授权菜单目录树 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让角色授权左侧严格对应菜单目录层级，隐藏功能页保留授权能力但明确标识为“导航隐藏”。

**Architecture:** 保留现有资源权限模型和保存协议，仅调整前端展示模型。资源树转换继续产出页面权限数据，再由独立导航组件把目录模块、嵌套目录和页面入口渲染为可展开树；目录选择展示后代页面，页面选择展示单页。应用中心传入的扁平权限模块继续兼容。

**Tech Stack:** Vue 3 `<script setup>`、Naive UI `NTree`、Vitest、现有 Forge CSS 变量。

---

### Task 1: 建立权限导航树模型

**Files:**
- Create: `forge-admin-ui/src/views/system/components/role-permission-navigation.js`
- Modify: `forge-admin-ui/src/views/system/components/RolePermissionSettings.vue:605-753`
- Test: `forge-admin-ui/src/views/system/__tests__/role-permission-navigation.spec.js`

- [ ] 增加目录/页面树转换、搜索过滤、树节点查找和页面收集函数。
- [ ] 为资源菜单页面保存 `navigationHidden` 标记；不改变 `resourceIds`、选中和保存协议。
- [ ] 让嵌套目录保留 `parentKey`，空目录只要有后代页面仍可作为导航节点。
- [ ] 用测试覆盖嵌套目录、页面叶子、隐藏页面标记和搜索保留父节点。

### Task 2: 拆分左侧导航组件

**Files:**
- Create: `forge-admin-ui/src/views/system/components/RolePermissionNavigation.vue`
- Modify: `forge-admin-ui/src/views/system/components/RolePermissionSettings.vue:1-82,470-604,1275-1479`

- [ ] 使用 `NTree` 展示菜单目录层级，目录和页面使用不同图标/标签。
- [ ] 目录节点显示授权状态，页面节点显示“导航隐藏”标记。
- [ ] 保留搜索、全局授权、骨架屏和状态图例。
- [ ] 通过单层 props/emit 传递选中节点和批量操作，不引入额外跨层 props 链。

### Task 3: 接入目录/页面选择语义

**Files:**
- Modify: `forge-admin-ui/src/views/system/components/RolePermissionSettings.vue:516-603,937-1218`
- Modify: `forge-admin-ui/src/views/system/components/RolePermissionSettings.vue:258-300,121-195`

- [ ] 目录节点选择展示该目录所有后代页面，页面节点选择只展示对应页面。
- [ ] 当前批量授权、展开/折叠、状态统计作用于当前目录范围；全局操作保持不变。
- [ ] 将按钮文案从“模块全选/清空”调整为“目录全选/清空”，避免与真实菜单结构冲突。
- [ ] 页面卡片对隐藏入口显示“导航隐藏”，普通入口保持原标签。

### Task 4: 增量验证与记录

**Files:**
- Create: `code-copilot/changes/system-user-sort-and-role-checkall/tasks.md`
- Create: `code-copilot/changes/system-user-sort-and-role-checkall/test-spec.md`
- Modify: `code-copilot/changes/system-user-sort-and-role-checkall/execution-log.md`

- [ ] 执行目标 Vitest、ESLint、`git diff --check` 和前端构建。
- [ ] 记录命令、结果和未执行的浏览器验证原因。
- [ ] 更新本轮任务状态，不修改既有后端授权口径。
