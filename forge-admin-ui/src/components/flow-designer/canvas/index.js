/**
 * 钉钉样式流程设计器 - 画布层导出
 *
 * Phase 3-4 已落地：
 *   - FlowCanvas.vue：画布容器
 *   - EdgeLayer.vue / EdgePath.vue：SVG 连线层
 *   - AddNodeButton.vue / AddNodePopover.vue：节点之间 "+" 添加按钮
 *   - BranchHeader.vue：分支首部条件徽章
 *   - MergeNode.vue：分支汇合点视觉提示
 *   - NodeContextMenu.vue：节点右键菜单
 *   - NodeRenderer.vue：按 nodeType 调度对应节点卡片
 *   - layout-engine.js：画布布局
 */

export { default as AddNodeButton } from './AddNodeButton.vue'
export { default as AddNodePopover } from './AddNodePopover.vue'
export { default as BranchHeader } from './BranchHeader.vue'
export { default as EdgeLayer } from './EdgeLayer.vue'
export { default as EdgePath } from './EdgePath.vue'
export { default as FlowCanvas } from './FlowCanvas.vue'
export * from './layout-engine.js'
export { default as MergeNode } from './MergeNode.vue'
export { default as NodeContextMenu } from './NodeContextMenu.vue'
export { default as NodeRenderer } from './NodeRenderer.vue'
