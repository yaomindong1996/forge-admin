import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { engineStop, hubAgents, hubRoute, hubTodos, streamEngineChat } from '@/api/ai'
import { assistantLocalTurn } from '@/composables/ai/assistant-local-turn'
import { messageWithPageContext } from '@/composables/ai/assistant-page-context'
import { useAuthStore, useUserStore } from '@/store'
import { generateUUID } from '@/utils'

export const useHubAssistantStore = defineStore('hub-assistant', () => {
  const auth = useAuthStore()
  const user = useUserStore()

  const turns = ref([])
  const draft = ref('')
  const busy = ref(false)
  const agents = ref([])
  const selectedAgentCode = ref(null)
  let activeStream = null
  let activeSessionId = ''

  watch(() => [auth.accessToken, user.userId], () => {
    clear()
    agents.value = []
    selectedAgentCode.value = null
  })

  async function loadAgents() {
    try {
      const res = await hubAgents()
      agents.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
    }
    catch {
      agents.value = []
    }
  }

  function stopStreaming() {
    if (activeStream) {
      activeStream.abort()
      activeStream = null
    }
    if (activeSessionId) {
      engineStop(activeSessionId).catch(() => {})
      activeSessionId = ''
    }
    const last = turns.value[turns.value.length - 1]
    if (last && last.status === 'streaming')
      last.status = 'stopped'
    busy.value = false
  }

  function clear() {
    stopStreaming()
    turns.value = []
    draft.value = ''
  }

  function findPreviousAgentCode() {
    for (let i = turns.value.length - 1; i >= 0; i -= 1) {
      const item = turns.value[i]
      if (item.agent?.code && item.status === 'done')
        return item.agent.code
    }
    return undefined
  }

  function startAgentStream(turn, agent) {
    const sessionId = turn.sessionId || generateUUID()
    turn.sessionId = sessionId
    turn.agent = agent
    turn.status = 'streaming'
    activeSessionId = sessionId

    activeStream = streamEngineChat(
      {
        agentCode: agent.code,
        sessionId,
        message: messageWithPageContext(turn.question, turn.page),
      },
      (type, data) => {
        if (type === 'TEXT_BLOCK_DELTA' && data?.text) {
          turn.answer += data.text
        }
        else if (type === 'TOOL_CALL_START' && data?.tool) {
          turn.tools.push({ name: data.tool, status: 'running' })
        }
        else if (type === 'TOOL_RESULT_END' && data?.tool) {
          const item = [...turn.tools].reverse().find(t => t.name === data.tool && t.status === 'running')
          if (item)
            item.status = data.error ? 'error' : 'done'
        }
      },
      () => {
        activeStream = null
        activeSessionId = ''
        turn.status = turn.answer.trim() ? 'done' : 'error'
        if (turn.status === 'error')
          turn.error = '智能体未返回正文，请重试。'
        busy.value = false
      },
      (err) => {
        activeStream = null
        activeSessionId = ''
        turn.status = 'error'
        turn.error = err?.message || '智能体对话失败'
        busy.value = false
      },
    )
  }

  async function send(rawText, forcedAgent = null, context = null) {
    const question = String(rawText ?? draft.value).trim()
    if (!question || busy.value)
      return

    draft.value = ''
    busy.value = true
    const turn = {
      id: generateUUID(),
      question,
      kind: 'CHAT',
      agent: forcedAgent || null,
      candidates: [],
      todos: null,
      answer: '',
      tools: [],
      guide: null,
      page: context?.page || null,
      sessionId: '',
      status: 'routing',
      error: '',
    }
    turns.value.push(turn)

    if (!forcedAgent?.code) {
      const local = assistantLocalTurn(question, context || {})
      if (local.resolve) {
        turn.kind = 'GUIDE'
        Object.assign(turn, await local.resolve())
        turn.status = 'done'
        busy.value = false
        return
      }
    }

    if (forcedAgent?.code) {
      startAgentStream(turn, forcedAgent)
      return
    }

    if (selectedAgentCode.value) {
      const picked = agents.value.find(item => item.code === selectedAgentCode.value)
      if (picked) {
        startAgentStream(turn, picked)
        return
      }
    }

    try {
      const res = await hubRoute({
        message: question,
        previousAgentCode: findPreviousAgentCode(),
      })
      if (res.code !== 200 || !res.data)
        throw new Error(res.msg || '助手路由失败')

      const plan = res.data
      turn.kind = plan.kind
      if (plan.kind === 'TODO') {
        if (plan.todos) {
          turn.todos = {
            ...plan.todos,
            items: plan.todos.items || plan.todos.records || [],
          }
        }
        else {
          const todoRes = await hubTodos({ pageNum: 1, pageSize: 10 })
          const page = todoRes?.code === 200 && todoRes.data ? todoRes.data : null
          turn.todos = page
            ? {
                available: page.available,
                total: Number(page.total || 0),
                items: page.items || page.records || [],
                message: page.message,
              }
            : { available: false, total: 0, items: [], message: todoRes?.msg || '待办列表加载失败' }
        }
        turn.status = 'done'
        busy.value = false
        return
      }
      if (plan.kind === 'CLARIFY') {
        turn.answer = plan.notice || plan.message || '请选择要对话的智能体：'
        turn.candidates = plan.candidates?.length ? plan.candidates : [...agents.value]
        turn.status = 'done'
        busy.value = false
        return
      }
      if (plan.kind === 'CHAT' && plan.agent?.code) {
        startAgentStream(turn, plan.agent)
        return
      }
      turn.answer = plan.notice || plan.message || '暂无可用智能体，请先在「智能体」页面启用智能体。'
      turn.status = 'done'
      busy.value = false
    }
    catch (err) {
      turn.status = 'error'
      turn.error = err?.message || '请求失败，请稍后重试'
      busy.value = false
    }
  }

  return {
    turns,
    draft,
    busy,
    agents,
    selectedAgentCode,
    loadAgents,
    send,
    stopStreaming,
    clear,
  }
})
