# UI 细节修复方案（2026-08-19）

> 针对 5 个观感问题，给出根因和具体修复步骤。每条都标明文件路径、行号范围、修复内容。

---

## 问题 1：页面管理主入口左侧导航太丑

### 1.1 去掉「系统页面」「我创建的页面」标题文字

**根因**：`application-runtime.[applicationCode].vue` 第 352-353 行和第 370-371 行硬编码了标题文字，视觉太重。

**修复**：
- 文件：`forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`
- 第 352-353 行：删除 `<div class="navigation-section-label">系统页面</div>`
- 第 370-371 行：删除 `<div class="navigation-section-label">我创建的页面</div>`
- 改用 8px 的 `margin-top` 作为隐式分隔，或者保留极淡的分隔线

### 1.2 系统页面没有图标

**根因**：`page-management.js` 第 1-7 行定义的系统页面只有 `id`、`title`、`view`，没有 `icon` 字段；渲染处（第 361-368 行）只输出 `<span>{{ item.title }}</span>`，没有图标。

**修复**：
- 文件：`forge-admin-ui/src/views/app-center/in-app-builder/page-management.js`
- 给每个系统页面加 `icon` 字段：

```js
export const PAGE_MANAGEMENT_SYSTEM_PAGES = Object.freeze([
  { id: 'system:workbench', title: '个人工作台', view: 'workbench', icon: 'GridOutline' },
  { id: 'system:todo', title: '我的待办', view: 'todo', icon: 'CheckboxOutline' },
  { id: 'system:done', title: '我已办的', view: 'done', icon: 'CheckmarkDoneOutline' },
  { id: 'system:sent', title: '我发送的', view: 'sent', icon: 'PaperPlaneOutline' },
  { id: 'system:cc', title: '抄送我的', view: 'cc', icon: 'PeopleOutline' },
])
```

- 文件：`application-runtime.[applicationCode].vue`
- 第 361-368 行渲染系统页面按钮时，加上图标：

```html
<button class="navigation-page" :class="{ active: ... }" @click="...">
  <span class="navigation-icon-slot" aria-hidden="true">
    <NIcon v-if="item.icon"><component :is="iconMap[item.icon]" /></NIcon>
  </span>
  <span>{{ item.title }}</span>
</button>
```

- 在 `<script setup>` 中导入对应图标并建立 `iconMap`

### 1.3 个人工作台选中态背景蓝色太重

**根因**：`base-app-sidebar__node_selected` 或 `.navigation-page.active` 的背景色使用了主色高透明度，导致蓝色过于明显。

**修复**：
- 文件：`application-runtime.[applicationCode].vue` 的 `<style>` 部分
- 找到 `.navigation-page.active` 或 `base-app-sidebar__node_selected` 的样式
- 把背景从主色高透明度改为 `#f0f2f5`（浅灰），选中文字色改为主色
- 或者只保留左侧 3px 竖条 + 文字加粗，不用大面积背景色

---

## 问题 2：待办/已办页面样式乱 + 头部按钮不统一

### 2.1 系统页面头部多余

**根因**：`PageManagementSystemView.vue` 第 3-6 行，每个系统页面渲染时都有 `<header>` 显示标题和「系统页面固定置顶，不参与用户页面排序」这句话，用户不知道这句话的作用。

**修复**：
- 文件：`forge-admin-ui/src/views/app-center/components/portal/PageManagementSystemView.vue`
- 删除 `<header>` 整块（第 3-6 行），或者只保留标题，删掉说明文字
- 把标题改为页面级标题样式，不要用 header 卡片

### 2.2 头部按钮太多、样式不统一

**根因**：`application-runtime.[applicationCode].vue` 第 37-159 行，头部有 9 个按钮（撤销、重做、页面资源、保存草稿、预览草稿、运行应用、设置、发布、更多）。设置用 `secondary`，发布用 `type="primary"`，颜色突兀。

**修复**：
- 文件：`application-runtime.[applicationCode].vue`
- 精简头部按钮，非编辑态只保留 3 个：运行应用、设置、发布
- 编辑态保留：保存草稿、预览、设置、发布
- 把撤销/重做/页面资源/更多合并到一个下拉菜单「更多」里
- 设置按钮改为 `quaternary`（和运行应用一样），只有发布用 `type="primary"`

```html
<div v-if="!formDesignerMode" class="runtime-header-actions">
  <!-- 编辑态：撤销/重做/页面资源 收到下拉 -->
  <n-dropdown v-if="editing" trigger="click" :options="moreOptions" placement="bottom-end">
    <n-button quaternary circle><NIcon><EllipsisHorizontalOutline /></NIcon></n-button>
  </n-dropdown>
  <n-button v-if="editing" secondary @click="saveCurrentDesignerSection">保存草稿</n-button>
  <n-button v-if="editing" secondary @click="openDraftPreview">预览</n-button>
  <n-button quaternary @click="openApplicationPortal">运行应用</n-button>
  <n-button quaternary @click="openApplicationSettings">设置</n-button>
  <n-button type="primary" @click="openApplicationPublish">发布</n-button>
</div>
```

---

## 问题 3：发布/设置不要跳转，改为当前页面内打开

**根因**：`openApplicationPublish`（第 4542-4547 行）和 `openApplicationSettings`（第 4532-4540 行）都用 `router.push` 跳转到独立路由，导致页面来回跳转。

**修复**：改为抽屉方式，在当前页面内打开。

### 3.1 设置改为抽屉

- 文件：`application-runtime.[applicationCode].vue`
- 修改 `openApplicationSettings` 函数：

```js
const settingsDrawerVisible = ref(false)
const settingsDrawerSection = ref('basic')

function openApplicationSettings(section = 'basic') {
  settingsDrawerSection.value = section
  settingsDrawerVisible.value = true
}
```

- 在模板末尾加抽屉：

```html
<n-drawer v-model:show="settingsDrawerVisible" :width="720" placement="right">
  <n-drawer-content title="应用设置" closable>
    <AppSettingsBasic v-if="settingsDrawerSection === 'basic'" v-model="settingsModel" />
    <AppSettingsAccess v-else-if="settingsDrawerSection === 'access'" v-model="settingsModel" />
    <!-- 其他 section 同理 -->
  </n-drawer-content>
</n-drawer>
```

- 抽屉内放一个 section 切换 tab + 保存按钮

### 3.2 发布改为抽屉

- 同理修改 `openApplicationPublish`：

```js
const publishDrawerVisible = ref(false)

function openApplicationPublish() {
  publishDrawerVisible.value = true
}
```

```html
<n-drawer v-model:show="publishDrawerVisible" :width="640" placement="right">
  <n-drawer-content title="应用发布" closable>
    <AppPublishStatusCard :application="application" @publish="..." />
    <AppPublishAccess :application="application" />
    <AppPublishVersionHistory :application="application" @changed="..." />
  </n-drawer-content>
</n-drawer>
```

### 3.3 设置/发布组件复用

- 把 `application-settings.vue` 和 `application-publish.vue` 的内容拆成独立子组件（如果还没拆的话），在抽屉中直接引用
- 独立路由可以保留（兼容直接访问），但默认入口改为抽屉

---

## 问题 4：表单设计器样式丑

### 4.1 顶部蓝色太重

**根因**：`application-runtime.[applicationCode].vue` 第 6906-6914 行，`.application-form-object-icon` 的背景用了 `color-mix(in srgb, var(--n-primary-color, #3370ff) 10%, transparent)`，加上 `--n-primary-color` 默认蓝色，整体看偏蓝。

**修复**：
- 文件：`application-runtime.[applicationCode].vue` 的 `<style>` 部分
- 把 `.application-form-object-icon` 背景改为 `#f0f2f5`（浅灰），图标颜色改为 `#4e5969`
- 整个 `.application-form-object-bar` 背景保持白色，不要蓝色

### 4.2 按钮不对齐

**根因**：第 6876-6884 行，`.application-form-object-bar` 用 grid 四列，但列宽不匹配内容，导致对不齐。

**修复**：
- 简化为 flex 布局：

```css
.application-form-object-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #e5e6eb;
  padding: 10px 24px;
  background: #fff;
}
.application-form-object-bar label {
  display: flex;
  align-items: center;
  gap: 6px;
}
.application-form-object-bar label > span {
  white-space: nowrap;
}
```

### 4.3 「对象编码」用户看不懂

**根因**：第 307 行 `<span>对象编码</span>`，技术术语对用户不友好。

**修复**：
- 改为 `<span>数据表名</span>`
- 「对象名称」改为「数据表名称」或保持「对象名称」但加 tooltip 说明
- 或者把对象信息栏整体折叠，默认只显示「数据对象：customer（客户管理）」，点击展开编辑

### 4.4 只有「保存并返回」，没有单独保存

**根因**：第 289-291 行，只有一个 `type="primary"` 按钮，文案是「保存并返回页面管理」或「保存表单」。

**修复**：
- 增加一个「保存」按钮（不返回），保留「保存并返回」：

```html
<n-space size="small">
  <n-button secondary @click="returnToPageDesigner">返回</n-button>
  <n-button secondary :loading="saving" :disabled="!dirty" @click="saveActiveFormDesigner(false)">保存</n-button>
  <n-button type="primary" :loading="saving" :disabled="!dirty" @click="saveActiveFormDesigner(true)">保存并返回</n-button>
</n-space>
```

- 修改 `saveActiveFormDesigner` 接受 `returnAfter` 参数

### 4.5 保存超时

**根因**：后端 `POST /ai/business/application/{id}/design-page` 在保存元数据后执行 MySQL DDL（建表/加字段），DDL 执行慢导致超时。

**修复**：
- 后端：把 DDL 改为异步执行，保存接口只保存元数据，DDL 放到后台任务
- 或者：增大接口超时时间，前端 `request` 加 `timeout: 60000`
- 前端：保存按钮加 loading 状态和进度提示「正在创建数据表，请稍候...」

---

## 问题 5：点击页面提示「低代码应用尚未发布」

**根因**：非编辑态下点击用户页面，前端调用运行时接口，后端检查应用发布状态，未发布时返回错误。

**修复**：
- 文件：`application-runtime.[applicationCode].vue`
- 在非编辑态点击页面时，应该用草稿态数据渲染预览，而不是调用运行时接口
- 或者报错时给出引导而不是纯错误提示：

```js
// 在 catch 或 error 处理中
if (error?.message?.includes('尚未发布')) {
  message.warning('应用尚未发布，正在使用草稿预览', {
    action: () => openApplicationPublish(),
    actionText: '去发布'
  })
  // 回退到草稿态渲染
}
```

- 更好的做法：页面管理主入口的右侧预览区，始终用草稿态 Schema 渲染，不依赖运行时接口

---

## 修复优先级

| 优先级 | 问题 | 修复难度 | 影响面 |
|---|---|---|---|
| P0 | 3. 设置/发布改为抽屉不跳转 | 中 | 交互体验根本改善 |
| P0 | 2.2 头部按钮精简统一 | 低 | 视觉立即改善 |
| P0 | 1.1 去掉系统页面标题 | 低 | 视觉立即改善 |
| P0 | 1.2 系统页面加图标 | 低 | 视觉立即改善 |
| P1 | 4.3 对象编码改名 | 低 | 理解性改善 |
| P1 | 4.4 增加单独保存按钮 | 低 | 操作改善 |
| P1 | 4.1 顶部去蓝 | 低 | 视觉改善 |
| P1 | 2.1 系统页面头部去掉 | 低 | 视觉改善 |
| P1 | 5. 未发布提示改为草稿预览 | 中 | 不阻塞使用 |
| P2 | 4.5 保存超时 | 中 | 后端改造 |
| P2 | 4.2 按钮对齐 | 低 | 细节 |
| P2 | 1.3 选中态蓝色 | 低 | 细节 |

---

## 执行建议

1. **先做 P0 的 4 项**（去标题、加图标、精简按钮、设置/发布改抽屉），这 4 项改动小但视觉改善最大
2. **再做 P1**（对象编码改名、加保存按钮、去蓝、未发布提示）
3. **最后做 P2**（保存超时需要后端配合）

P0 的 4 项可以一次性改完，改完后面貌会有质的变化。
