import { describe, expect, it } from 'vitest'
import {
  buildJobLogQuery,
  normalizeJobMonitorSummary,
  resolveJobLogStartedAt,
} from '../job-log-query'

describe('job log query', () => {
  it('builds all supported filters and preserves failed status zero', () => {
    const start = new Date(2026, 6, 20, 8, 30, 0).getTime()
    const end = new Date(2026, 6, 20, 9, 45, 30).getTime()

    expect(buildJobLogQuery({
      jobName: ' inventoryClose ',
      jobGroup: ' BUSINESS ',
      status: 0,
      triggerType: 'MANUAL',
      dateRange: [start, end],
    })).toEqual({
      jobName: 'inventoryClose',
      jobGroup: 'BUSINESS',
      status: 0,
      triggerType: 'MANUAL',
      startTime: '2026-07-20 08:30:00',
      endTime: '2026-07-20 09:45:30',
    })
  })

  it('uses stable task id instead of a mutable task name', () => {
    expect(buildJobLogQuery({ jobName: 'draft-name' }, {
      jobConfigId: 7,
      jobName: 'fixed-name',
    })).toEqual({ jobConfigId: 7 })
  })

  it('normalizes empty monitor data for rendering', () => {
    expect(normalizeJobMonitorSummary()).toMatchObject({
      totalCount: 0,
      acceptedCount: 0,
      successRate: 0,
      consecutiveFailureTaskCount: 0,
      failureTasks: [],
    })
  })

  it('normalizes accepted executions as a numeric count', () => {
    expect(normalizeJobMonitorSummary({ acceptedCount: '3' }).acceptedCount).toBe(3)
  })

  it('resolves the most useful execution start time for the log list', () => {
    expect(resolveJobLogStartedAt({
      scheduledFireTime: '2026-07-21 10:00:00',
      triggerTime: '2026-07-21 10:00:02',
      startTime: '2026-07-21 10:00:03',
    })).toBe('2026-07-21 10:00:03')
    expect(resolveJobLogStartedAt({
      scheduledFireTime: '2026-07-21 10:00:00',
      triggerTime: '2026-07-21 10:00:02',
    })).toBe('2026-07-21 10:00:02')
    expect(resolveJobLogStartedAt({
      scheduledFireTime: '2026-07-21 10:00:00',
    })).toBe('2026-07-21 10:00:00')
    expect(resolveJobLogStartedAt()).toBeNull()
  })
})
