/** 内置能力的展示资料，不参与安装清单、授权或版本判定；ID 与运行描述保持一致。 */
export const builtinPresentations = {
  'plugin-system': {
    artwork: 0,
    summary: '用户、角色与权限，统一管理',
    introduction: '集中维护组织、账号与访问权限，为业务应用提供一致的系统管理基础。',
    highlights: ['用户与组织', '角色与菜单权限', '租户与系统配置'],
  },
  'plugin-generator': {
    artwork: 1,
    summary: '从数据模型快速生成业务代码',
    introduction: '围绕数据表和业务模型组织开发，减少重复的页面、接口与基础代码编写。',
    highlights: ['数据模型配置', '代码生成与预览', '业务开发扩展'],
  },
  'plugin-flow': {
    artwork: 2,
    summary: '让业务审批有序流转',
    introduction: '通过可视化流程设计连接业务单据、动态表单和审批任务，集中处理业务流转。',
    highlights: ['可视化流程设计', '动态表单衔接', '审批与任务协同'],
  },
  'plugin-message': {
    artwork: 3,
    summary: '连接系统通知与业务提醒',
    introduction: '统一组织消息模板、通知渠道和业务提醒，让关键通知触达相应用户。',
    highlights: ['消息模板', '通知渠道', '站内消息'],
  },
  'plugin-job': {
    artwork: 4,
    summary: '按计划执行，让任务准时完成',
    introduction: '管理定时任务及执行记录，为周期性处理、后台计算等场景提供调度能力。',
    highlights: ['任务计划', '执行记录', '调度管理'],
  },
  'plugin-ai': {
    artwork: 5,
    summary: '统一接入模型，扩展智能能力',
    introduction: '集中维护 AI 供应商与模型配置，为应用中的智能功能提供统一接入能力。',
    highlights: ['供应商管理', '模型配置', '统一调用接入'],
  },
  'plugin-data': {
    artwork: 6,
    summary: '连接数据源，组织业务数据',
    introduction: '集中管理业务数据连接与数据集，为查询、分析和应用展示提供数据基础。',
    highlights: ['数据源连接', '数据集管理', '数据查询'],
  },
  'plugin-external': {
    artwork: 7,
    summary: '让业务连接外部服务',
    introduction: '维护外部接口配置与调用能力，为业务集成提供统一的接入方式。',
    highlights: ['接口配置', '请求参数', '业务集成'],
  },
  'plugin-print': {
    artwork: 8,
    summary: '设计模板，按业务数据打印',
    introduction: '围绕业务场景配置打印模板及数据绑定，支持业务页面复用打印能力。',
    highlights: ['打印模板设计', '业务数据绑定', '预览与打印'],
  },
  'plugin-collaboration': {
    artwork: 9,
    summary: '连接企业协同与办公场景',
    introduction: '集中配置企业协同连接，为组织内办公及外部协同平台对接提供基础能力。',
    highlights: ['协同连接', '平台对接', '办公集成'],
  },
  'plugin-mcp': {
    artwork: 10,
    summary: '让智能工具连接业务能力',
    introduction: '通过 MCP 服务衔接模型工具与受控业务能力，支持统一的工具接入。',
    highlights: ['MCP 服务', '工具接入', '能力调用'],
  },
  'plugin-capability-core': {
    artwork: 11,
    summary: '组织可复用的业务能力',
    introduction: '提供业务能力描述、注册和受控执行的基础设施，为上层开放与编排提供内核。',
    highlights: ['能力定义', '能力注册', '执行基础'],
  },
  'plugin-capability-platform': {
    artwork: 12,
    summary: '将业务能力有序开放',
    introduction: '集中管理能力目录和开放接入，将已登记的业务能力提供给授权调用方。',
    highlights: ['能力目录', '开放接入', '调用授权'],
  },
  'plugin-capability-actions': {
    artwork: 13,
    summary: '把业务操作沉淀为可复用动作',
    introduction: '组织业务动作及其执行约定，让应用中的常用操作能够被一致地编排和调用。',
    highlights: ['动作定义', '动作编排', '执行约定'],
  },
  'plugin-capability-high-risk-approval': {
    artwork: 14,
    summary: '为高风险操作增加审批保障',
    introduction: '为受控的高风险业务操作提供审批衔接，在执行前明确审批与操作边界。',
    highlights: ['操作审查', '审批衔接', '执行控制'],
  },
}

const unknownPresentation = Object.freeze({
  artwork: 15,
  summary: '查看插件声明与安装说明',
  introduction: '此插件尚未提供功能介绍，请以交付包中的 README 和发布方说明为准。',
  highlights: [],
})

export function pluginPresentation(plugin) {
  // 外部插件即使使用相似名称，也不能冒用系统内置能力的介绍。
  return (plugin?.origin === 'builtin' && Object.hasOwn(builtinPresentations, plugin.id))
    ? builtinPresentations[plugin.id]
    : unknownPresentation
}
