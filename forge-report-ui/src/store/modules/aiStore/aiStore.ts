import { defineStore } from 'pinia'
import { AIStoreType, AIHistoryItem, ChatMessage } from './aiStore.d'
import { AIGenerateResponse } from '@/api/ai/ai.d'

export const useAIStore = defineStore({
  id: 'useAIStore',
  state: (): AIStoreType => ({
    generating: false,
    streaming: false,
    streamingText: '',
    lastPrompt: '',
    lastResponse: null,
    generateHistory: [],
    chatMessages: [],
    aiPanelVisible: false,
    _abortController: null as AbortController | null
  }),
  getters: {
    getGenerating(): boolean {
      return this.generating
    },
    getStreaming(): boolean {
      return this.streaming
    },
    getStreamingText(): string {
      return this.streamingText
    },
    getLastPrompt(): string {
      return this.lastPrompt
    },
    getLastResponse(): AIGenerateResponse | null {
      return this.lastResponse
    },
    getGenerateHistory(): AIHistoryItem[] {
      return this.generateHistory
    },
    getChatMessages(): ChatMessage[] {
      return this.chatMessages
    },
    getAIPanelVisible(): boolean {
      return this.aiPanelVisible
    }
  },
  actions: {
    setGenerating(value: boolean) {
      this.generating = value
    },
    setStreaming(value: boolean) {
      this.streaming = value
    },
    setStreamingText(value: string) {
      this.streamingText = value
    },
    appendStreamingText(chunk: string) {
      this.streamingText += chunk
      // 更新最后一条 assistant 消息的内容（使用 splice 触发响应式更新）
      const idx = this.chatMessages.length - 1
      if (this.chatMessages[idx] && this.chatMessages[idx].role === 'assistant' && this.chatMessages[idx].streaming) {
        this.chatMessages[idx].content += chunk
      }
    },
    getAbortController(): AbortController {
      if (!this._abortController) {
        this._abortController = new AbortController()
      }
      return this._abortController
    },
    abortGenerating() {
      if (this._abortController) {
        this._abortController.abort()
        this._abortController = null
      }
      this.generating = false
      // 取消最后一条消息的 streaming 状态
      const lastMsg = this.chatMessages[this.chatMessages.length - 1]
      if (lastMsg && lastMsg.role === 'assistant' && lastMsg.streaming) {
        lastMsg.streaming = false
      }
    },
    setLastPrompt(prompt: string) {
      this.lastPrompt = prompt
    },
    setLastResponse(response: AIGenerateResponse | null) {
      this.lastResponse = response
    },
    addHistory(prompt: string, response: AIGenerateResponse) {
      this.generateHistory.unshift({
        prompt,
        response,
        timestamp: Date.now()
      })
      // 最多保留 20 条历史
      if (this.generateHistory.length > 20) {
        this.generateHistory = this.generateHistory.slice(0, 20)
      }
    },
    addChatMessage(msg: ChatMessage) {
      this.chatMessages.push(msg)
    },
    updateLastAssistantMessage(content: string, canvasResponse?: AIGenerateResponse | null) {
      const lastMsg = this.chatMessages[this.chatMessages.length - 1]
      if (lastMsg && lastMsg.role === 'assistant') {
        lastMsg.content = content
        lastMsg.streaming = false
        if (canvasResponse !== undefined) {
          lastMsg.canvasResponse = canvasResponse
        }
      }
    },
    clearChat() {
      this.chatMessages = []
      this.streamingText = ''
      this.abortGenerating()
    },
    setAIPanelVisible(value: boolean) {
      this.aiPanelVisible = value
    }
  }
})
