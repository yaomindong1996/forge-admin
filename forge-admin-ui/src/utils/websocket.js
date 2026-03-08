import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useAuthStore, useUserStore } from '@/store'

let stompClient = null
let isConnected = false
let isConnecting = false

/**
 * 初始化 WebSocket 客户端
 * - 仅在有 token 且未连接时执行
 */
export function initWebSocketClient() {
  console.log('初始化 WebSocket 客户端...')
  const authStore = useAuthStore()
  const userStore = useUserStore()

  // 没有 token 不初始化
  if (!authStore.accessToken) return
  // 没有用户信息时一般还在登录流程,由权限守卫在拿到用户信息后再调用
  if (!userStore.userId) return

  // 已连接或正在连接时不重复初始化
  if (isConnected || isConnecting) return

  isConnecting = true

  // 使用 SockJS 连接后端 /ws 端点
  // 开发环境下直接连接后端服务(避免通过Vite代理的WebSocket兼容问题)
  let socketUrl = '/ws'
  if (import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY_TARGET) {
    try {
      const target = new URL(import.meta.env.VITE_HTTP_PROXY_TARGET)
      socketUrl = `${target.origin}/ws`
    } catch (e) {
      console.warn('解析 VITE_HTTP_PROXY_TARGET 失败, 使用相对路径 /ws', e)
    }
  }
  const socket = new SockJS(socketUrl, null, { transports: ['xhr-streaming', 'xhr-polling'] })

  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {},
  })

  console.log('WebSocket 客户端初始化完成')

  stompClient.onConnect = () => {
    isConnected = true
    isConnecting = false

    // 订阅认证相关广播主题
    stompClient.subscribe('/topic/auth', (frame) => {
      try {
        const body = JSON.parse(frame.body || '{}')
        handleWebSocketMessage(body)
      } catch (e) {
        console.error('解析 WebSocket 消息失败:', e, frame)
      }
    })
  }

  stompClient.onStompError = (frame) => {
    console.error('STOMP 错误:', frame.headers['message'], frame.body)
  }

  stompClient.onWebSocketClose = () => {
    isConnected = false
    isConnecting = false
  }

  stompClient.activate()
}

/**
 * 断开 WebSocket 连接
 */
export function disconnectWebSocketClient() {
  if (stompClient) {
    try {
      stompClient.deactivate()
    } catch (e) {
      console.error('关闭 WebSocket 连接失败:', e)
    }
    stompClient = null
  }
  isConnected = false
  isConnecting = false
}

/**
 * 处理服务端推送的消息
 */
function handleWebSocketMessage(payload) {
  const { type, title, message, level, data } = payload || {}
  const authStore = useAuthStore()
  const userStore = useUserStore()

  // 当前登录用户ID
  const currentUserId = userStore.userId
  const targetUserId = data && (data.userId ?? data.userID)

  // 如果消息指定了用户且不是发给当前用户的,直接忽略
  if (targetUserId && currentUserId && String(targetUserId) !== String(currentUserId)) {
    return
  }

  // 使用 Naive UI 全局消息
  const notify = (msg, lvl = 'info') => {
    if (window.$message) {
      switch (lvl) {
        case 'success':
          window.$message.success(msg)
          break
        case 'warning':
          window.$message.warning(msg)
          break
        case 'error':
          window.$message.error(msg)
          break
        default:
          window.$message.info(msg)
      }
    } else {
      console.log('[WebSocket]', lvl, msg)
    }
  }

  const content = message || title || '收到一条系统消息'
  const lvl = level || 'info'

  // 统一处理认证相关消息
  switch (type) {
    case 'auth.kickout':
    case 'auth.replaced':
    case 'auth.banned': {
      notify(content, lvl)
      // 清理本地登录状态并跳转登录页
      authStore.logout()
      break
    }
    default:
      // 其它类型按普通通知处理
      notify(content, lvl)
  }
}
