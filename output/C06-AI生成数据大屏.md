# 从自然语言到数据大屏：Forge Report Studio 的 AI 生成链路

> 业务人员说"我要一张经营监控大屏"，AI 在 10 秒内给出完整可交互的可视化方案 —— 这不是演示 Demo，是 Forge Report Studio 的日常。

---

## 一、问题场景：当业务人员需要大屏，但不懂代码

如果你在传统 BI 团队呆过，一定见过这样的流程：

```
业务提需求 → BI 分析师出指标定义 → 前端开发画布局 → 
后端开发配数据源 → 联调数据接口 → UI 走查 → 上线
```

一张中等复杂度的大屏，从需求到上线通常需要 **2-4 周**，中间至少经历 3 个角色的反复沟通。任何一方排期紧张，大屏就搁置。

更残酷的真相是：**80% 的数据大屏需求并没有那么复杂**。它们往往是"把 A 表的数据用柱状图展示、B 表的趋势画折线图、再加几个 KPI 卡片"。这类需求的问题不在技术难度，而在**角色分工导致的流程成本**。

Forge Report Studio 的 AI 生成链路试图回答一个问题：**能不能让业务人员直接说人话，AI 负责把话变成大屏？**

---

## 二、解决方案：自然语言 → 大屏 JSON → 可视化组件

### 整体思路

我们选择了一条**结构化生成**路径，而不是让 AI 直接写前端代码：

```
用户自然语言
  ↓
AI 模型（理解需求 + 组件选择 + 数据绑定）
  ↓
大屏 JSON Schema（标准化的组件配置）
  ↓
前端渲染引擎（组件库 + 布局算法 + 数据适配器）
  ↓
可交互的数据大屏
```

选择 JSON 而非代码生成，有三个核心考量：

1. **可控性**：JSON 可以在进入渲染引擎前被校验、修复、规范化，代码则几乎无法安全自动化
2. **审计性**：每一份 AI 生成的 JSON 都完整记录了"AI 选了什么组件、绑了什么数据集、做了什么取舍"，出问题时可以精确定位
3. **安全性**：JSON 运行在已有的沙箱渲染引擎中，不会引入任意代码执行风险

### 端到端流程

从用户发送需求到看到大屏，整个链路大约 10-15 秒：

| 阶段 | 耗时 | 做什么 |
|---|---|---|
| 上下文准备 | <1s | 拉取组件目录、业务定义、数据集画像、运行时样例 |
| AI 推理 | 5-10s | 模型根据 prompt 生成大屏 JSON |
| 流式解析 | 同步 | SSE 逐块接收，前端实时展示进度 |
| 结果校验 | <1s | 数据集绑定验证、布局规范化、组件合法性检查 |
| 应用到画布 | <1s | 创建组件实例、合并默认配置、建立数据请求 |

---

## 三、数据结构：大屏的三个核心抽象

在进入实现细节前，先理解大屏的数据模型。一张大屏由三层结构组成：

### 3.1 画布层

```typescript
// 画布配置
interface CanvasConfig {
  width: number          // 画布宽度，默认 1920
  height: number         // 画布高度，默认 1080
  background: string     // 背景色或背景图
  chartThemeColor: string // 主题色，影响所有图表的配色
  projectName: string    // 项目名称
}
```

画布是整个大屏的容器，定义了尺寸和全局视觉基调。

### 3.2 组件层

```typescript
// AI 返回的组件 Schema
interface AIComponentSchema {
  key: string            // 组件类型，如 'BarCommon'、'PieCommon'
  x: number; y: number   // 左上角坐标
  w: number; h: number   // 宽高
  title?: string         // 自定义标题
  option?: Record<string, any>  // 图表配置（ECharts option 等）
  request?: {            // 动态数据绑定
    datasetId: number
    datasetName: string
    datasetFields: string[]
    datasetMapping: { ... }
  }
}
```

关键设计：`request` 字段是可选的数据绑定。AI 生成时如果知道该用哪个数据集，就会填 `request.datasetId`；如果无法确定，就只填 `option.dataset` 作为静态预览数据。这个设计打通了"AI 生成"和"动态数据查询"两个世界。

### 3.3 组件库：AI 的"调色盘"

AI 不是凭空创造组件，而是从已有的 **100+ 组件库**中选择。每个组件都有 AI 可见的描述信息：

```typescript
// 组件注册表中的 AI 描述
const aiComponentInfoMap = {
  BarCommon: { defaultW: 500, defaultH: 300, description: '柱状图，适合对比分类数据、月度/季度趋势' },
  PieCommon: { defaultW: 500, defaultH: 300, description: '饼图，适合展示占比分布' },
  LineCommon: { defaultW: 500, defaultH: 300, description: '折线图，适合展示时间趋势变化' },
  KpiCard: { defaultW: 300, defaultH: 120, description: 'KPI 指标卡片，适合展示核心数值' },
  TableScrollBoard: { defaultW: 500, defaultH: 250, description: '轮播表格，适合多行数据滚动展示' },
  MapBase: { defaultW: 800, defaultH: 600, description: '地图，适合地理数据展示' },
  // ... 共 100+ 个组件
}
```

AI prompt 中会注入组件目录文本，让模型知道有哪些"积木"可用。组件按类别组织：

- **图表类**：柱状图、折线图、饼图、散点图、热力图、雷达图、漏斗图、桑基图等 22 个 ECharts 组件 + 10 个 VChart 组件
- **信息类**：文本、渐变文字、图片、轮播图、视频、词云等 15 个
- **表格类**：普通表格、轮播表格、排行榜
- **装饰类**：13 种边框、6 种装饰元素
- **指标类**：KPI 卡片、指标组、仪表盘等 20 余个

这个组件目录是 AI 和渲染引擎之间的**契约**：AI 只负责选组件和给配置，渲染引擎负责把配置变成真实的 Vue 组件。

---

## 四、实现链路：AI 生成大屏的完整流程

### 4.1 整体架构

```
┌──────────────────────────────────────────────────┐
│              大屏设计器前端 (Vue 3)                 │
│  ┌──────────┐  ┌───────────┐  ┌────────────────┐ │
│  │ AIChatPanel │  │ aiEngine  │  │ componentRegistry│ │
│  │ (聊天UI)   │  │ (画布应用) │  │ (组件目录)       │ │
│  └─────┬────┘  └─────┬─────┘  └───────┬────────┘ │
│        │             │               │           │
│  ┌─────▼────┐  ┌─────▼─────┐  ┌─────▼─────────┐ │
│  │generateValidation│ │layoutAlgorithm│ │datasetAdapter│ │
│  │(结果校验) │  │(布局规整)  │  │(数据适配)     │ │
│  └──────────┘  └───────────┘  └───────────────┘ │
└────────────────────┬─────────────────────────────┘
                     │ POST /forge-report-api/ai/generate/stream
                     ▼
┌──────────────────────────────────────────────────┐
│               后端 AI 服务 (Spring Boot)           │
│  ┌──────────────┐  ┌───────────────────────────┐ │
│  │ AiChatService │  │ AiPromptTemplateRenderer │ │
│  │ (提示词构建)  │  │ (模板变量渲染)              │ │
│  └──────┬───────┘  └───────────────────────────┘ │
│         │                                         │
│  ┌──────▼──────────────────────────────────────┐ │
│  │         forge-plugin-ai                      │ │
│  │  AiClient → ChatClientCache → OpenAI API    │ │
│  │  (统一调用 + 供应商切换 + 流式输出)            │ │
│  └─────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
```

### 4.2 提示词工程：让 AI 理解"做大屏"这个任务

AI 生成大屏的核心挑战不是"让模型输出 JSON"——任何现代模型都能做到。真正的挑战是**让模型输出正确、可用、符合组件库约束的 JSON**。

我们的提示词设计分三层：

**第一层：系统角色定义**

```
你是一个数据可视化大屏设计专家，请根据用户需求生成合法的 JSON 大屏配置。
```

**第二层：约束注入（12 个模板变量）**

后端 `AiChatService` 构建时注入以下上下文：

| 变量 | 内容 | 作用 |
|---|---|---|
| `componentCatalog` | 完整组件目录（100+ 组件，含 key、尺寸、描述） | 告诉 AI 可选组件池 |
| `businessContext` | 业务定义 + 数据集画像（字段、用途、推荐用法） | 告诉 AI 数据语义 |
| `runtimeDataPreview` | 数据集抽样查询结果（数据样例） | 让 AI 理解字段取值和量级 |
| `canvasWidth` / `canvasHeight` | 画布尺寸 | 约束组件位置和大小 |
| `style` | 深色/浅色主题 | 约定视觉风格 |
| `projectName` | 项目名称 | 生成合理的标题 |
| `canvasContext` | 画布已有组件（增量修改时） | 让 AI 在此基础上调整 |

**第三层：数据绑定指令**

这是最关键的一层。我们在 prompt 中明确要求：

```
- 所有图表、表格组件必须输出 request.datasetId 和 request.datasetFields
- request.datasetId 只能来自 datasets 列表中的值
- request.datasetFields 只能使用数据集 fields 内的 fieldName
- 优先使用 primary 数据集生成顶部 KPI 和主图
- 按 usageRemark 匹配趋势、排行、明细、地图等组件
- 允许保留 option.dataset 作为预览兜底，但动态组件必须带 request
```

前端在发送生成请求前，会将业务定义上下文和数据运行时样例进行**压缩和格式化**，构建为 `businessContext` JSON 字符串传给后端。

### 4.3 流式生成：让等待不再焦虑

AI 生成大屏通常需要 5-10 秒。如果这段时间用户只看到转圈，体验会很差。我们使用 **SSE 流式输出**，让用户实时看到进展：

```
用户发送 "生成经营监控大屏"
  ↓
[connecting] 正在连接模型...
  ↓
[chunk × N] 流式推理文本逐块返回
  ↓
[complete] 生成完成
```

前端 `consumeReasoningAwareChunk()` 函数负责解析流式数据块，支持思考过程分离（DeepSeek-R1 等推理模型）。在生成模式下，前端不直接展示 AI 输出的 JSON 原文，而是显示进度步骤：

```
● 理解需求 → ● 规划布局 → ○ 生成组件 → ○ 完善配置 → ○ 校验结果
```

进度步骤通过统计已接收文本长度来估算当前阶段，简单但有效。

### 4.4 JSON 解析与修复：容错是工程的第一要义

AI 输出的 JSON 不可能 100% 合法。`llmClient.ts` 中的解析管线做了**两层容错**：

```typescript
export function parseStreamedResponse(fullText: string): AIGenerateResponse {
  // 第一层：提取 JSON（去除 markdown 代码块、前后文字）
  const jsonStr = normalizeJSONString(extractJSON(fullText))
  
  try {
    return JSON.parse(jsonStr)
  } catch {
    // 第二层：修复常见错误后重试
    const repaired = repairJSON(jsonStr)  // 补括号、去尾逗号、修引号
    return JSON.parse(repaired)
  }
}
```

`normalizeJSONString()` 处理了 6 类 AI 常见错误：
- 对象碎片（`{ "0 }` 类幻觉产物）
- 引号错乱（`"xxx"option"` → `"xxx", "option"`）
- 尾部多余逗号
- 缺少引号的 key（`key:` → `"key":`）
- 缺失的组件对象开头（`"key"` → `{"key"`）
- 括号不平衡（自动补齐 `}` 和 `]`）

这些修复让解析成功率从裸 JSON.parse 的约 85% 提升到 98%+。

### 4.5 结果校验：AI 输出不可信的，程序来兜底

这是整个链路中**最重要的工程环节**。`generateValidation.ts` 的 `validateAIGenerateResponse()` 函数对 AI 输出执行三层校验：

**第一层：组件合法性检查**

```typescript
const descriptor = componentRegistry.get(key)
if (!descriptor) {
  summary.skipped++  // 组件不在组件库中，直接跳过
  summary.warnings.push(`组件 ${key} 不在组件库中，已跳过`)
  return
}
```

**第二层：布局规范化**

```typescript
// 钳制越界：确保组件不超出画布范围
x = Math.max(0, Math.min(x, canvasWidth - w))
y = Math.max(0, Math.min(y, canvasHeight - h))
// 钳制尺寸：确保组件不小于最小尺寸
w = Math.min(Math.max(w, minW), canvasWidth)
```

**第三层：数据集绑定验证与修复**

这是最复杂的校验逻辑。核心函数 `validateDatasetRequest()` 实现了**多级降级 + 自动修复**策略：

```
数据集绑定状态机：
  ┌──────────┐
  │ 有 datasetId │──→ 在当前业务定义中？──→ 是 ──→ 字段校验通过？──→✓ bound
  │              │                        │
  │              │                        └──→ 否 ──→ 自动修复字段 → repaired
  │              │
  │              │──→ 不在当前业务定义中？
  │              │      ├──→ 通过字段/编码/名称推断到其他数据集 → repaired（自动改绑）
  │              │      └──→ 无法推断 → staticFallback（删除 request，降级为静态）
  └──────┬───────┘
         │
  ┌──────▼───────┐
  │ 无 datasetId  │──→ 有业务定义？
  │              │      ├──→ 是 → 尝试推断数据集 → 成功 → repaired
  │              │      │                      └→ 失败 → static
  │              │      └──→ 否 → unverified（保留原始绑定但不校验）
  └──────────────┘
```

**推断算法**：当 AI 没有指定 `datasetId` 或指定了无效的 `datasetId` 时，校验器会根据以下线索自动匹配数据集：
- 组件 `request` 中的 `datasetCode` / `datasetName` 与数据集的精确/模糊匹配
- 组件标题与数据集名称的语义匹配
- `request.datasetFields` 与数据集 `fields` 的字段名交集
- 数据集的 `usageRemark`（用途说明）与组件类型的匹配

这个推断算法让 AI 即使"记错"了 `datasetId`，只要在请求中表达了足够的数据意图，程序也能自动修正。

### 4.6 布局算法：模型管选择，程序管摆放

一个容易被忽略的设计决策是：**布局由程序处理，AI 只管组件选择和数据**。

`layoutAlgorithm.ts` 中的 `normalizeAILayout()` 函数对 AI 返回的组件位置执行 9 步规整流程：

```
① 统一边框风格 (ensureConsistentPanelBorders)
② 去除冗余装饰 (removeRedundantDecorations)
③ 钳制越界尺寸 (normalizeSize)
④ 规范化标题 (normalizeHeader)
⑤ 装饰元素规范 (normalizeOverlayDecorations)
⑥ 指标组等宽排列 (normalizeTopMetrics)
⑦ 应用大屏布局模板 (applyLargeVisualTemplate)
    - 模板 A：左窄 + 中宽 + 右窄（经典监控布局）
    - 模板 B：左侧大图 + 右侧两列（地理大屏布局）
    - 模板 C：右侧大图 + 左侧两列（核心指标右置）
⑧ 大尺寸组件规范 (normalizeLargeVisuals)
⑨ 重叠检测 + 间距强制 (placeWithoutOverlap + enforceBorderGaps)
```

AI 返回的 `x, y, w, h` 只是一个"建议"，布局算法会把它规整到合理的范围内。这意味着即使用户反复修改 prompt 重新生成，大屏的视觉效果始终保持一致。

### 4.7 应用到画布：AI 输出如何变成真实组件

`aiEngine.ts` 的 `applyAIToCanvas()` 函数是连接 AI 输出和渲染引擎的桥梁：

```typescript
export function applyAIToCanvas(response: AIGenerateResponse) {
  // 1. 应用画布配置：标题、背景色、主题色、尺寸
  // 2. 遍历 components，逐个 createAIComponent()
  // 3. 模式选择：replace（替换整个画布）或 append（增量添加）
}

function createAIComponent(schema: AIComponentSchema) {
  // 1. findConfigTypeByKey(schema.key) —— 查找组件定义
  // 2. componentInstall(chartConfig) —— 注册 Vue 组件
  // 3. createComponent(chartConfig) —— 创建组件实例
  // 4. applyOption(schema.option, componentInstance) —— 智能合并配置
  // 5. applyDatasetRequest(schema.request, componentInstance) —— 建立数据绑定
}
```

**智能配置合并**是 `applyOption()` 的核心逻辑。由于 AI 只返回了部分 option 字段，前端需要把 AI 的输出**合并**到组件的默认配置上：

```typescript
// ECharts 组件的合并策略：
// 1. 覆盖 dataset（AI 给出的数据和维度）
// 2. 自动调整 series 数量匹配 dimensions 列数
// 3. 保留组件的样式、动画、交互等默认配置
```

这意味着 AI 只需要关心"数据长什么样"，不需要关心"图表怎么渲染"。所有渲染细节（坐标轴、图例、tooltip、动画）都由组件的默认配置提供。

---

## 五、设计取舍：工程化 AI 生成的四个关键决策

### 5.1 为什么是 JSON Schema 而不是代码生成？

这是做 AI 大屏时要回答的第一个问题。市面上有不少方案让 AI 直接生成 React/Vue 组件代码，但我们的判断是：

| 维度 | JSON Schema 生成 | 代码生成 |
|---|---|---|
| 输出可控性 | 高：结构化数据，可校验、可修复、可回滚 | 低：代码片段质量不稳定，难以自动化验证 |
| 安全性 | 高：运行在已有渲染引擎沙箱中 | 低：任意代码执行风险 |
| 上下文利用率 | 中：token 主要用于数据描述 | 低：大量 token 消耗在语法和模板代码 |
| 用户可编辑性 | 高：生成后用户可在设计器中拖拽调整 | 低：生成的代码用户无法修改 |
| 与已有组件库集成 | 高：组件 key 直接映射到注册表 | 低：需要额外的编译/转译步骤 |

结论：对于"业务人员使用"的场景，JSON Schema 是更合适的选择。代码生成更适合"开发者使用"的场景，那是另外的产品方向。

### 5.2 如何平衡 AI 的灵活性和系统的可控性？

我们的策略是**分层解耦，各自负责擅长的部分**：

```
AI 负责（灵活）:
  - 理解用户需求
  - 选择合适的组件组合
  - 确定数据绑定关系
  - 提供布局建议

程序负责（可控）:
  - 校验组件合法性
  - 修复无效的数据集绑定
  - 规范化布局位置
  - 合并组件默认配置
  - 统一视觉风格
```

这是一种**"放风筝"模式**：让 AI 自由发挥创意，但程序的校验和修复层像一根线，确保它不会飞出边界。

### 5.3 数据集绑定：为什么是最难的部分？

统计显示，数据集绑定错误占 AI 生成问题的 **60% 以上**。原因很简单：AI 模型不"认识"你的数据。

我们的解决方案分三个层次：

1. **Prompt 层面**：在 `businessContext` 中提供结构化的数据集画像（字段名、类型、用途、推荐用法），并明确要求 AI 按规则填写
2. **数据预检层面**：在生成前对绑定数据集做抽样查询，把真实数据样例注入 prompt，让 AI 看到字段的实际取值
3. **校验修复层面**：生成后运行 `validateDatasetRequest()`，自动推断和修复错误的数据集绑定

```typescript
// 数据预检样例（注入到 prompt 中）
runtimeDataPreview: {
  items: [{
    datasetId: 42,
    datasetName: "销售明细表",
    status: "ready",
    sampleRows: [
      { month: "2024-01", revenue: 1280000, orders: 3420, category: "电子产品" },
      { month: "2024-02", revenue: 1150000, orders: 2980, category: "电子产品" }
    ]
  }]
}
```

这些样例行让 AI 能从"字段名"推断出"数据长什么样"，从而做出更准确的组件类型选择和字段映射。

### 5.4 业务定义：AI 准备度评分

不是所有业务定义都适合 AI 直接生成。`businessReadiness.ts` 中的 `evaluateBusinessReadiness()` 函数对业务定义的 AI 准备度打分：

```
评分维度（满分 100）：
├── 语义字段（最多 60 分）：业务描述、分析目标、指标口径、分析维度、使用建议
├── 数据集绑定（最多 21 分）：数据集数量、主数据集标记
├── 字段上下文（最多 14 分）：字段总数
└── 用途说明（最多 5 分）：数据集 usageRemark 是否有值
```

```
≥80 分 → 准备充分 → 大概率生成高质量大屏
55-79 分 → 可生成 → 部分组件可能使用静态数据
<55 分 → 需补充 → 建议先完善业务定义再生成
```

这个评分在用户选择业务定义后实时展示，帮助用户在生成前评估预期的生成质量。

---

## 六、二开指南：三个最常用的扩展方向

### 6.1 扩展组件库

新增一个 AI 可选的组件需要两步：

**第一步**：在 `componentRegistry.ts` 的 `aiComponentInfoMap` 中添加组件描述：

```typescript
// 例如新增一个"实时监控指标卡"
RealTimeMonitor: {
  defaultW: 400,
  defaultH: 150,
  description: '实时监控指标卡，适合展示需要刷新的核心指标，支持阈值告警'
}
```

**第二步**：确保组件在 `packagesList` 中正确注册，`createComponent()` 可以正常实例化。

无需修改任何 AI prompt 模板——组件描述会自动进入下一次请求的 `componentCatalog`。

### 6.2 自定义主题

主题系统集中在 `src/settings/chartThemes/` 目录下：

```json
// global.theme.json 控制全局 ECharts 样式
{
  "title": { "textStyle": { "color": "#BFBFBF" } },
  "xAxis": { "axisLine": { "lineStyle": { "color": "#B9B8CE" } } },
  "legend": { "textStyle": { "color": "#B9B8CE" } }
}
```

如果要新增主题方案（如"商务蓝"、"科技绿"），需要：

1. 在 `chartThemes/` 下新建主题文件
2. 在 AI 聊天面板的 style 选择器中添加对应选项
3. 在 `AiChatService.buildDashboardContextVars()` 中增加对应的模板变量

### 6.3 调整 AI 生成策略

AI 生成行为主要由两个后端配置控制：

**系统提示词**（`AiPromptTemplateRenderer` 渲染）：

```java
// 默认提示词
"你是一个数据可视化大屏设计专家，请根据用户需求生成合法的 JSON 大屏配置。"
```

**业务定义上下文**（`compactBusinessContext()` 构建）：

如果想让 AI 更激进地使用动态数据绑定，可以调整 `generationPolicy`：

```typescript
// 例如：要求所有图表必须动态绑定
generationPolicy: {
  bindingPriority: [
    '所有图表、表格、指标组件必须输出 request.datasetId',
    '不允许生成不带 request 的图表组件'
  ]
}
```

如果希望 AI 更保守（优先使用静态数据预览），则反之。

---

## 七、体验预告

AI 生成大屏解决了"快速搭建"的问题，但大屏的真正价值在于**数据是活的**。

下一篇 **C07《大屏动态数据接入：从静态 Mock 到实时查询》** 将深入 Forge Report Studio 的数据接入体系：

- 数据源类型：HTTP API、JDBC 数据库直连、WebSocket 实时推送
- 数据集定义：SQL 查询、动态参数（时间范围/下拉筛选/级联参数）
- 数据安全：行级权限过滤、字段级脱敏、SQL 注入防护
- 数据适配：数据集到 ECharts 的标准化映射
- 外部 API 代理：跨域访问的安全方案设计

---

**项目入口**：

| 模块 | 地址 |
|---|---|
| 后台管理 | [http://81.70.22.48:8084/forge/login](http://81.70.22.48:8084/forge/login) |
| 项目文档 | [http://81.70.22.48:8084/forge-docs/](http://81.70.22.48:8084/forge-docs/) |
| 大屏设计器 | [http://81.70.22.48:8084/forge-report/](http://81.70.22.48:8084/forge-report/) |
| Gitee | [https://gitee.com/ForgeLab/forge-admin](https://gitee.com/ForgeLab/forge-admin) |
| GitHub | [https://github.com/yaomindong1996/forge-admin](https://github.com/yaomindong1996/forge-admin) |
