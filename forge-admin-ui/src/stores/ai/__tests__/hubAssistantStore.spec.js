import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const hubRouteMock = vi.fn()
const hubAgentsMock = vi.fn()
const hubTodosMock = vi.fn()
const streamEngineChatMock = vi.fn()
const engineStopMock = vi.fn(() => Promise.resolve({ code: 200 }))

vi.mock('@/api/ai', () => ({
  hubRoute: (...args) => hubRouteMock(...args),
  hubAgents: (...args) => hubAgentsMock(...args),
  hubTodos: (...args) => hubTodosMock(...args),
  streamEngineChat: (...args) => streamEngineChatMock(...args),
  engineStop: (...args) => engineStopMock(...args),
  agentGetById: vi.fn(() => Promise.resolve({ code: 200, data: null })),
  agentSkillPackages: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  knowledgePage: vi.fn(() => Promise.resolve({ code: 200, data: { records: [] } })),
}))

vi.mock('@/store', () => ({
  useAuthStore: () => ({ accessToken: 'token-1' }),
  useUserStore: () => ({ userId: 1 }),
}))

const { useHubAssistantStore } = await import('../hubAssistantStore')

describe('useHubAssistantStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('routes todo intent and fetches todo page when route omits inline todos', async () => {
    hubRouteMock.mockResolvedValue({
      code: 200,
      data: {
        kind: 'TODO',
        intent: 'MY_TODOS',
      },
    })
    hubTodosMock.mockResolvedValue({
      code: 200,
      data: {
        available: true,
        total: 1,
        records: [{ taskId: 't-1', taskName: '审批采购单' }],
      },
    })

    const store = useHubAssistantStore()
    await store.send('查看我的待办任务')

    expect(store.turns).toHaveLength(1)
    expect(store.turns[0].kind).toBe('TODO')
    expect(store.turns[0].todos.total).toBe(1)
    expect(store.turns[0].todos.items).toHaveLength(1)
    expect(store.turns[0].status).toBe('done')
    expect(hubTodosMock).toHaveBeenCalledTimes(1)
    expect(streamEngineChatMock).not.toHaveBeenCalled()
  })

  it('routes chat intent to matched agent and streams answer deltas', async () => {
    hubRouteMock.mockResolvedValue({
      code: 200,
      data: {
        kind: 'CHAT',
        agent: { code: 'general_assistant', name: '通用助手' },
      },
    })
    streamEngineChatMock.mockImplementation((_payload, onEvent, onComplete) => {
      onEvent('TOOL_CALL_START', {
        tool: 'mcp_1_abc',
        toolCallId: 'c1',
        label: 'CRM / search',
        source: 'mcp',
      })
      onEvent('TOOL_RESULT_END', { tool: 'mcp_1_abc', toolCallId: 'c1', success: true })
      onEvent('TEXT_BLOCK_DELTA', { text: '你好，' })
      onEvent('TEXT_BLOCK_DELTA', { text: '这是测试回复。' })
      onEvent('MODEL_CALL_END', { usage: { totalTokens: 42 } })
      onEvent('AGENT_END', {})
      onComplete()
      return { abort: vi.fn() }
    })

    const store = useHubAssistantStore()
    await store.send('帮我查下客户资料')

    expect(store.turns).toHaveLength(1)
    expect(store.turns[0].agent.code).toBe('general_assistant')
    expect(store.turns[0].answer).toBe('你好，这是测试回复。')
    expect(store.turns[0].tools).toEqual([{
      callId: 'c1',
      name: 'mcp_1_abc',
      label: 'CRM / search',
      source: 'mcp',
      args: '',
      result: '',
      status: 'done',
      error: '',
    }])
    expect(store.turns[0].tokenUsage).toBe(42)
    expect(store.turns[0].firstTokenMs).toEqual(expect.any(Number))
    expect(store.turns[0].totalMs).toEqual(expect.any(Number))
    expect(store.turns[0].status).toBe('done')
    expect(store.busy).toBe(false)
  })

  it('handles clarify response with clickable candidate agents', async () => {
    hubRouteMock.mockResolvedValue({
      code: 200,
      data: {
        kind: 'CLARIFY',
        message: '请选择智能体',
        candidates: [{ code: 'hr_helper', name: '人事助手' }],
      },
    })

    const store = useHubAssistantStore()
    await store.send('请问年假怎么算')

    expect(store.turns[0].kind).toBe('CLARIFY')
    expect(store.turns[0].candidates).toHaveLength(1)
    expect(store.turns[0].status).toBe('done')
  })

  it('locally answers menu guidance without routing to agents', async () => {
    const store = useHubAssistantStore()
    await store.send('角色怎么配置？', null, {
      page: null,
      entries: [{
        id: '10',
        title: '角色管理',
        trail: ['系统管理', '角色管理'],
        path: '/system/role',
        menu: { path: '/system/role' },
      }],
      can: () => true,
    })

    expect(store.turns[0].kind).toBe('GUIDE')
    expect(store.turns[0].guide.options[0].title).toBe('角色管理')
    expect(hubRouteMock).not.toHaveBeenCalled()
    expect(streamEngineChatMock).not.toHaveBeenCalled()
  })
})
