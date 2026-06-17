/**
 * 钉钉审批样式流程设计器 - 公共入口
 *
 * 主要导出（Phase 6-7 完成）：
 *   - DingFlowDesigner：流程设计器主组件（编辑模式）
 *   - DingFlowViewer：流程查看器（只读模式，ProcessDiagramViewer 替代）
 *   - NodeStatusBadge / NodeDetailPopover：查看器辅助组件
 *
 * 内部模块：
 *   - canvas/      画布、SVG 连线层、添加按钮、布局算法
 *   - nodes/       12 种节点卡片
 *   - panel/       右侧配置抽屉与各 Tab 配置组件
 *   - viewer/      钉钉样式流程查看器
 *   - converter/   BPMN XML ↔ flowJson 双向转换
 *   - composables/ useFlowDesigner / useFlowHistory / useCanvasViewport
 *   - constants/   节点类型枚举、菜单分组、默认配置
 *   - utils/       ID 生成、路径计算、审批人摘要等通用工具
 */

export * from './canvas/index.js'
export * from './composables/index.js'
export * from './constants/index.js'
export * from './converter/index.js'
export { default as DingFlowDesigner } from './DingFlowDesigner.vue'
export * from './nodes/index.js'
export * from './panel/index.js'
export * from './utils/index.js'
export * from './viewer/index.js'
