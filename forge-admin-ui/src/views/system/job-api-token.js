import dayjs from 'dayjs'

const DEFAULT_EXPIRY_DAYS = 90

export function createJobApiTokenForm(now = Date.now()) {
  return {
    callerName: '',
    callerDescription: '',
    scopes: [],
    jobIds: [],
    jobGroups: [],
    expiresAt: dayjs(now).add(DEFAULT_EXPIRY_DAYS, 'day').valueOf(),
  }
}

export function normalizeJobApiResources(resources = []) {
  const jobs = Array.isArray(resources) ? resources : []
  const jobOptions = jobs
    .filter(item => Number(item?.id) > 0)
    .map(item => ({
      label: `${item.jobName || `任务 ${item.id}`} · ${item.jobGroup || 'DEFAULT'}`,
      value: Number(item.id),
      jobGroup: item.jobGroup || 'DEFAULT',
      description: item.description || '',
    }))
  const jobGroups = [...new Set(jobOptions.map(item => item.jobGroup))]
  return {
    jobOptions,
    groupOptions: jobGroups.map(value => ({ label: value, value })),
  }
}

export function buildJobApiTokenPayload(form) {
  return {
    callerName: String(form?.callerName || '').trim(),
    callerDescription: String(form?.callerDescription || '').trim() || null,
    scopes: [...new Set(form?.scopes || [])],
    jobIds: [...new Set(form?.jobIds || [])],
    jobGroups: [...new Set(form?.jobGroups || [])],
    expiresAt: dayjs(form?.expiresAt).format('YYYY-MM-DDTHH:mm:ss'),
  }
}

export function summarizeJobApiResources(token = {}) {
  const jobCount = Array.isArray(token.jobIds) ? token.jobIds.length : 0
  const groups = Array.isArray(token.jobGroups) ? token.jobGroups : []
  const summary = []
  if (jobCount > 0)
    summary.push(`${jobCount} 个指定任务`)
  if (groups.length > 0)
    summary.push(`${groups.length} 个任务组`)
  return summary.join(' · ') || '未配置资源'
}
