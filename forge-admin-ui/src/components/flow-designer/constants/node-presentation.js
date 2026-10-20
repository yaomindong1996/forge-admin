/** 类型颜色只承担识别，不参与选中态；交互始终跟随系统主题。 */
export const NODE_PRESENTATION = Object.freeze({
  start: { label: '发起人', icon: 'i-lucide:user', color: '#5b8def' },
  approver: { label: '审批', icon: 'i-lucide:user', color: '#d99536' },
  carbonCopy: { label: '抄送', icon: 'i-lucide:send', color: '#9476d6' },
  condition: { label: '条件', icon: 'i-lucide:git-branch', color: '#3fa487' },
  parallel: { label: '并行分支', icon: 'i-lucide:split', color: '#3fa487' },
  inclusive: { label: '包容分支', icon: 'i-lucide:git-merge', color: '#3fa487' },
  service: { label: '服务任务', icon: 'i-lucide:settings', color: '#6b8fbd' },
  script: { label: '脚本任务', icon: 'i-lucide:code', color: '#6b8fbd' },
  subProcess: { label: '子流程', icon: 'i-lucide:network', color: '#6b8fbd' },
  callActivity: { label: '调用活动', icon: 'i-lucide:phone', color: '#6b8fbd' },
  advanced: { label: '高级节点', icon: 'i-lucide:shield', color: '#6b8fbd' },
  end: { label: '流程结束', icon: 'i-lucide:circle-stop', color: '#86909c' },
})

export function getNodePresentation(type) {
  return NODE_PRESENTATION[type] || NODE_PRESENTATION.advanced
}
