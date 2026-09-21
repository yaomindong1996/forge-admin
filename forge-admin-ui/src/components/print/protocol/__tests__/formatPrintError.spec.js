import { describe, expect, it } from 'vitest'
import { formatPrintApiError } from '../formatPrintError'

describe('formatPrintApiError', () => {
  it('surfaces the first protocol issue from RespInfo.data', () => {
    expect(formatPrintApiError({
      message: '打印模板校验失败',
      error: {
        code: 400,
        message: '打印模板校验失败',
        data: [
          { path: 'body[0].elements[0].type', code: 'UNSUPPORTED_VALUE', message: '不支持此配置值' },
          { path: 'body[0].elements[1]', code: 'UNKNOWN_PROPERTY', message: '不支持此属性' },
        ],
      },
    })).toBe('打印模板校验失败：不支持此配置值 · body[0].elements[0].type 等 2 项')
  })

  it('falls back to the raw message when issues are absent', () => {
    expect(formatPrintApiError({ message: '修订冲突，请重新载入' }, '保存失败')).toBe('修订冲突，请重新载入')
    expect(formatPrintApiError({}, '保存失败')).toBe('保存失败')
  })
})
