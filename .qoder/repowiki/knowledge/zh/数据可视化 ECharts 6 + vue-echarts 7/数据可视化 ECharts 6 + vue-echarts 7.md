---
kind: external_dependency
name: 数据可视化 ECharts 6 + vue-echarts 7
slug: echarts
category: external_dependency
category_hints:
    - vendor_identity
    - framework_behavior
scope:
    - '**'
source_files:
    - forge-admin-ui/package.json
    - forge-report-ui/package.json
    - README.md
---

### 身份与角色
- 低代码大屏（forge-report-ui）与后台管理端图表统一基于 ECharts 6 与 vue-echarts 7 实现，支持柱状图、折线图、饼图、雷达图、地图等内置组件。
- README 将 ECharts/VChart 列为前端核心可视化能力。

### 关键约束
- 大屏组件通过 AI 生成布局后再接入真实 API 数据，图表主题、尺寸、自适应方式由大屏编辑器配置。
- 多端（后台管理/H5/大屏）共用同一套图表生态，升级需评估三方影响。

### 集成点
- `forge-report-ui/package.json`：大屏侧同样依赖 echarts/vue-echarts。

### 方向
- 新增大屏图表组件时，应复用现有 ECharts 封装模式，保持主题、数据源、事件交互的一致性。