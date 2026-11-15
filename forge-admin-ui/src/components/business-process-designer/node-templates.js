export const START_NODE_TEMPLATES = Object.freeze([
  startTemplate('手动点击按钮', 'MANUAL', '用户在列表或详情页点击按钮触发', 'START_MANUAL', {
    positions: ['ROW', 'DETAIL'],
    permission: 'ai:businessProcess:start',
  }),
  startTemplate('记录创建后', 'EVENT_CREATED', '新记录保存到数据库后自动触发', 'START_EVENT', {
    eventType: 'RECORD_CREATED',
  }),
  startTemplate('状态变更后', 'EVENT_STATUS_CHANGED', '记录状态字段变化后自动触发', 'START_EVENT', {
    eventType: 'STATUS_CHANGED',
  }),
  startTemplate('定时扫描', 'SCHEDULED', '按固定周期扫描到期或超期记录', 'START_SCHEDULE', {
    dueField: '',
    lookAheadDays: 0,
    lookBackDays: 0,
    serviceActor: {
      mode: 'CONFIGURED_USER',
      userConfigKey: 'business.process.schedule.service-user',
    },
  }),
])

export const ACTION_NODE_TEMPLATES = Object.freeze([
  actionTemplate('更新状态', 'UPDATE_STATUS', '预填当前对象的状态字段和值', {
    actionType: 'UPDATE_RECORD',
    // 默认用平台托管的 flowStatus；有对象字段时由 createActionTemplateConfig 覆盖为实际编码
    fieldMappings: [{ field: 'flowStatus', valueSource: 'CONSTANT', value: '' }],
  }, [{ type: 'TRANSITION_STATUS', field: 'flowStatus', fromValue: '', toValue: '' }]),
  actionTemplate('调整数量', 'ADJUST_NUMBER', '选择已发布的数值调整业务动作', {
    actionType: 'BUSINESS_ACTION',
    businessActionCode: '',
  }, [{ type: 'ADJUST_NUMBER', field: '', amount: '' }]),
  actionTemplate('创建记录', 'CREATE_RECORD', '预填目标对象和一条字段赋值', {
    actionType: 'CREATE_RECORD',
    fieldMappings: [{ field: '', valueSource: 'CONSTANT', value: '' }],
  }, [{ type: 'CREATE_RECORD', fieldMappings: [] }]),
  actionTemplate('发送消息', 'SEND_MESSAGE', '选择已启用的消息模板发送通知', {
    actionType: 'SEND_MESSAGE',
    messageTemplateCode: '',
  }, [{ type: 'SEND_MESSAGE', messageTemplateCode: '' }]),
])

export function createStartTemplateConfig(value) {
  const template = START_NODE_TEMPLATES.find(item => item.value === value)
  if (!template)
    return null
  return {
    type: template.nodeType,
    config: clone(template.config),
  }
}

/**
 * @param {string} value 模板编码
 * @param {{ objectCode?: string, statusField?: string }} [options]
 *   statusField：对象上的流程/业务状态字段编码（优先 flowStatus）
 */
export function createActionTemplateConfig(value, { objectCode = '', statusField = '' } = {}) {
  const template = ACTION_NODE_TEMPLATES.find(item => item.value === value)
  if (!template)
    return null
  const config = clone(template.config)
  if (['UPDATE_RECORD', 'CREATE_RECORD'].includes(config.actionType))
    config.objectCode = objectCode
  if (value === 'UPDATE_STATUS') {
    const field = normalizeStatusFieldCode(statusField) || 'flowStatus'
    config.fieldMappings = [{ field, valueSource: 'CONSTANT', value: '' }]
  }
  return config
}

/** 识别 flowStatus / status 等状态字段编码，避免模板写死不存在的 status。 */
export function normalizeStatusFieldCode(value) {
  const code = String(value || '').trim()
  if (!code)
    return ''
  const compact = code.replace(/[-_]/g, '').toLowerCase()
  if (compact === 'flowstatus' || compact === 'status' || compact.endsWith('status'))
    return code
  return ''
}

function startTemplate(label, value, description, nodeType, config) {
  return Object.freeze({ label, value, description, nodeType, config: Object.freeze(config) })
}

function actionTemplate(label, value, description, config, steps) {
  return Object.freeze({ label, value, description, config: Object.freeze(config), steps: Object.freeze(steps) })
}

function clone(value) {
  return JSON.parse(JSON.stringify(value || {}))
}
