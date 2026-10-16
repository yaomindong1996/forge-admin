import { ReadableStream } from 'node:stream/web'
import { TextDecoder, TextEncoder } from 'node:util'
import { flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { engineResume, streamEngineChat } from '../engine'

vi.mock('@/store/modules/auth', () => ({ useAuthStore: () => ({ accessToken: '' }) }))
vi.mock('@/utils', async () => {
  const { consumeSSE } = await import('@/utils/sse')
  return { consumeSSE, generateUUID: () => 'synthetic-nonce', request: { post: vi.fn() } }
})

beforeEach(() => {
  vi.stubGlobal('TextEncoder', TextEncoder)
  vi.stubGlobal('TextDecoder', TextDecoder)
})
afterEach(() => {
  vi.unstubAllGlobals()
})

function frame(type, data) {
  return `event: ${type}\r\ndata: ${JSON.stringify(data)}\r\n\r\n`
}

function respond(text) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, body: new ReadableStream({
    start(controller) {
      const bytes = new TextEncoder().encode(text)
      controller.enqueue(bytes.slice(0, 19))
      controller.enqueue(bytes.slice(19))
      controller.close()
    },
  }) }))
}

async function consume(resume = false) {
  const events = []
  const result = await new Promise((resolve) => {
    const onEvent = (type, data) => events.push({ type, data })
    const done = () => resolve({ done: true })
    const error = cause => resolve({ done: false, message: cause.message })
    if (resume) {
      engineResume('synthetic-confirmation', false, onEvent, done, error)
    }
    else {
      streamEngineChat({ sessionId: 'synthetic-session' }, onEvent, done, error)
    }
  })
  return { result, events }
}

describe('实际引擎封装与通用 SSE 解析器', () => {
  it('终态 JSON 传输半包也不能让通用 EOF 收尾误报成功', async () => {
    respond('event: AGENT_END\r\ndata: {"error":"truncated')
    const { result } = await consume()
    expect(result.done).toBe(false)
    expect(result.message).toContain('完成状态无效')
  })

  it('消息落库元信息的 Long 字符串不会被前端解析丢精度', async () => {
    respond(frame('PERSIST_META', { userRecordId: '9007199254740993', assistantRecordId: '9007199254740995' })
      + frame('AGENT_END', {}))
    const { result, events } = await consume()
    expect(result.done).toBe(true)
    expect(events[0].data).toEqual({ userRecordId: '9007199254740993', assistantRecordId: '9007199254740995' })
  })

  it('分块中文与 CRLF 正常完成，resume 仍使用已有确认接口', async () => {
    respond(frame('TEXT_BLOCK_DELTA', { text: '合成中文响应' }) + frame('AGENT_END', { aborted: true }))
    const { result, events } = await consume(true)
    expect(result.done).toBe(true)
    expect(events[0].data.text).toBe('合成中文响应')
    const [url, request] = fetch.mock.calls[0]
    expect(url).toContain('/ai/engine/resume')
    expect(JSON.parse(request.body)).toEqual({ interruptId: 'synthetic-confirmation', confirmed: false })
  })

  it.each([
    frame('TEXT_BLOCK_DELTA', { text: '未完成' }),
    frame('MODEL_CALL_END', { error: '模型失败' }) + frame('AGENT_END', {}),
    frame('ERROR', { message: '占用已失效' }),
    '{"code":500,"message":"不是SSE"}',
  ])('真实解析管线中异常 EOF/错误不能正常完成：%s', async (text) => {
    respond(text)
    const { result } = await consume()
    expect(result.done).toBe(false)
    expect(result.message).not.toBe('')
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('带尾帧的确认连接可以正常结束，不要求继续等待旧连接', async () => {
    respond(frame('REQUIRE_USER_CONFIRM', { interruptId: 'synthetic-confirmation' }).trimEnd())
    const { result } = await consume()
    expect(result.done).toBe(true)
  })

  it('主动 abort 不报告网络失败或完成', async () => {
    vi.stubGlobal('fetch', vi.fn((url, request) => new Promise((resolve, reject) => {
      request.signal.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')))
    })))
    const error = vi.fn()
    const complete = vi.fn()
    const controller = streamEngineChat({}, vi.fn(), complete, error)
    controller.abort()
    await flushPromises()
    expect(error).not.toHaveBeenCalled()
    expect(complete).not.toHaveBeenCalled()
  })
})
