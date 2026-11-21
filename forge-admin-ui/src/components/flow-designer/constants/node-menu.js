/**
 * 节点添加菜单分组
 *
 * 用于 AddNodePopover：点击 "+" 时弹出的节点类型选择菜单。
 * 与 spec 10.3 节点类型映射保持一致；advanced 不在添加菜单（仅作为加载兼容兜底）。
 */

import { NODE_TYPE } from './node-types.js'

export const NODE_MENU_GROUPS = Object.freeze([
  Object.freeze({
    label: '审批流',
    items: Object.freeze([
      { type: NODE_TYPE.APPROVER, label: '审批人', icon: 'i-lucide:user', tone: 'blue' },
      { type: NODE_TYPE.CARBON_COPY, label: '抄送人', icon: 'i-lucide:copy', tone: 'teal' },
    ]),
  }),
  Object.freeze({
    label: '分支',
    items: Object.freeze([
      { type: NODE_TYPE.CONDITION, label: '条件分支', icon: 'i-lucide:git-branch', tone: 'amber' },
      { type: NODE_TYPE.PARALLEL, label: '并行分支', icon: 'i-lucide:split', tone: 'emerald' },
      { type: NODE_TYPE.INCLUSIVE, label: '包容分支', icon: 'i-lucide:git-merge', tone: 'emerald' },
    ]),
  }),
  Object.freeze({
    label: '高级',
    items: Object.freeze([
      // 可设计；发布时服务端仍按执行白名单校验（仅抄送类 serviceTask 等受支持）
      { type: NODE_TYPE.SERVICE, label: '服务任务', icon: 'i-lucide:settings', tone: 'indigo' },
      { type: NODE_TYPE.SCRIPT, label: '脚本任务', icon: 'i-lucide:code', tone: 'violet' },
      { type: NODE_TYPE.SUB_PROCESS, label: '子流程', icon: 'i-lucide:network', tone: 'cyan' },
      { type: NODE_TYPE.CALL_ACTIVITY, label: '调用活动', icon: 'i-lucide:phone', tone: 'sky' },
    ]),
  }),
])

/** 反向查找：type → menu 项（label / icon） */
const TYPE_INDEX = (() => {
  const m = new Map()
  for (const g of NODE_MENU_GROUPS) {
    for (const it of g.items)
      m.set(it.type, it)
  }
  return m
})()

export function getNodeMenuItem(type) {
  return TYPE_INDEX.get(type) || null
}
