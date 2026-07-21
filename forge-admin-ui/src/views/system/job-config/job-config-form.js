import dayjs from 'dayjs'

export const JOB_SCHEDULE_CRON = 'CRON'
export const JOB_SCHEDULE_ONCE = 'ONCE'
export const JOB_INVOKE_SINGLE = 'SINGLE'
export const JOB_INVOKE_FLOW = 'FLOW'
export const DEFAULT_JOB_TIMEZONE = 'Asia/Shanghai'
export const DEFAULT_JOB_CONCURRENT_POLICY = 'ALLOW'
export const DEFAULT_JOB_MISFIRE_POLICY = 'DO_NOTHING'
export const MAX_JOB_RETRY_COUNT = 5

const TEXT_FIELDS = [
  'jobName',
  'jobGroup',
  'description',
  'invokeMode',
  'executorBean',
  'executorMethod',
  'executorHandler',
  'executorService',
  'flowModelKey',
  'flowDeploymentId',
  'flowProcessDefinitionId',
  'scheduleType',
  'cronExpression',
  'timezone',
  'jobParam',
  'concurrentPolicy',
  'misfirePolicy',
  'alarmEmail',
]

export function createDefaultJobForm() {
  return {
    id: null,
    jobName: '',
    jobGroup: 'DEFAULT',
    description: '',
    invokeMode: JOB_INVOKE_SINGLE,
    executeMode: 'HANDLER',
    executorBean: '',
    executorMethod: '',
    executorHandler: '',
    executorService: '',
    flowModelKey: '',
    flowModelVersion: null,
    flowDeploymentId: '',
    flowProcessDefinitionId: '',
    scheduleType: JOB_SCHEDULE_CRON,
    cronExpression: '0 0 2 * * ?',
    fireOnceTime: null,
    timezone: DEFAULT_JOB_TIMEZONE,
    jobParam: '',
    status: 0,
    concurrentPolicy: DEFAULT_JOB_CONCURRENT_POLICY,
    misfirePolicy: DEFAULT_JOB_MISFIRE_POLICY,
    idempotentFlag: 0,
    retryCount: 0,
    alarmEnabled: 0,
    alarmChannels: [],
    alarmRecipientUserIds: [],
    alarmEmail: '',
    version: null,
    syncStatus: '',
    syncError: '',
    syncTime: null,
  }
}

export function normalizeJobConfig(detail = {}) {
  const form = {
    ...createDefaultJobForm(),
    ...detail,
    invokeMode: String(detail.invokeMode || JOB_INVOKE_SINGLE).toUpperCase(),
    flowModelVersion: normalizePositiveInteger(detail.flowModelVersion),
    status: Number(detail.status ?? 0) === 2 ? 0 : Number(detail.status ?? 0),
    idempotentFlag: Number(detail.idempotentFlag ?? 0),
    retryCount: Number(detail.retryCount ?? 0),
    alarmEnabled: Number(detail.alarmEnabled ?? 0),
    alarmChannels: normalizeCsvList(detail.alarmChannels),
    alarmRecipientUserIds: normalizeCsvList(detail.alarmRecipientUserIds)
      .map(value => Number.isSafeInteger(Number(value)) ? Number(value) : value),
    fireOnceTime: normalizeDateTimeValue(detail.fireOnceTime),
  }
  TEXT_FIELDS.forEach((field) => {
    form[field] = form[field] == null ? '' : String(form[field])
  })
  return form
}

export function buildJobConfigPayload(form) {
  const data = normalizeJobConfig(form)
  validateRetryPolicy(data)
  validateAlarmConfig(data)
  validateInvocation(data)
  const flowInvocation = data.invokeMode === JOB_INVOKE_FLOW
  const payload = {
    id: data.id || null,
    jobName: data.jobName.trim(),
    jobGroup: data.jobGroup.trim() || 'DEFAULT',
    description: emptyToNull(data.description),
    invokeMode: data.invokeMode,
    executeMode: flowInvocation ? null : data.executeMode,
    executorBean: emptyToNull(data.executorBean),
    executorMethod: emptyToNull(data.executorMethod),
    executorHandler: emptyToNull(data.executorHandler),
    executorService: emptyToNull(data.executorService),
    flowModelKey: flowInvocation ? emptyToNull(data.flowModelKey) : null,
    flowModelVersion: flowInvocation ? data.flowModelVersion : null,
    scheduleType: data.scheduleType,
    cronExpression: data.scheduleType === JOB_SCHEDULE_CRON ? emptyToNull(data.cronExpression) : null,
    fireOnceTime: data.scheduleType === JOB_SCHEDULE_ONCE ? formatLocalDateTime(data.fireOnceTime) : null,
    timezone: data.timezone.trim(),
    jobParam: normalizeJsonParameter(data.jobParam, flowInvocation),
    status: Number(data.status),
    concurrentPolicy: data.concurrentPolicy,
    misfirePolicy: data.misfirePolicy,
    idempotentFlag: Number(data.idempotentFlag),
    retryCount: Number(data.retryCount || 0),
    alarmEnabled: Number(data.alarmEnabled),
    alarmChannels: joinCsv(data.alarmChannels),
    alarmRecipientUserIds: joinCsv(data.alarmRecipientUserIds),
    alarmEmail: joinCsv(normalizeCsvList(data.alarmEmail)),
    version: data.version ?? null,
  }

  if (flowInvocation) {
    payload.executorBean = null
    payload.executorMethod = null
    payload.executorHandler = null
    payload.executorService = null
  }
  else if (payload.executeMode === 'HANDLER') {
    payload.executorBean = null
    payload.executorMethod = null
    payload.executorService = null
  }
  else if (payload.executeMode === 'BEAN') {
    payload.executorHandler = null
    payload.executorService = null
  }
  else if (payload.executeMode === 'RPC') {
    payload.executorBean = null
    payload.executorMethod = null
  }
  return payload
}

function validateInvocation(data) {
  if (![JOB_INVOKE_SINGLE, JOB_INVOKE_FLOW].includes(data.invokeMode))
    throw new Error('调用方式仅支持单一执行器或流程编排')
  if (data.invokeMode !== JOB_INVOKE_FLOW)
    return
  if (!String(data.flowModelKey || '').trim())
    throw new Error('请选择已发布流程模型')
  if (!Number.isInteger(data.flowModelVersion) || data.flowModelVersion <= 0)
    throw new Error('请选择已发布流程版本')
}

function validateAlarmConfig(data) {
  if (![0, 1].includes(Number(data.alarmEnabled)))
    throw new Error('告警开关配置不正确')

  const channels = normalizeCsvList(data.alarmChannels).map(value => value.toUpperCase())
  if (channels.some(channel => !['WEB', 'EMAIL'].includes(channel)))
    throw new Error('告警渠道仅支持站内信或邮件')

  const emails = normalizeCsvList(data.alarmEmail)
  if (emails.some(email => !isEmail(email)))
    throw new Error('告警邮箱格式不正确')

  if (Number(data.alarmEnabled) !== 1)
    return
  if (!channels.length)
    throw new Error('启用告警后至少选择一个通知渠道')
  if (channels.includes('WEB') && !normalizeCsvList(data.alarmRecipientUserIds).length)
    throw new Error('站内信告警必须选择平台用户')
  if (channels.includes('EMAIL') && !emails.length)
    throw new Error('邮件告警必须填写邮箱地址')
}

function validateRetryPolicy(data) {
  if (!Number.isInteger(data.retryCount)
    || data.retryCount < 0
    || data.retryCount > MAX_JOB_RETRY_COUNT) {
    throw new Error('失败重试次数必须在 0 到 5 之间')
  }
  if (data.retryCount > 0 && data.idempotentFlag !== 1)
    throw new Error('只有明确声明幂等安全的任务才能开启自动重试')
}

export function formatJobParameter(value, requireObject = false) {
  const text = String(value || '').trim()
  if (!text)
    return ''
  try {
    const parsed = JSON.parse(text)
    if (requireObject && (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object'))
      throw new Error('FLOW任务参数必须是 JSON 对象')
    return JSON.stringify(parsed, null, 2)
  }
  catch (error) {
    if (error?.message === 'FLOW任务参数必须是 JSON 对象')
      throw error
    throw new Error('任务参数必须是合法 JSON')
  }
}

export function isOnceTimeInFuture(value, timezone, now = Date.now()) {
  const selected = dayjs(value)
  if (!selected.isValid() || !timezone)
    return false

  try {
    const selectedWallTime = Number(selected.format('YYYYMMDDHHmmss'))
    const currentWallTime = Number(formatZonedWallTime(now, timezone))
    return selectedWallTime > currentWallTime
  }
  catch {
    return false
  }
}

function normalizeJsonParameter(value, requireObject = false) {
  const text = String(value || '').trim()
  if (!text)
    return null
  try {
    const parsed = JSON.parse(text)
    if (requireObject && (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object'))
      throw new Error('FLOW任务参数必须是 JSON 对象')
    return JSON.stringify(parsed)
  }
  catch (error) {
    if (error?.message === 'FLOW任务参数必须是 JSON 对象')
      throw error
    throw new Error('任务参数必须是合法 JSON')
  }
}

function emptyToNull(value) {
  const text = String(value || '').trim()
  return text || null
}

function normalizeCsvList(value) {
  const values = Array.isArray(value) ? value : String(value || '').split(',')
  return [...new Set(values
    .map(item => String(item ?? '').trim())
    .filter(Boolean))]
}

function joinCsv(value) {
  const values = normalizeCsvList(value)
  return values.length ? values.join(',') : null
}

function isEmail(value) {
  const text = String(value || '')
  const atIndex = text.indexOf('@')
  const domain = text.slice(atIndex + 1)
  return atIndex > 0
    && atIndex === text.lastIndexOf('@')
    && domain.includes('.')
    && !text.includes(' ')
    && !text.includes('\t')
    && !text.includes('\n')
}

function normalizeDateTimeValue(value) {
  if (value == null || value === '')
    return null
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.valueOf() : null
}

function normalizePositiveInteger(value) {
  if (value == null || value === '')
    return null
  const number = Number(value)
  return Number.isInteger(number) && number > 0 ? number : null
}

function formatLocalDateTime(value) {
  const parsed = dayjs(value)
  if (!parsed.isValid())
    throw new Error('请选择一次性执行时间')
  return parsed.format('YYYY-MM-DDTHH:mm:ss')
}

function formatZonedWallTime(value, timezone) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(value))
  const values = Object.fromEntries(parts.map(part => [part.type, part.value]))
  return `${values.year}${values.month}${values.day}${values.hour}${values.minute}${values.second}`
}
