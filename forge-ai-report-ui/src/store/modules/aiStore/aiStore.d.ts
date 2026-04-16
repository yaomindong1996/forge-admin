import { AIGenerateResponse } from '@/api/ai/ai'
import type { AiChatSession } from '@/api/ai'

export interface AIProviderOption {
  providerId?: number | string
  providerName: string
  modelName?: string
  temperature: number
  maxTokens?: number | null
}

export interface AIStoreType {
  generating: boolean
  streaming: boolean
  streamingText: string
  lastPrompt: string
  lastResponse: AIGenerateResponse | null
  generateHistory: AIHistoryItem[]
  chatMessages: ChatMessage[]
  chatSessions: AiChatSession[]
  currentSessionId: string | null
  aiPanelVisible: boolean
  selectedProvider: AIProviderOption | null
  // 内部：AbortController 用于中止请求
  _abortController: AbortController | null
}

export interface AIHistoryItem {
  prompt: string
  response: AIGenerateResponse
  timestamp: number
}

export type ChatMessageRole = 'user' | 'assistant'

export interface ChatMessage {
  id: string
  role: ChatMessageRole
  content: string
  timestamp: number
  sessionId?: string
  // 当 role 为 assistant 时，保存应用到画布的响应
  canvasResponse?: AIGenerateResponse | null
  // 是否正在流式输出中
  streaming?: boolean
}
