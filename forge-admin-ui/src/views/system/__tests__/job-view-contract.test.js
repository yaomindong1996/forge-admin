import { describe, expect, it } from 'vitest'
import { hasAccessibleRoute, resolveJobExecutionMode } from '../job-view-contract'

describe('job view contract', () => {
  it('uses invoke mode dictionary for flow jobs', () => {
    const runModes = [{ label: 'Handler', value: 'HANDLER' }]
    const invokeModes = [{ label: '流程编排', value: 'FLOW' }]

    expect(resolveJobExecutionMode({
      invokeMode: 'flow',
      executeMode: null,
    }, runModes, invokeModes)).toEqual({
      options: invokeModes,
      value: 'FLOW',
    })
  })

  it('keeps execute mode dictionary for single jobs', () => {
    const runModes = [{ label: 'Handler', value: 'HANDLER' }]
    const invokeModes = [{ label: '单一执行器', value: 'SINGLE' }]

    expect(resolveJobExecutionMode({
      invokeMode: 'SINGLE',
      executeMode: 'HANDLER',
    }, runModes, invokeModes)).toEqual({
      options: runModes,
      value: 'HANDLER',
    })
  })

  it('only permits an exact accessible flow monitor route', () => {
    const routes = [
      { path: '/system/job-config' },
      { path: '/flow/monitor?source=menu' },
    ]

    expect(hasAccessibleRoute(routes, '/flow/monitor')).toBe(true)
    expect(hasAccessibleRoute([{ path: '/flow/model' }], '/flow/monitor')).toBe(false)
    expect(hasAccessibleRoute([], '/flow/monitor')).toBe(false)
  })
})
