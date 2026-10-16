import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, reactive, ref } from 'vue'
import { engineResume, engineStop, streamEngineChat } from '@/api/ai/engine'
import { useAgentChatStream } from '../useAgentChatStream'

vi.mock('@/api/ai/engine', () => ({ engineResume: vi.fn(), engineStop: vi.fn(), streamEngineChat: vi.fn() }))
let wrapper
let chat
let options
function assistant() {
  return reactive({ id: 'assistant', role: 'assistant', status: 'streaming', streaming: true, content: '', thinking: '', toolCalls: [], error: '' })
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.resetAllMocks()
  streamEngineChat.mockReturnValue(new AbortController())
  engineResume.mockReturnValue(new AbortController())
  engineStop.mockResolvedValue({ code: 200 })
  options = {
    message: { warning: vi.fn(), error: vi.fn(), info: vi.fn() },
    messages: ref([]),
    currentAgent: ref({ agentCode: 'synthetic' }),
    currentSessionId: ref('synthetic-session'),
    isStreaming: ref(false),
    connectionError: ref(false),
    pendingConfirm: ref(null),
    hitlCountdown: ref(0),
    scrollToBottom: vi.fn(),
    scheduleRender: vi.fn(),
    flushRender: vi.fn(),
  }
  wrapper = mount(defineComponent({ setup() {
    chat = useAgentChatStream(options)
    return () => h('div')
  } }))
})
afterEach(() => {
  wrapper?.unmount()
  vi.clearAllTimers()
  vi.useRealTimers()
})

describe('agent 页面流编排', () => {
  it('取消等待确认提交已有拒绝接口，而非只清空前端面板', () => {
    const msg = assistant()
    options.messages.value = [msg]
    options.pendingConfirm.value = { interruptId: 'first' }
    chat.cancelHitl()
    expect(engineResume).toHaveBeenCalledTimes(1)
    expect(engineResume.mock.calls[0][1]).toBe(false)
    expect(engineStop).not.toHaveBeenCalled()
  })

  it('恢复流再次确认后 complete 仍保持等待状态和下一次确认', () => {
    const msg = assistant()
    options.messages.value = [msg]
    options.pendingConfirm.value = { interruptId: 'first' }
    chat.handleConfirm(true)
    const [, , event, complete] = engineResume.mock.calls[0]
    event('REQUIRE_USER_CONFIRM', { interruptId: 'second', tool: 'write' })
    complete()
    expect(msg.status).toBe('waiting_confirm')
    expect(options.pendingConfirm.value.interruptId).toBe('second')
    expect(options.isStreaming.value).toBe(false)
  })

  it('恢复失败/主动拒绝的最终状态不被 done 覆盖', () => {
    const msg = assistant()
    options.messages.value = [msg]
    options.pendingConfirm.value = { interruptId: 'first' }
    chat.handleConfirm(false)
    const [, confirmed, event, complete] = engineResume.mock.calls[0]
    expect(confirmed).toBe(false)
    event('AGENT_END', { aborted: true })
    complete()
    expect(msg.status).toBe('aborted')
  })

  it('旧流迟到不能清空新流；附件 ID 不通过 Number 丢失精度', () => {
    const old = assistant()
    const current = assistant()
    current.id = 'second'
    chat.startStream('first', old, 'synthetic', ['9007199254740993'])
    const [, oldEvent, oldComplete] = streamEngineChat.mock.calls[0]
    expect(streamEngineChat.mock.calls[0][0].imageFileIds).toEqual(['9007199254740993'])
    chat.startStream('second', current, 'synthetic', [])
    oldEvent('TEXT_BLOCK_DELTA', { text: '旧响应' })
    oldComplete()
    expect(old.content).toBe('')
    expect(options.isStreaming.value).toBe(true)
  })

  it('断流失败不自动重发，未完成工具不伪装成功', () => {
    const msg = assistant()
    chat.startStream('first', msg, 'synthetic', [])
    const [, event, , error] = streamEngineChat.mock.calls[0]
    event('TOOL_CALL_START', { tool: 'image_generate' })
    error(new Error('未收到完成状态'))
    expect(msg.status).toBe('error')
    expect(msg.toolCalls[0].status).toBe('error')
    expect(options.isStreaming.value).toBe(false)
    expect(streamEngineChat).toHaveBeenCalledTimes(1)
  })

  it('确认超时仅提交一次拒绝；离页断开本地订阅不调用后台 stop', async () => {
    const msg = assistant()
    options.messages.value = [msg]
    chat.startStream('first', msg, 'synthetic', [])
    const controller = streamEngineChat.mock.results[0].value
    const [, event, complete] = streamEngineChat.mock.calls[0]
    event('REQUIRE_USER_CONFIRM', { interruptId: 'first', tool: 'write' })
    complete()
    await vi.advanceTimersByTimeAsync(31000)
    expect(engineResume).toHaveBeenCalledTimes(1)
    expect(engineResume.mock.calls[0][1]).toBe(false)
    wrapper.unmount()
    wrapper = null
    expect(engineStop).not.toHaveBeenCalled()
    expect(controller.signal.aborted).toBe(false)
    expect(engineResume.mock.results[0].value.signal.aborted).toBe(true)
  })

  it('手动停止只发一个 stop，后续回调不能覆盖 aborted', () => {
    const msg = assistant()
    chat.startStream('first', msg, 'synthetic', [])
    const [, , complete] = streamEngineChat.mock.calls[0]
    chat.stopChat()
    complete()
    expect(msg.status).toBe('aborted')
    expect(engineStop).toHaveBeenCalledTimes(1)
  })
})
