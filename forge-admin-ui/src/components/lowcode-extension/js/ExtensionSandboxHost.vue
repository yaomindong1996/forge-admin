<template>
  <span class="extension-sandbox-host" aria-hidden="true" />
</template>

<script setup>
import {
  sanitizeExtensionContext,
  validateClientScript,
  validateSandboxResult,
} from './extension-context-api'

const props = defineProps({
  timeoutMs: {
    type: Number,
    default: 800,
  },
  maxOutputBytes: {
    type: Number,
    default: 64 * 1024,
  },
  startupTimeoutMs: {
    type: Number,
    default: 5000,
  },
})

function execute(script, context = {}, allowedFields = []) {
  validateClientScript(script)
  const safeContext = sanitizeExtensionContext(context, allowedFields)
  const nonce = createNonce()
  let worker
  try {
    worker = new Worker(new URL('./extension-sandbox.worker.js', import.meta.url), { type: 'module' })
  }
  catch (error) {
    throw new Error('无法创建客户端扩展隔离 Worker：' + normalizeThrownError(error))
  }

  return new Promise((resolve, reject) => {
    let settled = false
    let executionTimeout = null
    const startupTimeout = window.setTimeout(() => {
      rejectOnce(new Error('客户端扩展隔离 Worker 初始化超时：模块未在限定时间内完成加载，请刷新页面或重启前端开发服务'))
    }, normalizeStartupTimeout(props.startupTimeoutMs))

    worker.addEventListener('message', (event) => {
      const response = event.data || {}
      if (settled)
        return
      if (response.type === 'EXTENSION_WORKER_READY') {
        window.clearTimeout(startupTimeout)
        startExecution()
        return
      }
      if (response.type === 'EXTENSION_WORKER_FATAL') {
        rejectOnce(new Error(response.error || '客户端扩展隔离 Worker 发生未捕获错误'))
        return
      }
      if (response.nonce !== nonce)
        return
      if (!response.ok) {
        rejectOnce(new Error(response.error || stageError(response.stage)))
        return
      }
      try {
        resolveOnce(validateSandboxResult(response.result, props.maxOutputBytes))
      }
      catch (error) {
        rejectOnce(error)
      }
    })

    worker.addEventListener('error', (event) => {
      event.preventDefault?.()
      rejectOnce(new Error(formatWorkerError(event)))
    })

    worker.addEventListener('messageerror', () => {
      rejectOnce(new Error('客户端扩展隔离 Worker 消息解析失败：测试上下文或返回结果无法安全序列化'))
    })

    function startExecution() {
      if (settled || executionTimeout)
        return
      executionTimeout = window.setTimeout(() => {
        rejectOnce(new Error('客户端扩展脚本执行超时，已终止隔离 Worker'))
      }, normalizeTimeout(props.timeoutMs))
      try {
        worker.postMessage({
          type: 'EXECUTE_EXTENSION',
          nonce,
          script,
          context: safeContext,
        })
      }
      catch (error) {
        rejectOnce(new Error('无法向客户端扩展隔离 Worker 发送测试上下文：' + normalizeThrownError(error)))
      }
    }

    function resolveOnce(result) {
      if (settled)
        return
      settled = true
      cleanup()
      resolve(result)
    }

    function rejectOnce(error) {
      if (settled)
        return
      settled = true
      cleanup()
      reject(error instanceof Error ? error : new Error(String(error || '客户端扩展执行失败')))
    }

    function cleanup() {
      window.clearTimeout(startupTimeout)
      if (executionTimeout)
        window.clearTimeout(executionTimeout)
      worker.terminate()
    }
  })
}

function createNonce() {
  const values = new Uint32Array(4)
  crypto.getRandomValues(values)
  return [...values].map(value => value.toString(16).padStart(8, '0')).join('')
}

function normalizeTimeout(timeoutMs) {
  return Math.max(50, Math.min(Number(timeoutMs) || 800, 3000))
}

function normalizeStartupTimeout(timeoutMs) {
  return Math.max(1000, Math.min(Number(timeoutMs) || 5000, 15000))
}

function stageError(stage) {
  const label = {
    VALIDATE: '脚本安全校验',
    HARDEN: '沙箱环境初始化',
    EXECUTE: '脚本运行',
    RESPONSE: '结果返回',
  }[stage] || '未知阶段'
  return '客户端扩展在' + label + '失败'
}

function formatWorkerError(event) {
  const rawMessage = String(event?.message || '').trim()
  const message = rawMessage && rawMessage !== 'Script error.'
    ? sanitizeErrorMessage(rawMessage)
    : 'Worker 模块加载或初始化失败，浏览器未返回详细异常'
  const file = safeFileName(event?.filename)
  const line = Number(event?.lineno || 0)
  const column = Number(event?.colno || 0)
  const location = file
    ? '（' + file + (line ? ':' + line : '') + (column ? ':' + column : '') + '）'
    : ''
  return '客户端扩展隔离 Worker 异常终止：' + message + location + workerErrorHint(rawMessage)
}

function normalizeThrownError(error) {
  return sanitizeErrorMessage(error instanceof Error ? error.message : String(error || '未知错误'))
}

function sanitizeErrorMessage(message) {
  return String(message || '未知错误')
    .replace(/(token|secret|password|cookie|authorization)\S*/gi, '[REDACTED]')
    .slice(0, 500)
}

function safeFileName(filename) {
  const value = String(filename || '').split('?')[0].split('#')[0]
  if (!value)
    return ''
  return value.split('/').pop() || ''
}

function workerErrorHint(message) {
  const value = String(message || '')
  if (/module|import|fetch/i.test(value))
    return '；Worker 模块加载失败，请强制刷新页面，仍失败时重启前端开发服务'
  if (/content security|csp|worker-src/i.test(value))
    return '；当前 Content-Security-Policy 未允许加载 module Worker'
  return ''
}

defineExpose({ execute })
</script>

<style scoped>
.extension-sandbox-host {
  display: none;
}
</style>
