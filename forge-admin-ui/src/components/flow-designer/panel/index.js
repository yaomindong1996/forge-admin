/**
 * 钉钉样式流程设计器 - 配置抽屉与配置组件导出
 *
 * Phase 5 完成（Task 18-25）：
 *   - NodeConfigDrawer.vue：抽屉容器
 *   - BasicConfig.vue：节点 ID/name/documentation
 *   - 节点专属配置：StartConfig / EndConfig / ApproverConfig（含 5 Tab）/ CarbonCopyConfig
 *     / ConditionConfig / ServiceConfig / ScriptConfig / SubProcessConfig / CallActivityConfig
 *     / AdvancedConfig（rawXml 只读 / 编辑）
 *   - 子表单：ApproverAssigneeForm / MultiInstanceConfig / PermissionConfig
 *     / FormPermissionConfig / ListenerConfig
 *   - config-renderer-map.js：CONFIG_RENDERER_MAP 调度
 */

export { default as AdvancedConfig } from './AdvancedConfig.vue'
export { default as ApproverAssigneeForm } from './ApproverAssigneeForm.vue'
export { default as ApproverConfig } from './ApproverConfig.vue'
export { default as BasicConfig } from './BasicConfig.vue'
export { default as CallActivityConfig } from './CallActivityConfig.vue'
export { default as CarbonCopyConfig } from './CarbonCopyConfig.vue'
export { default as ConditionConfig } from './ConditionConfig.vue'
export * from './config-renderer-map.js'
export { default as EndConfig } from './EndConfig.vue'
export { default as FormPermissionConfig } from './FormPermissionConfig.vue'
export { default as ListenerConfig } from './ListenerConfig.vue'
export { default as MultiInstanceConfig } from './MultiInstanceConfig.vue'
export { default as NodeConfigDrawer } from './NodeConfigDrawer.vue'
export { default as PermissionConfig } from './PermissionConfig.vue'
export { default as ScriptConfig } from './ScriptConfig.vue'
export { default as ServiceConfig } from './ServiceConfig.vue'
export { default as StartConfig } from './StartConfig.vue'
export { default as SubProcessConfig } from './SubProcessConfig.vue'
