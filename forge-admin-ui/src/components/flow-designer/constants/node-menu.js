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
      { type: NODE_TYPE.APPROVER, label: '审批人', icon: 'i-mdi-account-check', color: 'text-primary' },
      { type: NODE_TYPE.CARBON_COPY, label: '抄送人', icon: 'i-mdi-email-outline', color: 'text-info' },
    ]),
  }),
  Object.freeze({
    label: '分支',
    items: Object.freeze([
      { type: NODE_TYPE.CONDITION, label: '条件分支', icon: 'i-mdi-source-branch', color: 'text-warning' },
      { type: NODE_TYPE.PARALLEL, label: '并行分支', icon: 'i-mdi-call-split', color: 'text-success' },
      { type: NODE_TYPE.INCLUSIVE, label: '包容分支', icon: 'i-mdi-set-merge', color: 'text-success' },
    ]),
  }),
  Object.freeze({
    label: '高级',
    items: Object.freeze([
      { type: NODE_TYPE.SERVICE, label: '服务任务', icon: 'i-mdi-cog-outline', color: 'text-info' },
      { type: NODE_TYPE.SCRIPT, label: '脚本任务', icon: 'i-mdi-code-tags', color: 'text-info' },
      { type: NODE_TYPE.SUB_PROCESS, label: '子流程', icon: 'i-mdi-sitemap-outline', color: 'text-info' },
      { type: NODE_TYPE.CALL_ACTIVITY, label: '调用活动', icon: 'i-mdi-phone-forward-outline', color: 'text-info' },
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
