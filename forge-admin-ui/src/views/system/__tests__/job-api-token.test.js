import { describe, expect, it } from 'vitest'
import {
  buildJobApiTokenPayload,
  createJobApiTokenForm,
  normalizeJobApiResources,
  summarizeJobApiResources,
} from '../job-api-token.js'

describe('job api token helpers', () => {
  it('builds deduplicated token payload without display-only values', () => {
    const payload = buildJobApiTokenPayload({
      callerName: '  order-center  ',
      callerDescription: '  ',
      scopes: ['jobs:read', 'jobs:read', 'executions:read'],
      jobIds: [7, 7, 8],
      jobGroups: ['OPS', 'OPS'],
      expiresAt: new Date(2026, 6, 30, 12, 30, 0).getTime(),
    })

    expect(payload).toEqual({
      callerName: 'order-center',
      callerDescription: null,
      scopes: ['jobs:read', 'executions:read'],
      jobIds: [7, 8],
      jobGroups: ['OPS'],
      expiresAt: '2026-07-30T12:30:00',
    })
    expect(payload).not.toHaveProperty('token')
  })

  it('derives task and group options from backend resources', () => {
    const normalized = normalizeJobApiResources([
      { id: 7, jobName: 'dailyReport', jobGroup: 'OPS', description: '日报' },
      { id: 8, jobName: 'archive', jobGroup: 'OPS' },
      { id: 9, jobName: 'settlement', jobGroup: 'FINANCE' },
    ])

    expect(normalized.jobOptions).toHaveLength(3)
    expect(normalized.jobOptions[0]).toMatchObject({ value: 7, jobGroup: 'OPS' })
    expect(normalized.groupOptions).toEqual([
      { label: 'OPS', value: 'OPS' },
      { label: 'FINANCE', value: 'FINANCE' },
    ])
  })

  it('creates a future default expiry and summarizes resource scope', () => {
    const now = new Date(2026, 6, 20, 8, 0, 0).getTime()
    const form = createJobApiTokenForm(now)

    expect(form.expiresAt).toBeGreaterThan(now)
    expect(summarizeJobApiResources({ jobIds: [7, 8], jobGroups: ['OPS'] }))
      .toBe('2 个指定任务 · 1 个任务组')
  })
})
