import { describe, expect, it } from 'vitest'
import { quickActionPolicyError, quickActionPrecheckError } from '../quickActionPolicy'

describe('quickActionPolicyError', () => {
  it('blocks quick actions the node disallows before opening the comment dialog', () => {
    expect(quickActionPolicyError('reject', { allowReject: false })).toBe('当前节点不允许驳回')
    expect(quickActionPolicyError('approve', { allowApprove: false })).toBe('当前节点不允许同意')
    expect(quickActionPolicyError('reject', { allowMultiReturn: true, returnTargets: [{ activityId: 'A' }] }))
      .toContain('指定节点驳回')
    expect(quickActionPolicyError('approve', { requireSignature: true })).toContain('签名')
  })

  it('allows actions when the policy is open or unknown', () => {
    expect(quickActionPolicyError('reject', { allowReject: true })).toBe('')
    expect(quickActionPolicyError('approve', null)).toBe('')
  })

  it('prechecks flow dynamic forms that must be filled in the detail drawer', () => {
    expect(quickActionPrecheckError('approve', { formType: 'dynamic', formJson: '[{"field":"amount"}]' })).toContain('节点表单')
    expect(quickActionPrecheckError('reject', { formType: 'dynamic', formJson: '[{"field":"amount"}]' })).toBe('')
    expect(quickActionPrecheckError('approve', { formType: 'dynamic', formJson: '' })).toBe('')
  })
})
