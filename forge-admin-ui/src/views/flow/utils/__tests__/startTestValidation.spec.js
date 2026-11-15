import { describe, expect, it } from 'vitest'
import { formatStartTestValidationMessage } from '../startTestValidation'

describe('formatStartTestValidationMessage', () => {
  it('从 Naive 校验数组提取必填文案', () => {
    expect(formatStartTestValidationMessage([
      { field: 'amount', message: '请输入金额' },
      { field: 'reason', message: '请填写原因' },
    ])).toBe('请完善必填项：请输入金额；请填写原因')
  })

  it('非校验错误返回空串，避免误伤真实失败', () => {
    expect(formatStartTestValidationMessage(new Error('网络超时'))).toBe('')
    expect(formatStartTestValidationMessage(null)).toBe('')
  })
})
