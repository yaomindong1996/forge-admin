import { describe, expect, it, vi } from 'vitest'
import { agentStreamLifecycle } from '../agent-stream-lifecycle'

function fixture() {
  const callbacks = { onEvent: vi.fn(), onComplete: vi.fn(), onError: vi.fn() }
  return { callbacks, stream: agentStreamLifecycle(callbacks) }
}

describe('agent 协议终态而非 HTTP EOF', () => {
  it('终态也必须是有效对象，半包/空值/数组不能伪装完成', () => {
    for (const data of ['{"error":"truncated', null, []]) {
      const { callbacks, stream } = fixture()
      stream.onEvent('AGENT_END', data)
      stream.onComplete()
      expect(callbacks.onComplete).not.toHaveBeenCalled()
      expect(callbacks.onError).toHaveBeenCalledTimes(1)
    }
  })

  it('正常完成与主动停止只完成一次', () => {
    const { callbacks, stream } = fixture()
    stream.onEvent('AGENT_END', { aborted: true })
    stream.onComplete()
    stream.onComplete()
    expect(callbacks.onComplete).toHaveBeenCalledTimes(1)
    expect(callbacks.onError).not.toHaveBeenCalled()
  })

  it('有效确认可以结束当前连接，但不是业务已完成', () => {
    const { callbacks, stream } = fixture()
    stream.onEvent('REQUIRE_USER_CONFIRM', { interruptId: 'synthetic-confirmation' })
    stream.onComplete()
    expect(callbacks.onComplete).toHaveBeenCalledTimes(1)
    expect(callbacks.onError).not.toHaveBeenCalled()
  })

  it.each([
    ['MODEL_CALL_END', { error: '模型调用失败' }],
    ['EXCEED_MAX_ITERS', {}],
    ['AGENT_END', { error: '循环失败' }],
    ['ERROR', { message: '占用已失效' }],
    ['error', { error: '流失败' }],
    ['AGENT_END', { interrupted: true }],
    ['REQUIRE_USER_CONFIRM', { interruptId: '' }],
  ])('失败 %s 不能被随后的成功终态覆盖', (type, data) => {
    const { callbacks, stream } = fixture()
    stream.onEvent(type, data)
    stream.onEvent('AGENT_END', {})
    stream.onComplete()
    stream.onError(new Error('second error'))
    expect(callbacks.onComplete).not.toHaveBeenCalled()
    expect(callbacks.onError).toHaveBeenCalledTimes(1)
  })

  it('无终态 EOF 明确报错，不能自动重发请求', () => {
    const { callbacks, stream } = fixture()
    stream.onEvent('TEXT_BLOCK_DELTA', { text: '部分输出' })
    stream.onComplete()
    expect(callbacks.onError.mock.calls[0][0].message).toContain('未收到完成状态')
    expect(callbacks.onComplete).not.toHaveBeenCalled()
  })
})
