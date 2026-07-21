export function formatJobLogQueryTime(timestamp) {
  const date = new Date(timestamp)
  const pad = value => String(value).padStart(2, '0')
  const datePart = [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-')
  return `${datePart} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function buildJobLogQuery(filters = {}, fixed = {}) {
  const query = {}
  const jobConfigId = fixed.jobConfigId ?? filters.jobConfigId
  const jobName = fixed.jobName || String(filters.jobName || '').trim()
  const jobGroup = String(filters.jobGroup || '').trim()

  if (jobConfigId !== null && jobConfigId !== undefined && jobConfigId !== '')
    query.jobConfigId = jobConfigId
  else if (jobName)
    query.jobName = jobName

  if (jobGroup)
    query.jobGroup = jobGroup
  if (filters.status !== null && filters.status !== undefined && filters.status !== '')
    query.status = filters.status
  if (filters.triggerType)
    query.triggerType = filters.triggerType
  if (Array.isArray(filters.dateRange) && filters.dateRange.length === 2) {
    query.startTime = formatJobLogQueryTime(filters.dateRange[0])
    query.endTime = formatJobLogQueryTime(filters.dateRange[1])
  }
  return query
}

export function resolveJobLogStartedAt(log = {}) {
  return log.startTime || log.triggerTime || log.scheduledFireTime || null
}

export function normalizeJobMonitorSummary(value = {}) {
  return {
    totalCount: Number(value.totalCount || 0),
    successCount: Number(value.successCount || 0),
    failedCount: Number(value.failedCount || 0),
    runningCount: Number(value.runningCount || 0),
    skippedCount: Number(value.skippedCount || 0),
    acceptedCount: Number(value.acceptedCount || 0),
    successRate: Number(value.successRate || 0),
    failureRate: Number(value.failureRate || 0),
    consecutiveFailureTaskCount: Number(value.consecutiveFailureTaskCount || 0),
    failureTasks: Array.isArray(value.failureTasks) ? value.failureTasks : [],
  }
}
