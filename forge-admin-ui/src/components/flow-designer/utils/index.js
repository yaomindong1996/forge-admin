/**
 * 钉钉样式流程设计器 - 通用工具导出
 *
 * 已落地：
 *   - id-generator：nodeId / edgeId / branchId 单调递增
 *   - path-builder：SVG 边 path d 构造 + 颜色 / 虚线 / 中点
 *   - approver-summary：审批人卡片摘要文案
 */

export * from './approver-summary.js'
export * from './id-generator.js'
export * from './path-builder.js'
