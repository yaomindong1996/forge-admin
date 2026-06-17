/**
 * 节点配置组件调度表（Task 25）
 *
 * NodeConfigDrawer 根据 nodeType 从这里选取对应配置组件。
 * 网关三种类型（condition/parallel/inclusive）共用 ConditionConfig（仅 condition 实际有
 * conditionExpression 字段，parallel / inclusive 实际只展示分支列表）。
 */

import AdvancedConfig from './AdvancedConfig.vue'
import ApproverConfig from './ApproverConfig.vue'
import CallActivityConfig from './CallActivityConfig.vue'
import CarbonCopyConfig from './CarbonCopyConfig.vue'
import ConditionConfig from './ConditionConfig.vue'
import EndConfig from './EndConfig.vue'
import ScriptConfig from './ScriptConfig.vue'
import ServiceConfig from './ServiceConfig.vue'
import StartConfig from './StartConfig.vue'
import SubProcessConfig from './SubProcessConfig.vue'

export const CONFIG_RENDERER_MAP = Object.freeze({
  start: StartConfig,
  end: EndConfig,
  approver: ApproverConfig,
  carbonCopy: CarbonCopyConfig,
  condition: ConditionConfig,
  parallel: ConditionConfig, // 仅展示分支列表，不需要 condition
  inclusive: ConditionConfig,
  service: ServiceConfig,
  script: ScriptConfig,
  subProcess: SubProcessConfig,
  callActivity: CallActivityConfig,
  advanced: AdvancedConfig,
})

export function getConfigComponent(nodeType) {
  return CONFIG_RENDERER_MAP[nodeType] || null
}
