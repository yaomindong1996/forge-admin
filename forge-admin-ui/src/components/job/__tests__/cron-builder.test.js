import { describe, expect, it } from 'vitest'
import { buildCronExpression, parseCronExpression } from '../cron-builder'

describe('cron builder', () => {
  it('builds five supported simple schedules', () => {
    expect(buildCronExpression({ type: 'interval', intervalMinutes: 10 })).toBe('0 0/10 * * * ?')
    expect(buildCronExpression({ type: 'hourly', minute: 15 })).toBe('0 15 * * * ?')
    expect(buildCronExpression({ type: 'daily', time: '02:30' })).toBe('0 30 2 * * ?')
    expect(buildCronExpression({ type: 'weekly', weekday: 'FRI', time: '18:00' })).toBe('0 0 18 ? * FRI')
    expect(buildCronExpression({ type: 'monthly', dayOfMonth: 5, time: '09:20' })).toBe('0 20 9 5 * ?')
  })

  it('parses only expressions the simple form can preserve', () => {
    expect(parseCronExpression('0 0/10 * * * ?')).toMatchObject({ type: 'interval', intervalMinutes: 10 })
    expect(parseCronExpression('0 15 * * * ?')).toMatchObject({ type: 'hourly', minute: 15 })
    expect(parseCronExpression('0 30 2 * * ?')).toMatchObject({ type: 'daily', time: '02:30' })
    expect(parseCronExpression('0 0 18 ? * FRI')).toMatchObject({ type: 'weekly', weekday: 'FRI', time: '18:00' })
    expect(parseCronExpression('0 20 9 5 * ?')).toMatchObject({ type: 'monthly', dayOfMonth: 5, time: '09:20' })
  })

  it('keeps complex cron in expert mode', () => {
    expect(parseCronExpression('0 0 9-18 ? * MON-FRI')).toBeNull()
    expect(parseCronExpression('0 0 2 L * ?')).toBeNull()
  })
})
