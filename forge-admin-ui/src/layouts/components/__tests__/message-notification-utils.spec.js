import { describe, expect, it } from 'vitest'
import {
  isFlowApprovalMessage,
  isPendingFlowApprovalMessage,
} from '../message-notification-utils'

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
})
