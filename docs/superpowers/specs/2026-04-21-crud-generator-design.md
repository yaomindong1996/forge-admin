# AI CRUD 配置生成器设计文档

> 创建日期：2026-04-21
> 变更名称：ai-crud-generator-ui
> 设计状态：pending-approval

## 一、背景与目标

### 1.1 现状问题

当前 `/ai/crud-config` 页面的 AI 生成功能仅通过简单弹窗实现：
- 用户填写 configKey + 描述/表名，点击生成后等待结果
- 无流式输出，无法看到生成过程
- 无法编辑预览结果，直接保存
- 无会话历史，无法追溯生成过程

### 1.2 目标

打造类似豆包/Cursor 的专业 AI CRUD 配置生成器体验：
- 左右分栏布局：左侧对话+配置区，右侧代码预览区
- 流式分阶段输出：用户清晰感知生成进度
- 实时编辑预览：生成结果可在线编辑后再保存
- 会话历史管理：支持查看历史生成会话

## 二、整体架构

### 2.1 新增路由

| 路由 | 页面 | 说明 |
|------|------|------|
| `/ai/crud-generator` | `crud-generator.vue` | AI CRUD 配置生成器主页面 |

### 2.2 页面布局

三栏布局结构：

```
┌─────────────────────────────────────────────────────────────────────┐
│  AI CRUD 配置生成器                                    [Provider] [Model] │
├──────────┬─────────────────────────────────┬──────────────────────────┤
│ 会话历史 │ 配置区                          │ 文件树                   │
│ (200px)  │ ┌─────────────────────────────┐ │ (400px)                  │
│          │ │ configKey: [____________]   │ │                          │
│ ○ 会话1  │ │ tableName: [____________]   │ │ □ search-schema.json    │
│ ○ 会话2  │ │ [温度: 0.3] [MaxTokens:4k] │ │ □ columns-schema.json   │
│ + 新会话 │ └─────────────────────────────┘ │ □ edit-schema.json      │
│          │ 对话区                          │ □ api-config.json       │
│          │ ┌─────────────────────────────┐ │                          │
│          │ │ [AI]: 正在分析表结构...      │ │ ┌────────────────────────┐│
│          │ │ [AI]: 正在生成 searchSchema..│ │ │ 当前文件内容           ││
│          │ │ [用户]: 生成员工管理页面     │ │ │ (JSON + 可编辑)        ││
│          │ │                              │ │ │                        ││
│          │ │                              │ │ │                        ││
│          │ │                              │ │ └────────────────────────┘│
│          │ └─────────────────────────────┘ │                          │
│          │ ┌─────────────────────────────┐ │ [查看Diff] [保存配置]   │
│          │ │ [输入框________________] [发送]│ │                          │
│          │ └─────────────────────────────┘ │                          │
└──────────┴─────────────────────────────────┴──────────────────────────┘
```

### 2.3 页面入口

- 原 `crud-config.vue` 工具栏的"AI 生成配置"按钮改为跳转到 `/ai/crud-generator`
- AI 管理菜单新增"CRUD 生成器"菜单项（sort: 11，在 CRUD 配置之后）

### 2.4 复用资源

| 资源 | 复用方式 |
|------|----------|
| 会话历史 | 复用 `ai_chat_session`/`ai_chat_record` 表 + `/ai/admin/session` 相关接口 |
| 流式输出 | 复用 `/ai/client/stream` SSE 接口 + `AiClient` |
| Provider/Model | 复用 `modelListByProvider` 接口 |
| CRUD 配置保存 | 复用 `crudConfigAdd`/`crudConfigUpdate` 接口 |

## 三、左侧会话历史区

### 3.1 功能

- 显示历史会话列表，按时间倒序排列
- 每个会话项显示：第一句用户输入（截断 30 字符） + 创建时间
- 点击切换到对应会话，加载历史消息
- 底部"新建会话"按钮

### 3.2 交互

- 当前会话高亮显示（左侧蓝色边框）
- 会话项右侧有删除按钮（hover 显示）
- 新建会话时：
  - 清空对话区消息
  - 保留配置区输入（configKey/tableName）
  - 清空右侧预览区文件内容

### 3.3 数据来源

| 操作 | 接口 |
|------|------|
| 获取历史会话列表 | `GET /ai/admin/session/page` |
| 获取会话消息 | `GET /ai/admin/session/{sessionId}/messages` |
| 创建新会话 | `POST /ai/admin/session/create`（需确认是否存在，若无则新增） |
| 删除会话 | `DELETE /ai/admin/session/{sessionId}` |

## 四、中间对话+配置区

### 4.1 顶部配置区

| 配置项 | 类型 | 说明 |
|--------|------|------|
| configKey | 输入框 | 必填，小写字母开头+数字+下划线，2-64字符 |
| tableName | 输入框 | 可选，用于表结构生成模式，输入后自动查询表字段 |
| Provider | 下拉 | 可选，指定 AI 供应商 |
| Model | 下拉 | 可选，指定模型 |
| 温度 | 滑块 | 0-1，默认 0.3 |
| MaxTokens | 输入 | 默认 4096 |

### 4.2 对话区

**消息列表**：
- 用户消息：右侧蓝色气泡
- AI 消息：左侧灰色气泡，支持 Markdown 渲染（代码块语法高亮）
- 流式输出时显示"正在思考..."动画 + 打字机光标效果

**流式分阶段输出显示**：
- `analyzing`："正在分析需求..."
- `generating-search`："正在生成 searchSchema..."（实时输出 JSON）
- `generating-columns`："正在生成 columnsSchema..."
- `generating-edit`："正在生成 editSchema..."
- `generating-api`："正在生成 apiConfig..."
- `complete`："生成完成！"

### 4.3 底部输入区

- 多行输入框（支持 Shift+Enter 换行）
- 发送按钮（Enter 快捷发送）
- 输入提示文案："描述你需要的 CRUD 页面功能，如：员工管理，包含姓名、部门、职位..."

## 五、右侧代码预览区

### 5.1 文件树

**文件节点**：
- `search-schema.json`（搜索配置）
- `columns-schema.json`（表格列配置）
- `edit-schema.json`（编辑表单配置）
- `api-config.json`（API 配置）

**交互**：
- 当前文件高亮显示
- 点击切换显示对应文件内容
- 文件名旁显示状态图标：
  - 空（灰色空心圆）
  - 已生成（绿色实心圆）
  - 有变更（黄色实心圆，用户编辑过）

### 5.2 代码编辑器

**功能**：
- JSON 语法高亮 + 自动格式化
- 支持在线编辑（用户可微调生成的配置）
- 行号显示 + 代码折叠
- 编辑器选择：Monaco Editor（VS Code 同款，功能完整）

### 5.3 Diff 对比功能

**触发条件**：configKey 对应的配置已存在时显示"查看 Diff"按钮

**展示方式**：
- 弹窗显示左右对比视图
- 左侧：现有配置（从数据库加载）
- 右侧：AI 生成配置
- 红色标记删除，绿色标记新增

### 5.4 底部操作按钮

| 按钮 | 功能 |
|------|------|
| 保存配置 | 校验 configKey + JSON 格式，调用 `crudConfigAdd` 或 `crudConfigUpdate` |
| 复制 JSON | 复制当前文件内容到剪贴板 |
| 导出全部 | 导出完整配置为单 JSON 文件下载 |

## 六、后端接口设计

### 6.1 新增接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/ai/crud-config/ai/stream-generate` | POST SSE | 流式生成 CRUD 配置 |

### 6.2 流式生成接口请求体

```json
{
  "sessionId": "可选-关联会话ID",
  "configKey": "employee_manage",
  "tableName": "可选-表名",
  "description": "员工管理页面，包含姓名、部门...",
  "providerId": "可选-供应商ID",
  "modelId": "可选-模型ID",
  "temperature": 0.3,
  "maxTokens": 4096
}
```

### 6.3 流式输出事件格式

**事件类型**：

| event | 说明 |
|-------|------|
| `progress` | 进度阶段更新 |
| `chunk` | JSON 内容增量输出 |
| `complete` | 生成完成，返回完整结果 |
| `error` | 错误信息 |

**事件示例**：

```
event: progress
data: {"stage": "analyzing", "message": "正在分析需求..."}

event: progress
data: {"stage": "generating-search", "message": "正在生成 searchSchema..."}

event: chunk
data: {"file": "searchSchema", "content": "[{\"field\": \"name\", "}

event: chunk
data: {"file": "searchSchema", "content": "\"label\": \"姓名\"}"}

event: progress
data: {"stage": "generating-columns", "message": "正在生成 columnsSchema..."}

event: complete
data: {"configKey": "employee_manage", "tableName": "employee", "tableComment": "员工表", "searchSchema": "...", "columnsSchema": "...", "editSchema": "...", "apiConfig": "..."}
```

### 6.4 Agent 配置

复用现有 `crud_config_builder` Agent，增强 System Prompt：

- 输出分阶段进度标记
- JSON 结构按 searchSchema/columnsSchema/editSchema/apiConfig 四个文件输出
- 每个 JSON 片段可独立解析（增量输出友好）

## 七、前端组件结构

### 7.1 新增文件

| 文件路径 | 说明 |
|----------|------|
| `src/views/ai/crud-generator.vue` | 主页面（三栏布局） |
| `src/views/ai/components/CrudGeneratorSidebar.vue` | 左侧会话历史组件 |
| `src/views/ai/components/CrudGeneratorChat.vue` | 中间对话+配置区组件 |
| `src/views/ai/components/CrudGeneratorPreview.vue` | 右侧代码预览组件 |
| `src/views/ai/components/CrudGeneratorFileTree.vue` | 文件树子组件 |
| `src/views/ai/components/CrudGeneratorDiff.vue` | Diff 对比弹窗组件 |
| `src/composables/useCrudGenerator.js` | 生成器逻辑封装 |
| `src/api/crud-generator.js` | API 接口封装 |

### 7.2 状态管理（useCrudGenerator）

**状态**：
```javascript
const sessionId = ref(null)
const messages = ref([])
const configKey = ref('')
const tableName = ref('')
const providerId = ref(null)
const modelId = ref(null)
const temperature = ref(0.3)
const maxTokens = ref(4096)
const generating = ref(false)
const currentStage = ref('')
const generatedFiles = ref({
  searchSchema: '',
  columnsSchema: '',
  editSchema: '',
  apiConfig: ''
})
const activeFile = ref('searchSchema')
const inputText = ref('')
const sessionList = ref([])
```

**方法**：
- `loadSessionList()` — 加载历史会话列表
- `startNewSession()` — 新建会话
- `loadSession(id)` — 加载历史会话详情
- `deleteSession(id)` — 删除会话
- `sendMessage()` — 发送消息并开始流式生成
- `handleSSEChunk(event, data)` — 处理 SSE 事件
- `saveConfig()` — 保存配置到数据库
- `loadExistingConfig()` — 加载现有配置（用于 Diff）
- `copyCurrentFile()` — 复制当前文件
- `exportAllFiles()` — 导出全部配置

## 八、菜单注册与 SQL 初始化

### 8.1 菜单配置

| 属性 | 值 |
|------|------|
| 资源名称 | CRUD 生成器 |
| 父级 | AI 管理（path=/ai） |
| 资源类型 | 2（菜单） |
| 排序 | 11 |
| 路由路径 | `/ai/crud-generator` |
| 组件 | `ai/crud-generator` |
| 权限标识 | `ai:crud-generator:use` |
| 图标 | `mdi:magic-staff` |

### 8.2 SQL 初始化

追加到 `ai_crud_config.sql`：

```sql
-- CRUD 生成器菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_time, update_time)
SELECT 0, 'CRUD 生成器', id, 2, 11, '/ai/crud-generator', 'ai/crud-generator', 0, 0, 1, 1, 'ai:crud-generator:use', 'mdi:magic-staff', 0, 0, 'AI CRUD 配置生成器', NOW(), NOW()
FROM sys_resource WHERE tenant_id = 0 AND path = '/ai' AND resource_type = 1 LIMIT 1;

-- 原 CRUD 配置菜单排序调整为 12
UPDATE sys_resource SET sort = 12 WHERE tenant_id = 0 AND path = '/ai/crud-config' AND resource_type = 2;
```

## 九、风险与注意事项

### 9.1 技术风险

| 风险 | 说明 | 应对措施 |
|------|------|----------|
| SSE 连接中断 | 流式输出过程中网络中断 | 前端显示错误提示，提供"重新生成"按钮 |
| JSON 解析失败 | AI 输出 JSON 格式不规范 | 后端校验 + 前端容错，显示原始文本让用户手动修复 |
| Monaco Editor 加载慢 | 编辑器库体积较大 | 使用 CDN 或懒加载，显示加载动画 |

### 9.2 注意事项

- 会话历史暂不与 `ai_crud_config` 关联（MVP 阶段）
- 流式生成接口需支持租户隔离（TenantContextHolder）
- Diff 对比仅对比 JSON 结构差异，不深度对比字段语义

## 十、测试策略

### 10.1 功能测试

- 新建会话 → 发送描述 → 流式生成 → 编辑 → 保存
- 加载历史会话 → 查看消息 → 切换会话
- Diff 对比：现有配置 vs 新生成配置
- 复制/导出功能

### 10.2 边界测试

- configKey 格式校验（非法字符、超长）
- tableName 不存在时的错误提示
- SSE 连接中断后的恢复
- AI 输出 JSON 格式错误时的处理