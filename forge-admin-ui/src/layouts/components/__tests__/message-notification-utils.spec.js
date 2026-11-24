import { describe, expect, it } from 'vitest'
import {
  formatNotificationPreview,
  isFlowApprovalMessage,
  isPendingFlowApprovalMessage,
  mergeMessageNavigationTarget,
} from '../message-notification-utils'

describe('formatNotificationPreview', () => {
  it('strips card html tags so notification list does not show raw div markup', () => {
    const preview = formatNotificationPreview(
      '<div class="gray">流程待办提醒</div><div class="normal">任务：模具清洗</div><div class="normal">流程：请假流程</div>',
    )

    expect(preview).toContain('流程待办提醒')
    expect(preview).toContain('任务：模具清洗')
    expect(preview).not.toContain('<div')
  })

  it('fills known placeholders then drops unresolved ones', () => {
    const preview = formatNotificationPreview(
      '<div class="normal">任务：${taskTitle}</div><div class="normal">流程：${processName}</div>',
      { taskTitle: '模具清洗申请' },
    )

    expect(preview).toBe('任务：模具清洗申请')
    expect(preview).not.toContain('${processName}')
  })
})

describe('message notification flow actions', () => {
  it('keeps the approval action for an unread flow todo message', () => {
    const message = { bizType: 'FLOW_TODO', readFlag: 0 }

    expect(isFlowApprovalMessage(message)).toBe(true)
    expect(isPendingFlowApprovalMessage(message)).toBe(true)
  })

  it('treats a read flow todo message as history instead of an actionable todo', () => {
    const message = { bizType: 'FLOW_TODO', readFlag: 1 }

    expect(isFlowApprovalMessage(message)).toBe(true)
    expect(isPendingFlowApprovalMessage(message)).toBe(false)
  })

  it('does not expose approval actions for ordinary messages', () => {
    expect(isFlowApprovalMessage({ bizType: 'SYSTEM', readFlag: 0 })).toBe(false)
    expect(isPendingFlowApprovalMessage(null)).toBe(false)
  })

  it('keeps approval task parameters inside an application portal target', () => {
    expect(mergeMessageNavigationTarget({
      name: 'ApplicationPortal',
      params: { applicationCodeOrSlug: 'sales' },
      query: { pageId: 'system:todo' },
    }, {
      taskId: 'task-1',
      source: 'message',
    })).toEqual({
      name: 'ApplicationPortal',
      params: { applicationCodeOrSlug: 'sales' },
      query: {
        pageId: 'system:todo',
        taskId: 'task-1',
        source: 'message',
      },
    })
  })

  it('converts the legacy string route when task parameters are present', () => {
    expect(mergeMessageNavigationTarget('/flow/todo', { taskId: 'task-1' })).toEqual({
      path: '/flow/todo',
      query: { taskId: 'task-1' },
    })
  })
})
