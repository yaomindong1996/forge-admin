import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function source(relativePath) {
  return readFileSync(resolve(relativePath), 'utf8')
}

describe('flow task action bar sizing', () => {
  it('keeps todo / purchase / leave approve actions on the shared equal-size bar', () => {
    const css = source('src/styles/flow-task-actions.css')
    const todo = source('src/views/flow/todo.vue')
    const purchase = source('src/views/business/purchase-order-test.vue')
    const leave = source('src/views/leave/LeaveApproveForm.vue')

    expect(css).toContain('min-width: 120px')
    expect(css).toContain('height: 34px')

    expect(todo).toContain('flow-task-action-bar')
    expect(todo).toContain('flow-task-actions.css')
    expect(todo).not.toMatch(/canDelegate[\s\S]{0,80}size="small"/)

    expect(purchase).toContain('flow-task-action-bar')
    expect(purchase).not.toMatch(/canApprove[\s\S]{0,120}size="large"/)
    expect(purchase).not.toMatch(/canReject[\s\S]{0,120}size="large"/)

    expect(leave).toContain('flow-task-action-bar')
    expect(leave).not.toMatch(/isActionSubmitting\('approve'\)[\s\S]{0,80}size="large"/)
  })
})
