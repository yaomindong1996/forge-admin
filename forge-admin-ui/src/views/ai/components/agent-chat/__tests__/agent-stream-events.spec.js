import { describe, expect, it, vi } from 'vitest'
import { applyAgentStreamEvent, finishAssistantMessage } from '../agent-stream-events'

function fixture() {
  const msg = { status: 'streaming', streaming: true, content: '', thinking: '', toolCalls: [] }
  const effects = { persistMeta: vi.fn(), scheduleRender: vi.fn(), flushRender: vi.fn(), confirm: vi.fn(), hint: vi.fn() }
  return { msg, effects, event: (type, data) => applyAgentStreamEvent(type, data, msg, effects) }
}

describe('agent 消息与工具终态', () => {
  it('历史等待记录没有调用 ID 时只接回最后的旧调用，不错配明确 ID', () => {
    const { msg, event } = fixture()
    msg.toolCalls = [{ name: 'write', status: 'waiting_confirm', result: '' }]
    event('USER_CONFIRM_RESULT', { toolCallId: 'restored-1', confirmed: true })
    event('TOOL_RESULT_TEXT_DELTA', { content: '合成结果' })
    event('TOOL_RESULT_END', { toolCallId: 'restored-1', success: true })
    expect(msg.toolCalls).toHaveLength(1)
    expect(msg.toolCalls[0]).toMatchObject({ callId: 'restored-1', status: 'done', result: '合成结果' })
    event('TOOL_RESULT_END', { toolCallId: 'other-call', success: false })
    expect(msg.toolCalls[0].status).toBe('done')
  })

  it('工具失败/拒绝保留真实状态，结束对话不能改为完成', () => {
    const { msg, event } = fixture()
    event('TOOL_CALL_START', { tool: 'image_generate', toolCallId: 'tool-1' })
    event('TOOL_RESULT_TEXT_DELTA', { content: '错误: 合成失败' })
    event('TOOL_RESULT_END', { toolCallId: 'tool-1', success: false })
    event('TOOL_CALL_START', { tool: 'http_request', toolCallId: 'tool-2' })
    event('ALL_TOOLS_DENIED', { toolCallId: 'tool-2' })
    event('AGENT_END', {})
    expect(msg.toolCalls.map(tool => tool.status)).toEqual(['error', 'aborted'])
    expect(msg.toolCalls[0].result).toContain('错误')
  })

  it('恢复后按调用 ID 接续，不创建重复工具；再次确认不被 complete 覆盖', () => {
    const { msg, event, effects } = fixture()
    event('TOOL_CALL_START', { tool: 'write', toolCallId: 'tool-1' })
    event('REQUIRE_USER_CONFIRM', { interruptId: 'first', toolCallId: 'tool-1' })
    finishAssistantMessage(msg)
    expect(msg.status).toBe('waiting_confirm')
    expect(msg.toolCalls[0].status).toBe('waiting_confirm')
    event('USER_CONFIRM_RESULT', { confirmed: true, toolCallId: 'tool-1' })
    event('TOOL_RESULT_END', { toolCallId: 'tool-1', success: true })
    event('TOOL_CALL_START', { tool: 'second', toolCallId: 'tool-2' })
    event('REQUIRE_USER_CONFIRM', { interruptId: 'second', toolCallId: 'tool-2' })
    finishAssistantMessage(msg)
    expect(msg.status).toBe('waiting_confirm')
    expect(msg.toolCalls.map(tool => tool.status)).toEqual(['done', 'waiting_confirm'])
    expect(effects.confirm).toHaveBeenCalledTimes(2)
  })

  it.each([
    ['MODEL_CALL_END', { error: '模型失败' }],
    ['EXCEED_MAX_ITERS', {}],
    ['ERROR', { message: '占用失败' }],
    ['AGENT_END', { error: '执行失败' }],
  ])('%s 后的终态不覆盖错误', (type, data) => {
    const { msg, event } = fixture()
    event(type, data)
    event('AGENT_END', {})
    finishAssistantMessage(msg)
    expect(msg.status).toBe('error')
    expect(msg.error).not.toBe('')
  })

  it('未结束工具不能通过下一调用或 EOF 伪装成功', () => {
    const { msg, event } = fixture()
    event('TOOL_CALL_START', { tool: 'first', toolCallId: 'tool-1' })
    event('TOOL_CALL_START', { tool: 'second', toolCallId: 'tool-2' })
    finishAssistantMessage(msg)
    expect(msg.toolCalls.map(tool => tool.status)).toEqual(['error', 'error'])
  })
})
