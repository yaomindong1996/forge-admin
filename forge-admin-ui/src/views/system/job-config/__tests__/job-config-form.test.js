import { describe, expect, it } from 'vitest'
import {
  buildJobConfigPayload,
  createDefaultJobForm,
  formatJobParameter,
  isOnceTimeInFuture,
  JOB_INVOKE_FLOW,
  JOB_INVOKE_SINGLE,
  normalizeJobConfig,
} from '../job-config-form'

describe('job config form state', () => {
  it('creates a disabled handler task by default', () => {
    const form = createDefaultJobForm()

    expect(form.invokeMode).toBe(JOB_INVOKE_SINGLE)
    expect(form.executeMode).toBe('HANDLER')
    expect(form.status).toBe(0)
    expect(form.jobGroup).toBe('DEFAULT')
    expect(form.scheduleType).toBe('CRON')
    expect(form.cronExpression).toBe('0 0 2 * * ?')
    expect(form.fireOnceTime).toBeNull()
    expect(form.timezone).toBe('Asia/Shanghai')
    expect(form.concurrentPolicy).toBe('ALLOW')
    expect(form.misfirePolicy).toBe('DO_NOTHING')
    expect(form.idempotentFlag).toBe(0)
    expect(form.retryCount).toBe(0)
    expect(form.alarmEnabled).toBe(0)
    expect(form.alarmChannels).toEqual([])
    expect(form.alarmRecipientUserIds).toEqual([])
  })

  it('converts a completed task into a disabled rescheduling draft', () => {
    const form = normalizeJobConfig({
      status: 2,
      scheduleType: 'ONCE',
      fireOnceTime: '2026-07-19T09:30:00',
      timezone: 'Asia/Shanghai',
    })

    expect(form.status).toBe(0)
    expect(form.scheduleType).toBe('ONCE')
    expect(form.fireOnceTime).toBeTypeOf('number')
  })

  it('keeps complex cron and historical target while normalizing detail', () => {
    const form = normalizeJobConfig({
      id: 12,
      executeMode: 'HANDLER',
      executorHandler: 'legacyHandler',
      cronExpression: '0 0 9-18 ? * MON-FRI',
      status: '1',
      version: 3,
    })

    expect(form.executorHandler).toBe('legacyHandler')
    expect(form.cronExpression).toBe('0 0 9-18 ? * MON-FRI')
    expect(form.status).toBe(1)
    expect(form.version).toBe(3)
  })

  it('preserves snowflake IDs and trusted flow snapshots as strings', () => {
    const form = normalizeJobConfig({
      id: '9223372036854775807',
      invokeMode: JOB_INVOKE_FLOW,
      flowModelKey: 'daily-settlement',
      flowModelVersion: '7',
      flowDeploymentId: '9223372036854775806',
      flowProcessDefinitionId: 'daily-settlement:7:9223372036854775805',
    })

    expect(form.id).toBe('9223372036854775807')
    expect(form.flowModelVersion).toBe(7)
    expect(form.flowDeploymentId).toBe('9223372036854775806')
    expect(form.flowProcessDefinitionId).toBe('daily-settlement:7:9223372036854775805')
    expect(buildJobConfigPayload(form).id).toBe('9223372036854775807')
  })

  it('normalizes execution policy values from detail', () => {
    const form = normalizeJobConfig({
      concurrentPolicy: 'SKIP_IF_RUNNING',
      misfirePolicy: 'FIRE_ONCE_NOW',
      idempotentFlag: '1',
      retryCount: '3',
    })

    expect(form.concurrentPolicy).toBe('SKIP_IF_RUNNING')
    expect(form.misfirePolicy).toBe('FIRE_ONCE_NOW')
    expect(form.idempotentFlag).toBe(1)
    expect(form.retryCount).toBe(3)
  })

  it('submits mutually exclusive cron and one-time schedule fields', () => {
    const cronPayload = buildJobConfigPayload(createDefaultJobForm())
    const oncePayload = buildJobConfigPayload({
      ...createDefaultJobForm(),
      scheduleType: 'ONCE',
      cronExpression: '0 0 2 * * ?',
      fireOnceTime: new Date(2026, 6, 20, 9, 30, 0).getTime(),
      timezone: 'UTC',
    })

    expect(cronPayload.cronExpression).toBe('0 0 2 * * ?')
    expect(cronPayload.fireOnceTime).toBeNull()
    expect(oncePayload.cronExpression).toBeNull()
    expect(oncePayload.fireOnceTime).toBe('2026-07-20T09:30:00')
    expect(oncePayload.timezone).toBe('UTC')
  })

  it('compares a selected wall time with the current time in the task timezone', () => {
    const now = Date.parse('2026-07-19T00:00:00Z')
    const futureWallTime = new Date(2026, 6, 19, 9, 0, 0).getTime()
    const pastWallTime = new Date(2026, 6, 19, 7, 0, 0).getTime()

    expect(isOnceTimeInFuture(futureWallTime, 'Asia/Shanghai', now)).toBe(true)
    expect(isOnceTimeInFuture(pastWallTime, 'Asia/Shanghai', now)).toBe(false)
    expect(isOnceTimeInFuture(futureWallTime, 'invalid/timezone', now)).toBe(false)
  })

  it('clears fields outside the selected execution mode', () => {
    const payload = buildJobConfigPayload({
      ...createDefaultJobForm(),
      jobName: ' inventoryClose ',
      executeMode: 'HANDLER',
      executorHandler: 'inventoryCloseHandler',
      executorBean: 'oldBean',
      executorMethod: 'oldMethod',
      executorService: 'oldService',
      jobParam: '{"warehouseId":1}',
    })

    expect(payload.jobName).toBe('inventoryClose')
    expect(payload.executorHandler).toBe('inventoryCloseHandler')
    expect(payload.executorBean).toBeNull()
    expect(payload.executorMethod).toBeNull()
    expect(payload.executorService).toBeNull()
    expect(payload.flowModelKey).toBeNull()
    expect(payload.flowModelVersion).toBeNull()
    expect(payload.jobParam).toBe('{"warehouseId":1}')
  })

  it('submits only the selected flow model version and clears single targets', () => {
    const payload = buildJobConfigPayload({
      ...createDefaultJobForm(),
      invokeMode: JOB_INVOKE_FLOW,
      executeMode: 'HANDLER',
      executorHandler: 'staleHandler',
      executorBean: 'staleBean',
      executorMethod: 'execute',
      executorService: 'stale-service',
      flowModelKey: ' daily-settlement ',
      flowModelVersion: 7,
      flowDeploymentId: 'untrusted-deployment',
      flowProcessDefinitionId: 'untrusted-definition',
      jobParam: '{"warehouseId":"9223372036854775807"}',
    })

    expect(payload.invokeMode).toBe(JOB_INVOKE_FLOW)
    expect(payload.executeMode).toBeNull()
    expect(payload.executorHandler).toBeNull()
    expect(payload.executorBean).toBeNull()
    expect(payload.executorMethod).toBeNull()
    expect(payload.executorService).toBeNull()
    expect(payload.flowModelKey).toBe('daily-settlement')
    expect(payload.flowModelVersion).toBe(7)
    expect(payload).not.toHaveProperty('flowDeploymentId')
    expect(payload).not.toHaveProperty('flowProcessDefinitionId')
    expect(payload.jobParam).toBe('{"warehouseId":"9223372036854775807"}')
  })

  it('keeps single and flow payloads mutually exclusive when switching modes', () => {
    const form = {
      ...createDefaultJobForm(),
      executeMode: 'HANDLER',
      executorHandler: 'inventoryCloseHandler',
      flowModelKey: 'daily-settlement',
      flowModelVersion: 7,
    }

    const flowPayload = buildJobConfigPayload({ ...form, invokeMode: JOB_INVOKE_FLOW })
    const singlePayload = buildJobConfigPayload({ ...form, invokeMode: JOB_INVOKE_SINGLE })

    expect(flowPayload.executorHandler).toBeNull()
    expect(flowPayload.flowModelKey).toBe('daily-settlement')
    expect(singlePayload.executorHandler).toBe('inventoryCloseHandler')
    expect(singlePayload.flowModelKey).toBeNull()
    expect(singlePayload.flowModelVersion).toBeNull()
  })

  it('requires an active flow selection and JSON object parameters', () => {
    expect(() => buildJobConfigPayload({
      ...createDefaultJobForm(),
      invokeMode: JOB_INVOKE_FLOW,
    })).toThrow('流程模型')

    for (const jobParam of ['null', '[1,2]', '"text"', '10']) {
      expect(() => buildJobConfigPayload({
        ...createDefaultJobForm(),
        invokeMode: JOB_INVOKE_FLOW,
        flowModelKey: 'daily-settlement',
        flowModelVersion: 7,
        jobParam,
      })).toThrow('JSON 对象')
    }

    expect(buildJobConfigPayload({
      ...createDefaultJobForm(),
      invokeMode: JOB_INVOKE_FLOW,
      flowModelKey: 'daily-settlement',
      flowModelVersion: 7,
      jobParam: '',
    }).jobParam).toBeNull()
  })

  it('submits concurrency, misfire and idempotent retry policies', () => {
    const payload = buildJobConfigPayload({
      ...createDefaultJobForm(),
      concurrentPolicy: 'SKIP_IF_RUNNING',
      misfirePolicy: 'FIRE_ONCE_NOW',
      idempotentFlag: 1,
      retryCount: 2,
    })

    expect(payload.concurrentPolicy).toBe('SKIP_IF_RUNNING')
    expect(payload.misfirePolicy).toBe('FIRE_ONCE_NOW')
    expect(payload.idempotentFlag).toBe(1)
    expect(payload.retryCount).toBe(2)
  })

  it('rejects retry without idempotency and retry count over platform limit', () => {
    expect(() => buildJobConfigPayload({
      ...createDefaultJobForm(),
      retryCount: 1,
    })).toThrow('幂等')

    expect(() => buildJobConfigPayload({
      ...createDefaultJobForm(),
      idempotentFlag: 1,
      retryCount: 6,
    })).toThrow('0 到 5')
  })

  it('normalizes and submits station-message and email alarm recipients', () => {
    const form = normalizeJobConfig({
      alarmEnabled: '1',
      alarmChannels: 'WEB,EMAIL',
      alarmRecipientUserIds: '12,13',
      alarmEmail: 'ops@example.com,owner@example.com',
    })

    const payload = buildJobConfigPayload(form)

    expect(form.alarmEnabled).toBe(1)
    expect(form.alarmChannels).toEqual(['WEB', 'EMAIL'])
    expect(form.alarmRecipientUserIds).toEqual([12, 13])
    expect(payload.alarmChannels).toBe('WEB,EMAIL')
    expect(payload.alarmRecipientUserIds).toBe('12,13')
    expect(payload.alarmEmail).toBe('ops@example.com,owner@example.com')
    expect(payload).not.toHaveProperty('webhookUrl')
  })

  it('rejects enabled alarms without matching recipients', () => {
    expect(() => buildJobConfigPayload({
      ...createDefaultJobForm(),
      alarmEnabled: 1,
      alarmChannels: ['WEB'],
    })).toThrow('平台用户')

    expect(() => buildJobConfigPayload({
      ...createDefaultJobForm(),
      alarmEnabled: 1,
      alarmChannels: ['EMAIL'],
      alarmEmail: 'invalid-email',
    })).toThrow('邮箱格式')

    expect(() => buildJobConfigPayload({
      ...createDefaultJobForm(),
      alarmEnabled: 1,
      alarmChannels: ['WEBHOOK'],
    })).toThrow('站内信或邮件')
  })

  it('formats valid JSON and rejects invalid JSON', () => {
    expect(formatJobParameter('{"a":1}')).toBe('{\n  "a": 1\n}')
    expect(() => formatJobParameter('[1,2]', true)).toThrow('JSON 对象')
    expect(() => buildJobConfigPayload({
      ...createDefaultJobForm(),
      jobParam: '{invalid}',
    })).toThrow('任务参数必须是合法 JSON')
  })
})
